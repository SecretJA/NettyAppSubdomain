package client.gui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import client.network.NettyClient;
import client.crypto.CryptoUtils;

import java.io.File;

public class GuiClient extends JFrame {

    private JTextArea logArea;
    private NettyClient nettyClient;
    private JTextField rawMessageField;
    private JTextField privateKeyField;
    private JTextField publicKeyField;
    private JTextArea keyInfoArea;
    private JLabel statusLabel;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JButton sendBtn;
    private JTextArea scanResultArea;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private JTextField targetDomainField;
    private List<String> allFoundDomains = new ArrayList<>();
    private int lastTotalScanned = 0;
    private int lastTotalFound = 0;
    private String lastTargetDomain = "";

    public GuiClient() {
        setTitle("Secure Chat Client");
        setSize(1000, 700);
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
            System.err.println("Không thể khởi tạo FlatLaf");
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

        statusLabel = new JLabel("🔵 Client sẵn sàng");
        statusLabel.setFont(new Font("Montserrat", Font.BOLD, 14));
        statusLabel.setForeground(new Color(40, 90, 40));
        statusPanel.add(statusLabel);

        JLabel timeLabel = new JLabel("Thời gian: " + dateFormat.format(new Date()));
        timeLabel.setFont(new Font("Poppins", Font.PLAIN, 12));
        statusPanel.add(timeLabel);

        mainPanel.add(statusPanel, BorderLayout.NORTH);

        // Split pane left/right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(520);
        splitPane.setResizeWeight(0.5);

        // Left panel vertical layout
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout(10, 10));

