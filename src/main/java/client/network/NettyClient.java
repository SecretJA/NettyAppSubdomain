package client.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import client.gui.GuiClient;
import client.crypto.CryptoUtils;
import client.model.MessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import io.netty.channel.ChannelOption;
import java.util.ArrayList;
import java.security.PublicKey;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.buffer.Unpooled;

/**
 * Quản lý kết nối Netty client, cấu hình pipeline và handler.
 */
public class NettyClient {
    private final GuiClient gui;
    private Channel channel;
    private EventLoopGroup group;
    private volatile boolean isConnected = false;
    private String currentHost;
    private int currentPort;
    private PublicKey serverPublicKey;

    public NettyClient(GuiClient gui) {
        this.gui = gui;
    }

    public void connect(String host, int port) {
        // Nếu đã kết nối, ngắt kết nối cũ trước
        if (isConnected && channel != null) {
            disconnect();
        }

        this.currentHost = host;
        this.currentPort = port;

        group = new NioEventLoopGroup();
        try {
            gui.updateLog("🔗 Đang kết nối đến server " + host + ":" + port + "...");
            gui.setConnectionStatus(false);

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10 giây timeout
                    .option(ChannelOption.SO_KEEPALIVE, true) // Giữ kết nối
                    .handler(new ChannelInitializer<Channel>() {
                        // Trong phần initChannel của NettyClient.java, thay thế handler pipeline:

                        @Override
                        protected void initChannel(Channel ch) {
                            // Sử dụng LineBasedFrameDecoder thay vì DelimiterBasedFrameDecoder
                            ch.pipeline().addLast(
                                    new LineBasedFrameDecoder(65536), // Tách theo \n hoặc \r\n
                                    new StringDecoder(),
                                    new StringEncoder(),
                                    new SimpleChannelInboundHandler<String>() {
                                        // Buffer để tích lũy JSON chunks
                                        private StringBuilder jsonBuffer = new StringBuilder();

                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                            try {
                                                String line = msg.trim();

                                                // Bỏ qua dòng trống
                                                if (line.isEmpty()) return;

                                                // Xử lý public key
                                                if (line.startsWith("PUBLIC_KEY:")) {
                                                    String base64Key = line.substring("PUBLIC_KEY:".length()).trim();
                                                    serverPublicKey = client.crypto.CryptoUtils.loadPublicKeyFromBase64(base64Key);
                                                    gui.updateLog("🔑 Đã nhận public key từ server!");
                                                    gui.updateServerPublicKeyInfo(base64Key);
                                                    return;
                                                }

                                                // Xử lý delimiter ---END---
                                                if (line.equals("---END---")) {
                                                    if (jsonBuffer.length() > 0) {
                                                        processCompleteJson(jsonBuffer.toString());
                                                        jsonBuffer.setLength(0); // Clear buffer
                                                    }
                                                    return;
                                                }

                                                // Tích lũy JSON data
                                                if (line.startsWith("{")) {
                                                    // Bắt đầu JSON mới
                                                    jsonBuffer.setLength(0);
                                                    jsonBuffer.append(line);
                                                } else if (jsonBuffer.length() > 0) {
                                                    // Tiếp tục JSON hiện tại
                                                    jsonBuffer.append(line);
                                                }

                                                // Kiểm tra JSON hoàn chình (kết thúc bằng })
                                                if (jsonBuffer.length() > 0 && line.endsWith("}")) {
                                                    processCompleteJson(jsonBuffer.toString());
                                                    jsonBuffer.setLength(0);
                                                }

                                            } catch (Exception e) {
                                                gui.updateLog("❌ Lỗi xử lý response: " + e.getMessage());
                                                gui.updateLog("[DEBUG] Raw msg: " + msg);
                                                java.io.StringWriter sw = new java.io.StringWriter();
                                                e.printStackTrace(new java.io.PrintWriter(sw));
                                                gui.updateLog("[STACKTRACE] " + sw.toString());
                                            }
                                        }

                                        // Phương thức xử lý JSON hoàn chỉnh
                                        private void processCompleteJson(String jsonStr) {
                                            try {
                                                ObjectMapper mapper = new ObjectMapper();
                                                JsonNode node = mapper.readTree(jsonStr);

                                                // Xử lý JSON mã hóa
                                                if (node.has("encryptedKey") && node.has("encryptedData") && node.has("encryptedIv")) {
                                                    String decryptedMsg = tryDecryptResponse(node);
                                                    if (decryptedMsg != null) {
                                                        gui.updateLog("🔓 Đã giải mã response từ server bằng AES+RSA!");
                                                        try {
                                                            client.model.MessageResponse response = mapper.readValue(decryptedMsg, client.model.MessageResponse.class);
                                                            gui.updateScanResult(
                                                                    response.getStatus(),
                                                                    response.getResult(),
                                                                    response.getFoundDomains(),
                                                                    response.getTotalScanned(),
                                                                    response.getTotalFound(),
                                                                    response.getTargetDomain()
                                                            );
                                                        } catch (Exception e) {
                                                            gui.updateLog("❌ Lỗi parse response (sau giải mã): " + e.getMessage());
                                                            gui.updateLog("[DEBUG] Raw decrypted: " + decryptedMsg);
                                                        }
                                                    }
                                                    return;
                                                }

                                                // Xử lý JSON thường
                                                try {
                                                    client.model.MessageResponse response = mapper.readValue(jsonStr, client.model.MessageResponse.class);
                                                    gui.updateScanResult(
                                                            response.getStatus(),
                                                            response.getResult(),
                                                            response.getFoundDomains(),
                                                            response.getTotalScanned(),
                                                            response.getTotalFound(),
                                                            response.getTargetDomain()
                                                    );
                                                } catch (Exception e) {
                                                    gui.updateLog("📨 Nhận response từ server: " + jsonStr.substring(0, Math.min(100, jsonStr.length())) + "...");
                                                }

                                            } catch (Exception e) {
                                                gui.updateLog("❌ Lỗi parse JSON hoàn chỉnh: " + e.getMessage());
                                                gui.updateLog("[DEBUG] Complete JSON: " + jsonStr);
                                                java.io.StringWriter sw = new java.io.StringWriter();
                                                e.printStackTrace(new java.io.PrintWriter(sw));
                                                gui.updateLog("[STACKTRACE] " + sw.toString());
                                            }
                                        }

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) {
                                            isConnected = true;
                                            channel = ctx.channel();
                                            gui.updateLog("✅ Đã kết nối thành công đến server!");
                                            gui.updateLog("📍 Server address: " + ctx.channel().remoteAddress());
                                            gui.setConnectionStatus(true);
                                        }

                                        @Override
                                        public void channelInactive(ChannelHandlerContext ctx) {
                                            isConnected = false;
                                            gui.updateLog("🔌 Đã ngắt kết nối từ server");
                                            gui.updateLog("📍 Server address: " + ctx.channel().remoteAddress());
                                            gui.setConnectionStatus(false);
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                            gui.updateLog("❌ Lỗi kết nối: " + cause.getMessage());
                                            isConnected = false;
                                            gui.setConnectionStatus(false);
                                            ctx.close();
                                        }
                                    }
                            );
                        }
                    });

