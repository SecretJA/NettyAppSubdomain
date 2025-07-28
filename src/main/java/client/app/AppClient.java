package client.app;

import client.gui.GuiClient;
import client.network.NettyClient;

/**
 * Điểm khởi đầu của ứng dụng Client.
 * Khởi tạo giao diện GUI và kết nối Netty.
 */
public class AppClient {
    public static void main(String[] args) {
        System.out.println("🚀 Khởi động Secure Chat Client...");
        
        // Khởi tạo giao diện FlatLaf (Light/Dark)
        GuiClient.initLookAndFeel();
        
        // Khởi tạo GUI
        GuiClient gui = new GuiClient();
        
        // Khởi tạo Netty client và truyền GUI để cập nhật log
        NettyClient nettyClient = new NettyClient(gui);
        gui.setNettyClient(nettyClient);
        
        // Hiển thị GUI
        gui.setVisible(true);
        System.out.println("💡 Bấm nút '🔗 Kết nối' để kết nối đến server");
    }
} 