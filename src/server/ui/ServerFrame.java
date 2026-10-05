package server.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.net.InetAddress;
import server.ServerEngine;

import static common.AppConstants.*;
import common.UIFactory;

/**
 * Cửa sổ chính của Mail Server.
 * Lắp ráp: Header (IP, trạng thái) + LogPanel + nút điều khiển.
 */
public class ServerFrame extends JFrame {

    private final LogPanel logPanel;
    private JLabel statusBadge;
    private JLabel clientCountLabel;
    private JButton startButton;
    private JButton stopButton;
    private final ServerEngine engine;
    private JTextField hostField;
    private JTextField portField;

    public ServerFrame() {
        logPanel = new LogPanel();
        engine   = new ServerEngine(logPanel::log, this::onClientCountChanged);

        setTitle("\uD83D\uDCEC Mail Server - Quản lý Máy chủ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(860, 640);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        /* ── Khởi tạo badge mặc định ──────────────────── */
        // statusBadge already initialized in buildHeader
        setVisible(true);
    }

    /* ── HEADER ────────────────────────────────────────── */
    private JPanel buildHeader() {
        JPanel header = UIFactory.createHeaderPanel();

        // Left: title + IP
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("\uD83D\uDCEC  MAIL SERVER DASHBOARD");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        left.add(title);
        left.add(Box.createVerticalStrut(4));

        // Form config IP/Port
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        configPanel.setOpaque(false);

        JLabel hostLabel = new JLabel("Bind IP:");
        hostLabel.setFont(FONT_BODY);
        hostLabel.setForeground(Color.WHITE);
        configPanel.add(hostLabel);

        hostField = new JTextField("", 12);
        hostField.setFont(FONT_BODY);
        configPanel.add(hostField);

        JLabel portLabel = new JLabel("Cổng:");
        portLabel.setFont(FONT_BODY);
        portLabel.setForeground(Color.WHITE);
        configPanel.add(portLabel);

        portField = new JTextField("", 5);
        portField.setFont(FONT_BODY);
        configPanel.add(portField);

        left.add(configPanel);
        header.add(left, BorderLayout.WEST);

        // Right: status badge + client count + button
        JPanel rightInner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightInner.setOpaque(false);

        statusBadge = UIFactory.createBadge("  \u26AA  CHƯA KHỞI ĐỘNG  ", GRAY_BADGE);
        rightInner.add(statusBadge);

        clientCountLabel = new JLabel("Clients: 0");
        clientCountLabel.setFont(FONT_BADGE);
        clientCountLabel.setForeground(Color.WHITE);
        rightInner.add(clientCountLabel);

        JButton viewClientsBtn = UIFactory.createButton("Xem IP", new Color(0x3B, 0x82, 0xF6), Color.WHITE);
        viewClientsBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        viewClientsBtn.setPreferredSize(new Dimension(90, 32));
        viewClientsBtn.addActionListener(e -> showClientsDialog());
        rightInner.add(viewClientsBtn);

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(rightInner);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    /* ── CENTER ────────────────────────────────────────── */
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG_MAIN);
        center.setBorder(new EmptyBorder(12, 18, 0, 18));
        center.add(logPanel, BorderLayout.CENTER);
        return center;
    }

    /* ── FOOTER ────────────────────────────────────────── */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        footer.setBackground(BG_MAIN);
        footer.setBorder(new EmptyBorder(12, 18, 14, 18));

        startButton = UIFactory.createButton("\u25B6  KHỞI ĐỘNG SERVER", ACCENT_GREEN, Color.WHITE);
        startButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        startButton.setPreferredSize(new Dimension(240, 42));
        startButton.addActionListener(e -> doStart());

        stopButton = UIFactory.createButton("\u23F9  DỪNG SERVER", ACCENT_RED, Color.WHITE);
        stopButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        stopButton.setPreferredSize(new Dimension(240, 42));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> doStop());

        footer.add(startButton);
        footer.add(stopButton);
        return footer;
    }

    /* ── Actions ───────────────────────────────────────── */

    private void doStart() {
        String bindIp = hostField.getText().trim();
        int port;
        try { port = Integer.parseInt(portField.getText().trim()); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cổng không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            engine.start(bindIp, port);
            UIFactory.updateBadge(statusBadge,
                "  \uD83D\uDFE2  ĐANG HOẠT ĐỘNG - " + engine.getServerIP() + ":" + engine.getPort() + "  ",
                STATUS_OK_BG);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            hostField.setEnabled(false);
            portField.setEnabled(false);
        } catch (Exception e) {
            logPanel.log("[ERROR] Không thể khởi động: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Không thể khởi động Server.\n" + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doStop() {
        engine.stop();
        UIFactory.updateBadge(statusBadge, "  \uD83D\uDD34  ĐÃ DỪNG  ", STATUS_ERR_BG);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        hostField.setEnabled(true);
        portField.setEnabled(true);
    }

    private void onClientCountChanged(int count) {
        SwingUtilities.invokeLater(() -> clientCountLabel.setText("Clients: " + count));
    }

    private void showClientsDialog() {
        java.util.List<String> list = engine.getConnectedClientsInfo();
        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hiện tại không có Client nào đang kết nối.", 
                "Danh sách Client", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder("Tổng số kết nối hiện tại: " + list.size() + "\n\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append(i + 1).append(". ").append(list.get(i)).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), 
            "Danh sách Client Kết Nối", JOptionPane.INFORMATION_MESSAGE);
    }
}
