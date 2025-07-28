package server.handler;

import io.netty.channel.*;
import server.gui.GuiServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.model.MessageRequest;
import server.model.MessageResponse;
import server.crypto.CryptoUtils;
import server.scanner.SubdomainScanner;
import server.utils.KeyIvDatabase;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import server.network.NettyServer;
import java.security.PrivateKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Xử lý message đến/đi cho Netty server, cập nhật log GUI.
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {
    private final GuiServer gui;
    private final NettyServer nettyServer;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private PublicKey lastClientPublicKey;

    public NettyServerHandler(GuiServer gui, NettyServer nettyServer) {
        this.gui = gui;
        this.nettyServer = nettyServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            // Kiểm tra nếu message là hybrid encryption (JSON có encryptedKey, encryptedIv, encryptedData)
            if (msg.trim().startsWith("{")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(msg);
                if (node.has("encryptedKey") && node.has("encryptedIv") && node.has("encryptedData")) {
                    gui.updateLog("🔓 Nhận payload hybrid encryption, đang giải mã bằng private key server...");
                    String encryptedKey = node.get("encryptedKey").asText();
                    String encryptedIv = node.get("encryptedIv").asText();
                    String encryptedData = node.get("encryptedData").asText();
                    java.security.PrivateKey privKey = nettyServer.getPrivateKey();
                    if (privKey == null) throw new Exception("Server chưa có private key!");
                    javax.crypto.Cipher rsaCipher = javax.crypto.Cipher.getInstance("RSA");
                    rsaCipher.init(javax.crypto.Cipher.DECRYPT_MODE, privKey);
                    byte[] aesKeyBytes = rsaCipher.doFinal(java.util.Base64.getDecoder().decode(encryptedKey));
                    byte[] iv = rsaCipher.doFinal(java.util.Base64.getDecoder().decode(encryptedIv));
                    javax.crypto.SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");
                    javax.crypto.Cipher aesCipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
                    aesCipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey, new javax.crypto.spec.IvParameterSpec(iv));
                    byte[] decrypted = aesCipher.doFinal(java.util.Base64.getDecoder().decode(encryptedData));
                    msg = new String(decrypted);
                    gui.updateLog("🔓 Đã giải mã thành công payload hybrid encryption!");
                }
            }
            gui.updateLog("📨 Nhận tin nhắn từ client: " + ctx.channel().remoteAddress());

            // Parse JSON thành MessageRequest
            ObjectMapper mapper = new ObjectMapper();
            MessageRequest req = mapper.readValue(msg, MessageRequest.class);

            // Hiển thị thông tin chi tiết về tin nhắn
            StringBuilder details = new StringBuilder();
            details.append("=== CHI TIẾT TIN NHẮN NHẬN ĐƯỢC ===\n\n");
            details.append("Thời gian: ").append(dateFormat.format(new Date())).append("\n");
            details.append("Client: ").append(ctx.channel().remoteAddress()).append("\n\n");

            details.append("📝 Nội dung tin nhắn:\n");
            details.append("Raw Message: ").append(req.rawMessage).append("\n\n");

            details.append("🔑 Thông tin Key:\n");
            details.append("Public Key (Base64): ").append(req.publicKeyAes.substring(0, Math.min(50, req.publicKeyAes.length()))).append("...\n");
            details.append("AES Key (Base64): ").append(req.aesKey.substring(0, Math.min(30, req.aesKey.length()))).append("...\n");
            details.append("AES IV (Base64): ").append(req.aesIv.substring(0, Math.min(30, req.aesIv.length()))).append("...\n\n");

            details.append("✍️ Thông tin chữ ký:\n");
            details.append("Signature (Base64): ").append(req.signature.substring(0, Math.min(50, req.signature.length()))).append("...\n");
            details.append("Signature Length: ").append(req.signature.length()).append(" characters\n\n");

            // Giải mã AES key/iv
            byte[] keyBytes = Base64.getDecoder().decode(req.aesKey);
            byte[] ivBytes = Base64.getDecoder().decode(req.aesIv);
            SecretKey aesKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");

            details.append("🔓 Thông tin giải mã:\n");
            details.append("AES Key Length: ").append(keyBytes.length).append(" bytes\n");
            details.append("AES IV Length: ").append(ivBytes.length).append(" bytes\n");
            details.append("AES Algorithm: ").append(aesKey.getAlgorithm()).append("\n\n");

            // Giải mã public key từ AES
            byte[] encryptedPubKeyBytes = Base64.getDecoder().decode(req.publicKeyAes);
            byte[] pubKeyBytes = CryptoUtils.decryptWithAES(encryptedPubKeyBytes, aesKey, ivBytes);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            // Luôn dùng public key này để mã hóa response
            lastClientPublicKey = publicKey;

            details.append("🔐 Thông tin Public Key:\n");
            details.append("Algorithm: ").append(publicKey.getAlgorithm()).append("\n");
            details.append("Format: ").append(publicKey.getFormat()).append("\n");
            details.append("Length: ").append(publicKey.getEncoded().length).append(" bytes\n\n");

            // Xác thực chữ ký
            boolean verified = CryptoUtils.verify(req.rawMessage, req.signature, publicKey);

            details.append("✅ Kết quả xác thực:\n");
            details.append("Signature Verified: ").append(verified ? "✅ HỢP LỆ" : "❌ KHÔNG HỢP LỆ").append("\n\n");

            if (verified) {
                gui.updateLog("[DEBUG] Xác thực chữ ký thành công, bắt đầu xử lý scan domain.");
                details.append("🔄 Xử lý tiếp theo:\n");
                details.append("- Lưu key/iv vào database\n");
                details.append("- Thực hiện scan subdomain\n");
                details.append("- Gửi kết quả về client\n");
                try {
                    // Lưu key/iv vào database (chỉ lưu AES key và IV, không lưu public key)
                    String clientIp = ctx.channel().remoteAddress().toString();
                    KeyIvDatabase.save(new server.model.KeyIvRecord(req.aesKey, req.aesIv, req.targetDomain, clientIp));
                    
                    // Kiểm tra và cleanup database nếu cần
                    checkAndCleanupDatabase();
                    
                    // Thực hiện scan subdomain
                    StringBuilder result = new StringBuilder();
                    // Lấy domain cần scan từ request
                    String targetDomain = req.getTargetDomain() != null && !req.getTargetDomain().isEmpty() ? req.getTargetDomain() : "huflit.edu.vn";
                    gui.updateLog("🔍 Đang scan subdomain cho domain: " + targetDomain);
                    // Sử dụng scan DNS với domain động
                    SubdomainScanner.ScanResult scanResult = SubdomainScanner.scanDNSWithStats("src/main/resources/subdomains-top1million-110000.txt", targetDomain);
                    if (scanResult.totalScanned == 0) {
                        // Fallback về wordlist cũ nếu file lớn không có
                        gui.updateLog("[DEBUG] File subdomains-top1million-110000.txt không có hoặc không tìm thấy, fallback sang wordlist.txt");
                        scanResult = SubdomainScanner.scanDNSWithStats("src/main/resources/wordlist.txt");
                    }
                    gui.updateLog("[DEBUG] Scan xong, tổng domain đã scan: " + scanResult.totalScanned + ", tìm thấy: " + scanResult.totalFound);
                    // Tạo kết quả chi tiết
                    for (String domain : scanResult.foundDomains) {
                        result.append(domain).append("\n");
                    }
                    // Gửi kết quả chi tiết về client (CHIA NHỎ nếu quá nhiều domain)
                    int batchSize = 10; // Giảm batch nhỏ để tránh cắt dòng
                    List<String> allDomains = scanResult.foundDomains;
                    int totalScanned = scanResult.totalScanned;
                    int totalFound = scanResult.totalFound;
                    for (int i = 0; i < allDomains.size(); i += batchSize) {
                        List<String> batch = allDomains.subList(i, Math.min(i + batchSize, allDomains.size()));
                        StringBuilder batchResult = new StringBuilder();
                        for (String domain : batch) {
                            batchResult.append(domain).append("\n");
                        }
                        MessageResponse resp = new MessageResponse(
                                "OK",
                                batchResult.toString(),
                                batch,
                                totalScanned,
                                totalFound
                        );
                        resp.totalNotFound = scanResult.getTotalNotFound();
                        resp.setTargetDomain(targetDomain);
                        gui.updateLog("[DEBUG] Gửi batch: " + batch.size() + "/" + allDomains.size() + " domain");
                        sendEncryptedResponse(ctx, resp, publicKey); // dùng publicKey vừa nhận
                        ctx.flush();
                    }
                    // Nếu không có domain nào, vẫn gửi 1 response rỗng
                    if (allDomains.isEmpty()) {
                        MessageResponse resp = new MessageResponse(
                                "OK",
                                "",
                                new java.util.ArrayList<>(),
                                totalScanned,
                                totalFound
                        );
                        resp.totalNotFound = scanResult.getTotalNotFound();
                        resp.setTargetDomain(targetDomain);
                        sendEncryptedResponse(ctx, resp, publicKey); // dùng publicKey vừa nhận
                        ctx.flush();
                    }
                    // Hiển thị domain lên GUI
                    gui.updateScanDomains(scanResult.foundDomains);
                    gui.updateLog("✅ Đã xử lý thành công và gửi kết quả về client");
                    details.append("\n📤 Kết quả gửi về:\n");
                    details.append("Status: OK\n");
                    details.append("Total Scanned: ").append(scanResult.totalScanned).append("\n");
                    details.append("Total Found: ").append(scanResult.totalFound).append("\n");
                    details.append("Total Not Found: ").append(scanResult.getTotalNotFound()).append("\n");
                    details.append("Found Domains: ").append(scanResult.foundDomains.size()).append("\n");
                } catch (Exception scanEx) {
                    gui.updateLog("❌ Lỗi khi scan hoặc gửi domain: " + scanEx.getMessage());
                    java.io.StringWriter sw = new java.io.StringWriter();
                    scanEx.printStackTrace(new java.io.PrintWriter(sw));
                    gui.updateLog("[STACKTRACE] " + sw.toString());
                }
            } else {
                // Trả về lỗi xác thực
                MessageResponse resp = new MessageResponse("VERIFICATION_FAILED", "Chữ ký không hợp lệ");
                sendEncryptedResponse(ctx, resp, publicKey); // dùng publicKey vừa nhận
                gui.updateLog("❌ Chữ ký không hợp lệ - từ chối tin nhắn");
                details.append("\n❌ Kết quả:\n");
                details.append("Status: VERIFICATION_FAILED\n");
                details.append("Reason: Chữ ký không hợp lệ\n");
            }

            // Hiển thị chi tiết trong GUI
            gui.updateMessageDetails(details.toString());

        } catch (Exception e) {
            gui.updateLog("❌ Lỗi xử lý: " + e.getMessage());
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                gui.updateLog("[STACKTRACE] " + sw.toString());
                ObjectMapper mapper = new ObjectMapper();
                MessageResponse resp = new MessageResponse("VERIFICATION_FAILED", "Lỗi xử lý: " + e.getMessage());
                // Nếu có public key client, mã hóa response
                if (lastClientPublicKey != null) {
                    javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
                    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, lastClientPublicKey);
                    byte[] encrypted = cipher.doFinal(mapper.writeValueAsString(resp).getBytes());
                    String base64 = java.util.Base64.getEncoder().encodeToString(encrypted);
                    ctx.writeAndFlush("ENCRYPTED:" + base64);
                } else {
                    ctx.writeAndFlush(mapper.writeValueAsString(resp));
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        gui.updateLog("❌ Lỗi kết nối: " + cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        gui.updateLog("🔗 Client kết nối: " + ctx.channel().remoteAddress());
        // Gửi public key cho client
        try {
            PublicKey pubKey = nettyServer.getPublicKey();
            if (pubKey != null) {
                String base64PubKey = java.util.Base64.getEncoder().encodeToString(pubKey.getEncoded());
                ctx.writeAndFlush("PUBLIC_KEY:" + base64PubKey + "\n");
                gui.updateLog("📤 Đã gửi public key cho client");
            } else {
                gui.updateLog("⚠️ Chưa có public key để gửi cho client!");
            }
        } catch (Exception e) {
            gui.updateLog("❌ Lỗi gửi public key: " + e.getMessage());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        gui.updateLog("🔌 Client ngắt kết nối: " + ctx.channel().remoteAddress());
    }

    // Trong NettyServerHandler.java, sửa phương thức sendEncryptedResponse:

    private void checkAndCleanupDatabase() {
        int recordCount = KeyIvDatabase.getRecordCount();
        long dbSize = KeyIvDatabase.getDatabaseSize();
        
        // Cleanup nếu quá nhiều records hoặc file quá lớn
        if (recordCount > 5000 || dbSize > 50 * 1024 * 1024) { // 50MB
            gui.updateLog("🧹 Database quá lớn, thực hiện cleanup...");
            KeyIvDatabase.cleanupOldRecords(30); // Xóa records cũ hơn 30 ngày
            
            int newCount = KeyIvDatabase.getRecordCount();
            long newSize = KeyIvDatabase.getDatabaseSize();
            gui.updateLog("✅ Cleanup xong: " + newCount + " records, " + (newSize/1024/1024) + " MB");
        }
    }

    private void sendEncryptedResponse(ChannelHandlerContext ctx, Object responseObj, PublicKey clientPublicKey) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(responseObj);

        // Sinh AES key/iv
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        byte[] iv = new byte[16];
        new java.security.SecureRandom().nextBytes(iv);

        // Mã hóa dữ liệu bằng AES
        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] encryptedData = aesCipher.doFinal(json.getBytes());

        // Mã hóa AES key/iv bằng RSA
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
        byte[] encryptedKey = rsaCipher.doFinal(aesKey.getEncoded());
        byte[] encryptedIv = rsaCipher.doFinal(iv);

        // Tạo response object
        String b64Key = java.util.Base64.getEncoder().encodeToString(encryptedKey);
        String b64Iv = java.util.Base64.getEncoder().encodeToString(encryptedIv);
        String b64Data = java.util.Base64.getEncoder().encodeToString(encryptedData);

        // Log chi tiết
        gui.updateLog("[DEBUG] Gửi response: encryptedKey=" + b64Key.substring(0, Math.min(60, b64Key.length())) + "...");
        gui.updateLog("[DEBUG] Gửi response: encryptedIv=" + b64Iv.substring(0, Math.min(60, b64Iv.length())) + "...");
        gui.updateLog("[DEBUG] Gửi response: encryptedData=" + b64Data.substring(0, Math.min(60, b64Data.length())) + "...");

        // Tạo JSON payload
        java.util.Map<String, String> payloadMap = new java.util.HashMap<>();
        payloadMap.put("encryptedKey", b64Key);
        payloadMap.put("encryptedIv", b64Iv);
        payloadMap.put("encryptedData", b64Data);

        String payload = mapper.writeValueAsString(payloadMap);

        // Gửi với format rõ ràng
        String messageToSend = payload + "\n---END---\n";

        gui.updateLog("[DEBUG] Độ dài payload: " + payload.length() + " chars");
        gui.updateLog("[DEBUG] Message gửi: " + messageToSend.length() + " chars total");

        ctx.write(messageToSend);
        ctx.flush();
    }
}