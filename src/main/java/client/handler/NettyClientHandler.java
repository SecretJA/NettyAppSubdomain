package client.handler;

import io.netty.channel.*;
import client.gui.GuiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.model.MessageResponse;

/**
 * Xử lý message đến/đi cho Netty client, cập nhật log GUI.
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<String> {
    private final GuiClient gui;

    public NettyClientHandler(GuiClient gui) {
        this.gui = gui;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        // Không xử lý gì ở đây nữa, mọi xử lý đã chuyển sang NettyClient
        // Nếu cần log debug:
        gui.updateLog("[DEBUG] Đã nhận message ở handler phụ: " + msg.substring(0, Math.min(100, msg.length())) + "...");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        gui.updateLog("❌ Lỗi kết nối: " + cause.getMessage());
        cause.printStackTrace();
        // Không tự động đóng kết nối, để người dùng quyết định
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        gui.updateLog("🔗 Kết nối thành công đến server: " + ctx.channel().remoteAddress());
        // Không set connection status ở đây vì đã được set trong NettyClient
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        gui.updateLog("🔌 Ngắt kết nối từ server: " + ctx.channel().remoteAddress());
        // Không set connection status ở đây vì đã được set trong NettyClient
    }
} 