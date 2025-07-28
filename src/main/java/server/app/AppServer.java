package server.app;

import server.gui.GuiServer;
import server.network.NettyServer;

import javax.swing.*;

public class AppServer {
    public static void main(String[] args) {
        GuiServer.initLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            GuiServer gui = new GuiServer();
            gui.setVisible(true);
            NettyServer nettyServer = new NettyServer(gui);
            gui.setNettyServer(nettyServer);

            // Khởi động server trên thread riêng, log ra GUI
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Đợi GUI load xong
                    gui.updateLog("🚀 Đang khởi động server tại cổng 8080...");
                    nettyServer.start(8080);
                } catch (Exception e) {
                    gui.updateLog("❌ Lỗi khởi động server: " + e.getMessage());
                }
            }).start();
        });
    }
} 