        // Top input panel - fixed height ~180 px
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Nhập tin nhắn và Key"));
        inputPanel.setPreferredSize(new Dimension(0, 180));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel msgLabel = new JLabel("Tin nhắn:");
        msgLabel.setFont(new Font("Poppins", Font.PLAIN, 13));
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        inputPanel.add(msgLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        rawMessageField = new JTextField();
        rawMessageField.setFont(new Font("Poppins", Font.PLAIN, 14));
        inputPanel.add(rawMessageField, gbc);

        JLabel privKeyLabel = new JLabel("Private Key:");
        privKeyLabel.setFont(new Font("Poppins", Font.PLAIN, 13));
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        inputPanel.add(privKeyLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        privateKeyField = new JTextField("src/main/resources/client_private_key.pem");
        privateKeyField.setFont(new Font("Poppins", Font.PLAIN, 14));
        inputPanel.add(privateKeyField, gbc);

        JLabel pubKeyLabel = new JLabel("Public Key:");
        pubKeyLabel.setFont(new Font("Poppins", Font.PLAIN, 13));
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(pubKeyLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        publicKeyField = new JTextField("src/main/resources/client_public_key.der");
        publicKeyField.setFont(new Font("Poppins", Font.PLAIN, 14));
        inputPanel.add(publicKeyField, gbc);

        JLabel domainLabel = new JLabel("Domain cần scan:");
        domainLabel.setFont(new Font("Poppins", Font.PLAIN, 13));
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(domainLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        targetDomainField = new JTextField("huflit.edu.vn");
        targetDomainField.setFont(new Font("Poppins", Font.PLAIN, 14));
        inputPanel.add(targetDomainField, gbc);

        leftPanel.add(inputPanel, BorderLayout.NORTH);

        // Central panel contains key info and button+scanResult panel stacked vertically
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout(5, 5));

        // Key Info area: fix height small (~70 px), scrolls when overflow
        keyInfoArea = new JTextArea();
        keyInfoArea.setEditable(false);
        keyInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        keyInfoArea.setLineWrap(false);
        keyInfoArea.setWrapStyleWord(false);
        keyInfoArea.setBackground(new Color(245, 245, 245));
        JScrollPane keyScroll = new JScrollPane(keyInfoArea);
        keyScroll.setBorder(BorderFactory.createTitledBorder("Thông tin Key"));
        Dimension keyScrollDim = new Dimension(550, 70);
        keyScroll.setPreferredSize(keyScrollDim);
        keyScroll.setMinimumSize(new Dimension(300,70));
        keyScroll.setMaximumSize(keyScrollDim);
        keyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        keyScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        centerPanel.add(keyScroll, BorderLayout.NORTH);

        // Bottom panel for buttons and scan result, split vertically
        JPanel buttonsAndScanPanel = new JPanel(new BorderLayout(10, 10));
        buttonsAndScanPanel.setPreferredSize(new Dimension(0, 350)); // allow large height for scan

        // Buttons fixed height (~60 px)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        buttonPanel.setPreferredSize(new Dimension(0, 60));

        connectBtn = createBtn("🔗 Kết nối", new Color(255, 140, 0));
        disconnectBtn = createBtn("🔌 Ngắt kết nối", new Color(220, 20, 60));
        disconnectBtn.setEnabled(false);
        sendBtn = createBtn("🌐 Scan huflit", new Color(34, 139, 34));
        sendBtn.setEnabled(false);
        JButton generateKeyBtn = createBtn("🔑 Sinh Key Tự động", new Color(70, 130, 180));
        JButton searchDomainBtn = createBtn("🔍 Tìm kiếm domain", new Color(0, 123, 255));
        searchDomainBtn.setEnabled(false);

        connectBtn.addActionListener(e -> {
            if (nettyClient != null) {
                updateLog("Đang kết nối đến server...");
                setStatus("🟡 Đang kết nối...", new Color(180, 130, 0));
                nettyClient.connect("localhost", 8080);
            }
        });

        disconnectBtn.addActionListener(e -> {
            if (nettyClient != null) {
                nettyClient.disconnect();
            }
        });

        sendBtn.addActionListener(e -> {
            if (nettyClient != null) {
                nettyClient.sendSecureMessage(
                        rawMessageField.getText(),
                        privateKeyField.getText(),
                        publicKeyField.getText(),
                        "huflit.edu.vn"
                );
            }
        });

        generateKeyBtn.addActionListener(e -> generateKeys());

        searchDomainBtn.addActionListener(e -> {
            if (nettyClient != null) {
                nettyClient.sendSecureMessage(
                        rawMessageField.getText(),
                        privateKeyField.getText(),
                        publicKeyField.getText(),
                        targetDomainField.getText()
                );
            }
        });

        buttonPanel.add(connectBtn);
        buttonPanel.add(disconnectBtn);
        buttonPanel.add(sendBtn);
        buttonPanel.add(searchDomainBtn);
        buttonPanel.add(generateKeyBtn);

        buttonsAndScanPanel.add(buttonPanel, BorderLayout.NORTH);

        // Scan Result - Use scroll pane that fills rest of bottom panel
        scanResultArea = new JTextArea();
        scanResultArea.setEditable(false);
        scanResultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scanResultArea.setLineWrap(false);
        scanResultArea.setWrapStyleWord(false);
        scanResultArea.setBackground(new Color(248, 249, 250));
        JScrollPane scanScroll = new JScrollPane(scanResultArea);
        scanScroll.setBorder(BorderFactory.createTitledBorder("🌐 Kết quả Scan Subdomain"));
        scanScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scanScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        buttonsAndScanPanel.add(scanScroll, BorderLayout.CENTER);

        centerPanel.add(buttonsAndScanPanel, BorderLayout.CENTER);

        // Add centerPanel into leftPanel center
        leftPanel.add(centerPanel, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);

        // Right panel - log area
        JPanel rightPanel = new JPanel(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setBackground(new Color(30, 30, 35));
        logArea.setForeground(new Color(200, 230, 200));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log hoạt động"));
        rightPanel.add(logScroll, BorderLayout.CENTER);

        JPanel logControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearLogBtn = createBtn("Xóa Log", new Color(150, 30, 30));
        clearLogBtn.addActionListener(e -> logArea.setText(""));
        logControlPanel.add(clearLogBtn);
        rightPanel.add(logControlPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);

        Timer timer = new Timer(1000, e -> timeLabel.setText("Thời gian: " + dateFormat.format(new Date())));
        timer.start();
    }

    private JButton createBtn(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Poppins", Font.BOLD, 13));
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

    private void generateKeys() {
        try {
            updateLog("Đang sinh cặp key RSA...");
            setStatus("🟡 Đang sinh key...", new Color(180, 130, 0));
            java.security.KeyPair keyPair = CryptoUtils.generateRSAKeyPair();

            java.security.PrivateKey privateKey = keyPair.getPrivate();
            java.security.PublicKey publicKey = keyPair.getPublic();

            File resourcesDir = new File("src/main/resources");
            if (!resourcesDir.exists()) resourcesDir.mkdirs();

            String privateKeyPath = "src/main/resources/client_private_key.pem";
            CryptoUtils.savePrivateKeyToFile(privateKey, privateKeyPath);
            String publicKeyDerPath = "src/main/resources/client_public_key.der";
            CryptoUtils.savePublicKeyToDer(publicKey, publicKeyDerPath);
            String publicKeyPemPath = "src/main/resources/client_public_key.pem";
            CryptoUtils.savePublicKeyToPem(publicKey, publicKeyPemPath);

            privateKeyField.setText(privateKeyPath);
            publicKeyField.setText(publicKeyDerPath);

            updateKeyInfo(privateKey, publicKey);
            updateLog("✅ Đã sinh và lưu cặp key thành công!");
            updateLog("Private Key: " + privateKeyPath);
            updateLog("Public Key (DER): " + publicKeyDerPath);
            updateLog("Public Key (PEM): " + publicKeyPemPath);
            setStatus("🟢 Key đã sẵn sàng", new Color(0, 150, 0));
        } catch (Exception e) {
            updateLog("❌ Lỗi khi sinh key: " + e.getMessage());
            setStatus("🔴 Lỗi sinh key", Color.RED);
            e.printStackTrace();
        }
    }

    private void updateKeyInfo(java.security.PrivateKey privateKey, java.security.PublicKey publicKey) {
        StringBuilder info = new StringBuilder();
        info.append("=== THÔNG TIN KEY ===\n\n");
        info.append("Private Key Algorithm: ").append(privateKey.getAlgorithm()).append("\n");
        info.append("Private Key Format: ").append(privateKey.getFormat()).append("\n");
        info.append("Private Key Length: ").append(privateKey.getEncoded().length).append(" bytes\n\n");
        info.append("Public Key Algorithm: ").append(publicKey.getAlgorithm()).append("\n");
        info.append("Public Key Format: ").append(publicKey.getFormat()).append("\n");
        info.append("Public Key Length: ").append(publicKey.getEncoded().length).append(" bytes\n\n");
        info.append("Key được lưu tại:\n");
        info.append("- Private: ").append(privateKeyField.getText()).append("\n");
        info.append("- Public: ").append(publicKeyField.getText()).append("\n");
        keyInfoArea.setText(info.toString());
        keyInfoArea.setCaretPosition(0);
    }

    public void updateLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = "[" + dateFormat.format(new Date()) + "] ";
            logArea.append(timestamp + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    public void setConnectionStatus(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                setStatus("🟢 Đã kết nối", new Color(0, 150, 0));
                connectBtn.setEnabled(false);
                disconnectBtn.setEnabled(true);
                sendBtn.setEnabled(true);
                // Enable nút tìm kiếm domain khi kết nối
                for (Component c : ((JPanel) sendBtn.getParent()).getComponents()) {
                    if (c instanceof JButton && ((JButton) c).getText().contains("Tìm kiếm domain")) {
                        c.setEnabled(true);
                    }
                }
                updateLog("✅ Trạng thái GUI đã cập nhật: Đã kết nối");
            } else {
                setStatus("🔴 Chưa kết nối", Color.RED);
                connectBtn.setEnabled(true);
                disconnectBtn.setEnabled(false);
                sendBtn.setEnabled(false);
                // Disable nút tìm kiếm domain khi ngắt kết nối
                for (Component c : ((JPanel) sendBtn.getParent()).getComponents()) {
                    if (c instanceof JButton && ((JButton) c).getText().contains("Tìm kiếm domain")) {
                        c.setEnabled(false);
                    }
                }
                updateLog("❌ Trạng thái GUI đã cập nhật: Chưa kết nối");
            }
        });
    }

    public void setNettyClient(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    public void updateScanResult(String status, String result,
                                 List<String> foundDomains, int totalScanned,
                                 int totalFound, String targetDomain) {
        SwingUtilities.invokeLater(() -> {
            boolean isNewDomain = !targetDomain.equals(lastTargetDomain);
            if (isNewDomain) {
                allFoundDomains.clear();
                lastTargetDomain = targetDomain;
                updateLog("🔄 Bắt đầu scan mới cho domain: " + targetDomain + ", reset danh sách subdomain!");
            }
            int before = allFoundDomains.size();
            if (foundDomains != null && !foundDomains.isEmpty()) {
                for (String d : foundDomains) {
                    if (!allFoundDomains.contains(d))
                        allFoundDomains.add(d);
                }
            }
            int after = allFoundDomains.size();
            lastTotalScanned = totalScanned;
            lastTotalFound = totalFound;

            updateLog("📥 Nhận batch subdomain: " + (after - before) +
                    " domain mới, tổng đã nhận: " + after + "/" + lastTotalFound);

            if (allFoundDomains.size() < lastTotalFound) {
                updateLog("⏳ Đang nhận subdomain... (" + allFoundDomains.size() + "/" + lastTotalFound + ")");
                return;
            }

            StringBuilder scanInfo = new StringBuilder();
            scanInfo.append("🔍 KẾT QUẢ SCAN SUBDOMAIN\n");
            scanInfo.append("========================================\n\n");
            scanInfo.append("🎯 TARGET: ").append(targetDomain != null ? targetDomain : "(không xác định)").append("\n");
            scanInfo.append("📊 THỐNG KÊ TỔNG THỂ:\n");
            scanInfo.append("----------------------------------------\n");
            scanInfo.append("• Tổng domain đã scan: ").append(String.format("%,d", lastTotalScanned)).append("\n");
            scanInfo.append("• Domain tìm thấy: ").append(String.format("%,d", allFoundDomains.size())).append("\n");
            scanInfo.append("• Domain không tìm thấy: ").append(String.format("%,d", lastTotalScanned - allFoundDomains.size())).append("\n");
            if (lastTotalScanned > 0) {
                double successRate = (double) allFoundDomains.size() / lastTotalScanned * 100;
                scanInfo.append("• Tỷ lệ thành công: ").append(String.format("%.2f%%", successRate)).append("\n");
            }
            scanInfo.append("\n");

            if ("OK".equals(status)) {
                scanInfo.append("✅ TRẠNG THÁI: Xác thực thành công\n");
                scanInfo.append("----------------------------------------\n\n");
                if (!allFoundDomains.isEmpty()) {
                    scanInfo.append("🌐 DANH SÁCH DOMAIN TÌM THẤY (Tổng cộng: ").append(allFoundDomains.size()).append("):\n");
                    scanInfo.append("-------------------------------------\n");
                    for (int i = 0; i < allFoundDomains.size(); i++) {
                        scanInfo.append(String.format("%2d. %s\n", i + 1, allFoundDomains.get(i)));
                    }
                } else {
                    scanInfo.append("❌ Không tìm thấy subdomain nào\n");
                }
            } else {
                scanInfo.append("❌ TRẠNG THÁI: ").append(status).append("\n");
                scanInfo.append("💬 THÔNG BÁO: ").append(result).append("\n");
                scanInfo.append("----------------------------------------\n");
            }

            scanResultArea.setText(scanInfo.toString());
            scanResultArea.setCaretPosition(0);

            updateLog("📊 Kết quả scan đã được cập nhật (tổng cộng: " + allFoundDomains.size() + ")");
            if (!allFoundDomains.isEmpty()) {
                updateLog(" • Domain đầu tiên: " + allFoundDomains.get(0));
                if (allFoundDomains.size() > 1) {
                    updateLog(" • Domain cuối: " + allFoundDomains.get(allFoundDomains.size() - 1));
                }
            }
        });
    }

    public void updateServerPublicKeyInfo(String base64Key) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder info = new StringBuilder(keyInfoArea.getText());
            info.append("\n=== PUBLIC KEY SERVER NHẬN ĐƯỢC ===\n");
            if (base64Key.length() > 80) {
                info.append(base64Key.substring(0, 80)).append("...\n");
            } else {
                info.append(base64Key).append("\n");
            }
            keyInfoArea.setText(info.toString());
            keyInfoArea.setCaretPosition(keyInfoArea.getDocument().getLength());
        });
    }

    public String getPrivateKeyPath() {
        return privateKeyField.getText();
    }

    public String getRawMessage() {
        return rawMessageField.getText();
    }

    public String getPublicKeyPath() {
        return publicKeyField.getText();
    }

    public String getTargetDomain() {
        return targetDomainField.getText();
    }
}
