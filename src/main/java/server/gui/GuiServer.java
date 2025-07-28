package server.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.FlatLightLaf;

import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import server.network.NettyServer;
import java.io.File;

public class GuiServer extends JFrame {

    private JTextArea logArea;
    private JTextArea messageDetailsArea;
    private JTable subdomainTable;
    private NettyServer nettyServer;
    private JLabel statusLabel;
    private JLabel connectionCountLabel;
    private int connectionCount = 0;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public GuiServer() {
        setTitle("Secure Chat Server");
        setSize(1000, 750);
        setMinimumSize(new Dimension(850, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initLookAndFeel();
        initUI();
        setVisible(true);
    }

    public static void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Không thể khởi tạo FlatLaf: " + ex.getMessage());
        }
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Status panel top
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statusPanel.setBackground(new Color(230, 245, 230));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 180, 100)),
                new EmptyBorder(8, 15, 8, 15)
        ));

        statusLabel = new JLabel("🟢 Server đang chạy");
        statusLabel.setFont(new Font("Montserrat", Font.BOLD, 14));
        statusLabel.setForeground(new Color(40, 90, 40));
        statusPanel.add(statusLabel);

        connectionCountLabel = new JLabel("Kết nối: 0");
        connectionCountLabel.setFont(new Font("Poppins", Font.PLAIN, 12));
        statusPanel.add(connectionCountLabel);

        JLabel timeLabel = new JLabel("Thời gian: " + dateFormat.format(new Date()));
        timeLabel.setFont(new Font("Poppins", Font.PLAIN, 12));
        statusPanel.add(timeLabel);

        mainPanel.add(statusPanel, BorderLayout.NORTH);

        // Main split pane: left and right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);

        // Left Panel - comprises log area (top) and subdomain table (below, larger height)
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));

        // Log area - dark mode style
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setBackground(new Color(30, 30, 35));
        logArea.setForeground(new Color(200, 230, 200));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log hoạt động"));
        logScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        leftPanel.add(logScroll, BorderLayout.CENTER);

        // Subdomain Table with larger preferred height and scrollbars
        subdomainTable = new JTable();
        subdomainTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        subdomainTable.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(subdomainTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("🌐 Danh sách Subdomain"));
        tableScroll.setPreferredSize(new Dimension(0, 350));  // Tăng chiều cao lên 350px
        tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        leftPanel.add(tableScroll, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // Right Panel - Message Details and Control Buttons arranged vertically
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));

        // Message Details area - dark mode style
        messageDetailsArea = new JTextArea();
        messageDetailsArea.setEditable(false);
        messageDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messageDetailsArea.setBackground(new Color(30, 30, 35));
        messageDetailsArea.setForeground(new Color(200, 230, 200));
        JScrollPane detailsScroll = new JScrollPane(messageDetailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Chi tiết tin nhắn nhận được"));
        detailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        rightPanel.add(detailsScroll, BorderLayout.CENTER);

        // Control Panel with buttons at the bottom
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

        JButton clearLogBtn = createBtn("Xóa Log & Details", new Color(200, 50, 50));
        clearLogBtn.addActionListener(e -> {
            logArea.setText("");
            messageDetailsArea.setText("");
        });

        JButton refreshBtn = createBtn("🔁 Làm mới thời gian", new Color(60, 179, 113));
        refreshBtn.addActionListener(e -> {
            timeLabel.setText("Thời gian: " + dateFormat.format(new Date()));
        });

        JButton genKeyBtn = createBtn("🔑 Tạo cặp khóa", new Color(70, 130, 180));
        genKeyBtn.addActionListener(e -> {
            if (nettyServer != null) {
                try {
                    nettyServer.generateKeyPair();
                    updateLog("✅ Đã tạo cặp khóa RSA mới cho server!");
                } catch (Exception ex) {
                    updateLog("❌ Lỗi tạo cặp khóa: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                updateLog("❌ Server chưa khởi tạo!");
            }
        });

        controlPanel.add(clearLogBtn);
        controlPanel.add(refreshBtn);
        controlPanel.add(genKeyBtn);

        rightPanel.add(controlPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);

        // Timer để cập nhật thời gian mỗi giây
        Timer timer = new Timer(1000, e -> {
            timeLabel.setText("Thời gian: " + dateFormat.format(new Date()));
        });
        timer.start();
    }

    /**
     * Tạo nút bấm với style đặc trưng
     */
    private JButton createBtn(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Poppins", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(baseColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(baseColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(baseColor);
            }
        });
        return btn;
    }

    // Giữ nguyên các phương thức chức năng không chỉnh sửa logic

    public void updateLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = "[" + dateFormat.format(new Date()) + "] ";
            logArea.append(timestamp + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateMessageDetails(String details) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = "[" + dateFormat.format(new Date()) + "]\n";
            messageDetailsArea.append(timestamp + details + "\n\n");
            messageDetailsArea.setCaretPosition(messageDetailsArea.getDocument().getLength());
        });
    }

    public void updateConnectionCount(int count) {
        SwingUtilities.invokeLater(() -> {
            connectionCount = count;
            connectionCountLabel.setText("Kết nối: " + count);
        });
    }

    public void updateScanDomains(List<String> domains) {
        SwingUtilities.invokeLater(() -> {
            String[] columnNames = {"#", "Domain"};
            String[][] data = new String[domains.size()][2];
            for (int i = 0; i < domains.size(); i++) {
                data[i][0] = String.valueOf(i + 1);
                data[i][1] = domains.get(i);
            }
            subdomainTable.setModel(new DefaultTableModel(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            });
            subdomainTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        });
    }

    public void setServerStatus(boolean isRunning) {
        SwingUtilities.invokeLater(() -> {
            if (isRunning) {
                statusLabel.setText("🟢 Server đang chạy");
                statusLabel.setForeground(new Color(0, 150, 0));
            } else {
                statusLabel.setText("🔴 Server đã dừng");
                statusLabel.setForeground(Color.RED);
            }
        });
    }

    public void setNettyServer(NettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }
}
