package server.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import server.UDPServer;

import static common.AppConstants.*;
import common.UIFactory;

/**
 * Cửa sổ chính của Mail Server.
 * Người dùng tự nhập tay Bind IP và Port trong giao diện.
 */
public class ServerFrame extends JFrame {

    private final LogPanel logPanel;
    private JLabel statusBadge;
    private JButton startButton;
    private JButton stopButton;
    private final UDPServer udpServer;
    private JTextField hostField;
    private JTextField portField;

    public ServerFrame() {
        logPanel  = new LogPanel();
        udpServer = new UDPServer(logPanel::log);

        setTitle("\uD83D\uDCEC Mail Server - Quản lý Máy chủ (UDP)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(880, 640);
        setMinimumSize(new Dimension(750, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        setVisible(true);
    }

    /* ── HEADER ────────────────────────────────────────── */
    private JPanel buildHeader() {
        JPanel header = UIFactory.createHeaderPanel();

        // Left Panel: Title (trên) + Bind IP/Port Config nhập tay (dưới)
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("\uD83D\uDCEC  MAIL SERVER DASHBOARD (UDP)");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        left.add(title);
        left.add(Box.createVerticalStrut(6));

        // Form config IP/Port nhập tay
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        configPanel.setOpaque(false);

        JLabel hostLabel = new JLabel("Bind IP:");
        hostLabel.setFont(FONT_BODY);
        hostLabel.setForeground(Color.WHITE);
        configPanel.add(hostLabel);

        hostField = new JTextField("", 10);
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

        // Right Panel: Status Badge (trên) + Client Count & Button (dưới)
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrapper.setOpaque(false);
        statusBadge = UIFactory.createBadge("  \u26AA  CHƯA KHỞI ĐỘNG  ", GRAY_BADGE);
        badgeWrapper.add(statusBadge);
        right.add(badgeWrapper);

        right.add(Box.createVerticalStrut(6));

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
        if (bindIp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Bind IP (ví dụ: 127.0.0.1 hoặc 0.0.0.0)!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port;
        try { port = Integer.parseInt(portField.getText().trim()); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập cổng hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            udpServer.startUDPServer(bindIp, port);
            UIFactory.updateBadge(statusBadge, "  \uD83D\uDFE2  ĐANG HOẠT ĐỘNG  ", STATUS_OK_BG);
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
        udpServer.stopUDPServer();
        UIFactory.updateBadge(statusBadge, "  \uD83D\uDD34  ĐÃ DỪNG  ", STATUS_ERR_BG);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        hostField.setEnabled(true);
        portField.setEnabled(true);
    }

}