            // Kết nối đồng bộ
            ChannelFuture f = b.connect(host, port).sync();

            gui.updateLog("✅ Kết nối thành công!");

        } catch (Exception e) {
            isConnected = false;
            gui.setConnectionStatus(false);
            gui.updateLog("❌ Lỗi kết nối: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg) {
        if (channel != null && channel.isActive() && isConnected) {
            channel.writeAndFlush(msg);
            gui.updateLog("📤 Đã gửi tin nhắn: " + msg);
        } else {
            gui.updateLog("❌ Không thể gửi tin nhắn - chưa kết nối đến server");
        }
    }

    public void sendSecureMessage(String rawMessage, String privateKeyPath, String publicKeyPath, String targetDomain) {
        try {
            if (!isConnected || channel == null || !channel.isActive()) {
                gui.updateLog("❌ Không thể gửi tin nhắn - chưa kết nối đến server");
                return;
            }

            if (serverPublicKey == null) {
                gui.updateLog("❌ Chưa nhận được public key của server, không thể mã hóa tin nhắn!");
                return;
            }

            gui.updateLog("🔐 Đang chuẩn bị gửi tin nhắn bảo mật (hybrid RSA+AES)...");

            // Kiểm tra file key
            if (!Files.exists(Paths.get(privateKeyPath))) {
                gui.updateLog("❌ Không tìm thấy private key: " + privateKeyPath);
                gui.updateLog("💡 Vui lòng sinh key trước khi gửi tin nhắn");
                return;
            }

            if (!Files.exists(Paths.get(publicKeyPath))) {
                gui.updateLog("❌ Không tìm thấy public key: " + publicKeyPath);
                gui.updateLog("💡 Vui lòng sinh key trước khi gửi tin nhắn");
                return;
            }

            // Load private key
            gui.updateLog("📖 Đang đọc private key...");
            PrivateKey privateKey = CryptoUtils.loadPrivateKey(privateKeyPath);

            // Ký rawMessage
            gui.updateLog("✍️ Đang ký tin nhắn...");
            String signature = CryptoUtils.sign(rawMessage, privateKey);

            // Tạo AES key/iv cho mã hóa message
            gui.updateLog("🔑 Đang sinh AES key và IV cho message...");
            SecretKey aesKey = CryptoUtils.generateAESKey();
            byte[] iv = CryptoUtils.generateIV();

            // Load public key bytes
            gui.updateLog("📖 Đang đọc public key (DER): " + publicKeyPath);
            byte[] pubKeyBytes = Files.readAllBytes(Paths.get(publicKeyPath));

            // Mã hóa public key bằng AES (cho phần gửi lên server như cũ)
            gui.updateLog("🔒 Đang mã hóa public key...");
            String publicKeyAes = CryptoUtils.encryptPublicKeyWithAES(pubKeyBytes, aesKey, iv);

            // Tạo message request (dữ liệu gốc)
            gui.updateLog("📦 Đang đóng gói tin nhắn...");
            MessageRequest req = new MessageRequest(
                    rawMessage,
                    signature,
                    publicKeyAes,
                    Base64.getEncoder().encodeToString(aesKey.getEncoded()),
                    Base64.getEncoder().encodeToString(iv),
                    targetDomain
            );

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(req);

            // HYBRID ENCRYPTION: Mã hóa json bằng AES, mã hóa AES key/iv bằng RSA
            gui.updateLog("🔒 Đang mã hóa toàn bộ message bằng AES...");
            String encryptedData = CryptoUtils.encryptMessageWithAES(json, aesKey, iv);

            gui.updateLog("🔒 Đang mã hóa AES key và IV bằng public key server (RSA)...");
            String encryptedKey = CryptoUtils.encryptWithPublicKey(aesKey.getEncoded(), serverPublicKey);
            String encryptedIv = CryptoUtils.encryptWithPublicKey(iv, serverPublicKey);

            // Đóng gói payload
            gui.updateLog("📦 Đang đóng gói payload hybrid encryption...");
            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("encryptedKey", encryptedKey);
            payload.put("encryptedIv", encryptedIv);
            payload.put("encryptedData", encryptedData);

            String payloadJson = mapper.writeValueAsString(payload);

            // Gửi tin nhắn
            gui.updateLog("📤 Đang gửi payload hybrid encryption đến server...");
            sendMessage(payloadJson);

            gui.updateLog("✅ Đã gửi tin nhắn bảo mật hybrid thành công!");

        } catch (Exception e) {
            gui.updateLog("❌ Lỗi gửi tin nhắn bảo mật: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (group != null) {
            if (channel != null && channel.isActive()) {
                channel.close();
            }
            group.shutdownGracefully();
            isConnected = false;
            gui.setConnectionStatus(false);
            gui.updateLog("🔌 Đã ngắt kết nối khỏi server");
        }
    }

    public boolean isConnected() {
        return isConnected && channel != null && channel.isActive();
    }

    public String getCurrentHost() {
        return currentHost;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    public Channel getChannel() {
        return channel;
    }

    // Hàm giải mã response mã hóa
    private String tryDecryptResponse(JsonNode node) {
        try {
            String privateKeyPath = gui.getPrivateKeyPath();
            if (privateKeyPath == null || privateKeyPath.isEmpty()) {
                gui.updateLog("❌ Không tìm thấy đường dẫn private key để giải mã!");
                return null;
            }
            // Log nội dung file private key (cắt đầu/cuối)
            try {
                java.nio.file.Path pkPath = java.nio.file.Paths.get(privateKeyPath);
                String pkContent = java.nio.file.Files.readString(pkPath);
                gui.updateLog("[DEBUG] Nội dung private key (đầu): " + pkContent.substring(0, Math.min(60, pkContent.length())) + "...");
                gui.updateLog("[DEBUG] Nội dung private key (cuối): " + pkContent.substring(Math.max(0, pkContent.length()-60)));
            } catch (Exception e) {
                gui.updateLog("[DEBUG] Không đọc được nội dung file private key: " + e.getMessage());
            }
            String b64Key = node.get("encryptedKey").asText();
            String b64Iv = node.get("encryptedIv").asText();
            String b64Data = node.get("encryptedData").asText();
            gui.updateLog("[DEBUG] encryptedKey (base64, đầu): " + b64Key.substring(0, Math.min(60, b64Key.length())) + "...");
            gui.updateLog("[DEBUG] encryptedIv (base64, đầu): " + b64Iv.substring(0, Math.min(60, b64Iv.length())) + "...");
            gui.updateLog("[DEBUG] encryptedData (base64, đầu): " + b64Data.substring(0, Math.min(60, b64Data.length())) + "...");
            PrivateKey privKey = client.crypto.CryptoUtils.loadPrivateKey(privateKeyPath);
            gui.updateLog("[DEBUG] PrivateKey type: " + privKey.getAlgorithm() + ", format: " + privKey.getFormat() + ", length: " + privKey.getEncoded().length);
            javax.crypto.Cipher rsaCipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(javax.crypto.Cipher.DECRYPT_MODE, privKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(java.util.Base64.getDecoder().decode(b64Key));
            byte[] iv = rsaCipher.doFinal(java.util.Base64.getDecoder().decode(b64Iv));
            gui.updateLog("[DEBUG] AES key length: " + aesKeyBytes.length + ", IV length: " + iv.length);
            javax.crypto.SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");
            javax.crypto.Cipher aesCipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey, new javax.crypto.spec.IvParameterSpec(iv));
            byte[] decrypted = aesCipher.doFinal(java.util.Base64.getDecoder().decode(b64Data));
            String decryptedStr = new String(decrypted);
            gui.updateLog("[DEBUG] Đã giải mã thành công, độ dài chuỗi: " + decryptedStr.length());
            return decryptedStr;
        } catch (Exception ex) {
            gui.updateLog("❌ Lỗi giải mã response: " + ex.getMessage());
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            gui.updateLog("[STACKTRACE] " + sw.toString());
            gui.updateLog("[DEBUG] Raw node: " + node.toString());
            return null;
        }
    }
} 