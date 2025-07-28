package server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import server.handler.NettyServerHandler;
import server.gui.GuiServer;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Quản lý kết nối Netty server, cấu hình pipeline và handler.
 */
public class NettyServer {
    private final GuiServer gui;
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private AtomicInteger connectionCount = new AtomicInteger(0);
    private boolean isRunning = false;

    // Biến lưu cặp khóa RSA
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public NettyServer(GuiServer gui) {
        this.gui = gui;
        // Load key nếu đã tồn tại
        try {
            String privPath = "src/main/resources/server_private_key.pem";
            String pubPath = "src/main/resources/server_public_key.pem";
            if (Files.exists(Paths.get(privPath)) && Files.exists(Paths.get(pubPath))) {
                this.privateKey = server.crypto.CryptoUtils.loadPrivateKey(privPath);
                this.publicKey = server.crypto.CryptoUtils.loadPublicKey(pubPath);
                gui.updateLog("🔑 Đã load private key từ: " + privPath);
                gui.updateLog("🔑 Đã load public key từ: " + pubPath);
            } else {
                gui.updateLog("⚠️ Chưa có key server, hãy tạo mới bằng nút 'Tạo cặp khóa'.");
            }
        } catch (Exception e) {
            gui.updateLog("❌ Lỗi load key server: " + e.getMessage());
        }
    }

    // Sinh cặp khóa RSA mới
    public void generateKeyPair() throws Exception {
        KeyPair keyPair = server.crypto.CryptoUtils.generateRSAKeyPair(2048);
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        // Lưu key ra file giống client
        String privPath = "src/main/resources/server_private_key.pem";
        String pubPath = "src/main/resources/server_public_key.pem";
        server.crypto.CryptoUtils.savePrivateKey(privateKey, privPath);
        server.crypto.CryptoUtils.savePublicKey(publicKey, pubPath);
        gui.updateLog("🔑 Đã lưu private key tại: " + privPath);
        gui.updateLog("🔑 Đã lưu public key tại: " + pubPath);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void start(int port) {
        gui.updateLog("🚀 Đang khởi động server tại cổng " + port + "...");

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<Channel>() {
                 @Override
                 protected void initChannel(Channel ch) {
                     gui.updateLog("🔧 Thiết lập pipeline cho kết nối mới: " + ch.remoteAddress());

                     // Theo dõi kết nối trước
                     ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                         @Override
                         public void channelActive(ChannelHandlerContext ctx) {
                             int count = connectionCount.incrementAndGet();
                             gui.updateConnectionCount(count);
                             gui.updateLog("🔗 Client kết nối mới - Tổng: " + count);
                             gui.updateLog("📍 Client address: " + ctx.channel().remoteAddress());
                             ctx.fireChannelActive();
                         }

                         @Override
                         public void channelInactive(ChannelHandlerContext ctx) {
                             int count = connectionCount.decrementAndGet();
                             gui.updateConnectionCount(count);
                             gui.updateLog("🔌 Client ngắt kết nối - Còn lại: " + count);
                             gui.updateLog("📍 Client address: " + ctx.channel().remoteAddress());
                             ctx.fireChannelInactive();
                         }

                         @Override
                         public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                             gui.updateLog("❌ Lỗi kết nối client: " + cause.getMessage());
                             cause.printStackTrace();
                             ctx.close();
                         }
                     });

                     // Sau đó thêm encoder/decoder và handler chính
                     ch.pipeline().addLast(new StringEncoder(), new StringDecoder(), new server.handler.NettyServerHandler(gui, NettyServer.this));

                     gui.updateLog("✅ Pipeline đã được thiết lập cho kết nối mới");
                 }
             });

            gui.updateLog("🔧 Đang bind server đến cổng " + port + "...");
            ChannelFuture f = b.bind(port).sync();
            channel = f.channel();
            isRunning = true;
            gui.setServerStatus(true);
            gui.updateLog("🚀 Server đã khởi động thành công!");
            gui.updateLog("📡 Đang lắng nghe tại cổng " + port);
            gui.updateLog("✅ Server sẵn sàng nhận kết nối từ client");
            gui.updateLog("📍 Server address: " + channel.localAddress());

        } catch (Exception e) {
            isRunning = false;
            gui.setServerStatus(false);
            gui.updateLog("❌ Lỗi khởi động server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
        gui.setServerStatus(false);
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        gui.updateLog("🛑 Server đã dừng");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getConnectionCount() {
        return connectionCount.get();
    }
} 