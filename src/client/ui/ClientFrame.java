package client.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import client.UDPClient;
import common.UIFactory;

import static common.AppConstants.*;

/**
 * Cửa sổ chính của Mail Client.
 * Người dùng tự nhập tay Server IP và Port trong giao diện.
 */
public class ClientFrame extends JFrame {

    private static final String VIEW_AUTH      = "AUTH";
    private static final String VIEW_DASHBOARD = "DASHBOARD";

    private final UDPClient udpClient = new UDPClient();
    private final StatusBar statusBar = new StatusBar();

    /* ── Header components ─────────────────────────────── */
    private JTextField hostField;
    private JTextField portField;
    private JButton connectButton;
    private JLabel statusBadge;

    /* ── Body (CardLayout) ─────────────────────────────── */
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private DashboardPanel dashboardPanel;

    /* ── Connection check timer ────────────────────────── */
    private Timer pingTimer;

    public ClientFrame() {
        setTitle("\uD83D\uDCE7 Mail Client (UDP)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 680);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout());

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildBody(),    BorderLayout.CENTER);
        add(statusBar,      BorderLayout.SOUTH);

        startPingTimer();
        setVisible(true);
    }

    /* ═══════════════════════════════════════════════════ */
    /*                    BUILD UI                         */
    /* ═══════════════════════════════════════════════════ */

    private JPanel buildHeader() {
        JPanel header = UIFactory.createHeaderPanel();

        // Left: Title
        JLabel title = new JLabel("\uD83D\uDCE7  MAIL CLIENT (UDP)");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        // Center: Connection form nhập tay
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        connPanel.setOpaque(false);

        JLabel hostLabel = new JLabel("Server:");
        hostLabel.setFont(FONT_BODY);
        hostLabel.setForeground(Color.WHITE);
        connPanel.add(hostLabel);

        hostField = new JTextField("", 10);
        hostField.setFont(FONT_BODY);
        connPanel.add(hostField);

        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(FONT_BODY);
        portLabel.setForeground(Color.WHITE);
        connPanel.add(portLabel);

        portField = new JTextField("", 5);
        portField.setFont(FONT_BODY);
        connPanel.add(portField);

        connectButton = UIFactory.createButton("\uD83D\uDD0C Kết nối", ACCENT_GREEN, Color.WHITE);
        connectButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        connectButton.addActionListener(e -> doConnect());
        connPanel.add(connectButton);

        header.add(connPanel, BorderLayout.CENTER);

        // Right: Status badge gọn gàng
        statusBadge = UIFactory.createBadge("  \u26AA  CHƯA KẾT NỐI  ", GRAY_BADGE);
        header.add(statusBadge, BorderLayout.EAST);

        return header;
    }

    private JPanel buildBody() {
        cardPanel.setBackground(BG_MAIN);

        AuthPanel authPanel = new AuthPanel(udpClient, statusBar, this::onLoginSuccess);
        dashboardPanel = new DashboardPanel(udpClient, statusBar, this::onLogout);

        cardPanel.add(authPanel, VIEW_AUTH);
        cardPanel.add(dashboardPanel, VIEW_DASHBOARD);

        cardLayout.show(cardPanel, VIEW_AUTH);
        return cardPanel;
    }

    /* ═══════════════════════════════════════════════════ */
    /*                    ACTIONS                          */
    /* ═══════════════════════════════════════════════════ */

    private void doConnect() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập IP máy chủ!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port;
        try { port = Integer.parseInt(portField.getText().trim()); }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Port không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        statusBar.log("Đang kết nối đến Server UDP " + host + ":" + port + "...");
        connectButton.setEnabled(false);

        // Kết nối trên background thread để không block EDT
        new Thread(() -> {
            try {
                udpClient.ketNoiDenUDPServer(host, port);
                SwingUtilities.invokeLater(() -> {
                    UIFactory.updateBadge(statusBadge, "  \uD83D\uDFE2  ĐÃ KẾT NỐI  ", STATUS_OK_BG);
                    connectButton.setText("\uD83D\uDD04 Kết nối lại");
                    connectButton.setEnabled(true);
                    statusBar.log("Kết nối UDP thành công đến " + host + ":" + port + "! Sẵn sàng.");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    UIFactory.updateBadge(statusBadge, "  \uD83D\uDD34  MẤT KẾT NỐI  ", STATUS_ERR_BG);
                    connectButton.setEnabled(true);
                    statusBar.log("Kết nối thất bại: " + e.getMessage());
                    JOptionPane.showMessageDialog(ClientFrame.this,
                        "Không thể kết nối đến " + host + ":" + port + "\n" + e.getMessage(),
                        "Lỗi kết nối mạng", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void onLoginSuccess(String username, String[] files) {
        dashboardPanel.load(username, files);
        cardLayout.show(cardPanel, VIEW_DASHBOARD);
    }

    private void onLogout() {
        cardLayout.show(cardPanel, VIEW_AUTH);
        statusBar.log("Đã đăng xuất.");
    }

    /* ═══════════════════════════════════════════════════ */
    /*              CONNECTION STATUS CHECK                 */
    /* ═══════════════════════════════════════════════════ */

    private void startPingTimer() {
        pingTimer = new Timer(8000, e -> {
            new Thread(() -> {
                boolean ok = udpClient.kiemTraKetNoi();
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        UIFactory.updateBadge(statusBadge, "  \uD83D\uDFE2  ĐÃ KẾT NỐI  ", STATUS_OK_BG);
                    } else if (udpClient.getHost() != null) {
                        UIFactory.updateBadge(statusBadge, "  \uD83D\uDD34  MẤT KẾT NỐI  ", STATUS_ERR_BG);
                    }
                });
            }).start();
        });
        pingTimer.setInitialDelay(10000);
        pingTimer.start();
    }
}
