package client.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import client.UDPClient;
import common.Protocol;
import common.UIFactory;

import static common.AppConstants.*;

/**
 * Màn hình Đăng nhập / Đăng ký.
 * Hiển thị form nhập username + password (mật khẩu hiển thị rõ, không che),
 * 2 nút bấm.
 */
public class AuthPanel extends JPanel {

    /** Callback khi đăng nhập thành công. */
    public interface LoginCallback {
        void onLoginSuccess(String username, String[] files);
    }

    private final JTextField usernameField;
    private final JTextField passwordField;   // ★ Mật khẩu hiển thị rõ (không che bằng *)
    private final UDPClient udpClient;  // ★ UDP: Dùng UDPClient
    private final StatusBar statusBar;
    private final LoginCallback callback;

    public AuthPanel(UDPClient udpClient, StatusBar statusBar, LoginCallback callback) {
        this.udpClient = udpClient;  // ★ UDP
        this.statusBar = statusBar;
        this.callback  = callback;

        setLayout(new GridBagLayout());
        setBackground(BG_MAIN);

        /* ── Card trung tâm ──────────────────────────────── */
        JPanel card = UIFactory.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 420));

        // Tiêu đề
        JLabel title = new JLabel("ĐĂNG NHẬP / ĐĂNG KÝ");
        title.setFont(FONT_TITLE);
        title.setForeground(HEADER_BG);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("Nhập tên tài khoản và mật khẩu để tiếp tục");
        subtitle.setFont(FONT_BODY);
        subtitle.setForeground(new Color(0x47, 0x55, 0x69));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        // Label + Field (Username)
        JLabel label = new JLabel("Tên tài khoản (Username):");
        label.setFont(FONT_BODY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(label);
        card.add(Box.createVerticalStrut(6));

        usernameField = createStyledField();
        card.add(usernameField);
        card.add(Box.createVerticalStrut(14));

        // Label + Field (Password) - hiển thị rõ, không mask bằng dấu *
        JLabel passLabel = new JLabel("Mật khẩu (Password):");
        passLabel.setFont(FONT_BODY);
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));

        passwordField = createStyledField();
        passwordField.setToolTipText("Mật khẩu hiển thị rõ trên màn hình (không che)");
        card.add(passwordField);
        card.add(Box.createVerticalStrut(22));

        // Nút bấm
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setOpaque(false);

        JButton loginBtn = UIFactory.createButton("\uD83D\uDD11  Đăng nhập", ACCENT_BLUE, Color.WHITE);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.setPreferredSize(new Dimension(160, 40));
        loginBtn.addActionListener(e -> doLogin());

        JButton registerBtn = UIFactory.createButton("\uD83D\uDCDD  Đăng ký mới", ACCENT_GREEN, Color.WHITE);
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerBtn.setPreferredSize(new Dimension(160, 40));
        registerBtn.addActionListener(e -> doRegister());

        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);
        card.add(btnPanel);

        add(card);
    }

    /** Tạo TextField cùng style cho username / password. */
    private JTextField createStyledField() {
        JTextField f = new JTextField(20);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        f.setMaximumSize(new Dimension(320, 38));
        f.setHorizontalAlignment(JTextField.CENTER);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        return f;
    }

    /* ── Đăng nhập ─────────────────────────────────────── */
    private void doLogin() {
        String user = usernameField.getText().trim();
        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập tên tài khoản!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String pass = passwordField.getText().trim();
        if (pass.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập mật khẩu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        statusBar.log("Đang gửi UDP packet đăng nhập...");
        try {
            String resp = udpClient.guiLenhDon(Protocol.LOGIN + "|" + user + "|" + pass);  // ★ UDP: guiLenhDon()
            String[] parts = resp.split("\\|");
            if (parts[0].equals(Protocol.SUCCESS)) {
                String[] files = new String[parts.length - 1];
                System.arraycopy(parts, 1, files, 0, files.length);
                statusBar.log("Đăng nhập thành công! Đã tải " + files.length + " thư từ máy chủ.");
                JOptionPane.showMessageDialog(this,
                    "Đăng nhập thành công! Chào mừng " + user,
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                callback.onLoginSuccess(user, files);
            } else {
                String reason = parts.length > 1 ? parts[1] : "Lỗi không xác định";
                statusBar.log("Đăng nhập thất bại: " + reason);
                JOptionPane.showMessageDialog(this, reason,
                    "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            statusBar.log("Lỗi kết nối: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Lỗi kết nối đến Server!\n" + e.getMessage(),
                "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ── Đăng ký ───────────────────────────────────────── */
    private void doRegister() {
        String user = usernameField.getText().trim();
        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập tên tài khoản!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String pass = passwordField.getText().trim();
        if (pass.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập mật khẩu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (pass.contains("|")) {
            JOptionPane.showMessageDialog(this,
                "Mật khẩu không được chứa ký tự '|'", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        statusBar.log("Đang gửi UDP packet đăng ký tài khoản '" + user + "'...");
        try {
            String resp = udpClient.guiLenhDon(Protocol.REGISTER + "|" + user + "|" + pass);  // ★ UDP: guiLenhDon()
            String[] parts = resp.split("\\|", 2);
            String msg = parts.length > 1 ? parts[1] : "";
            if (parts[0].equals(Protocol.SUCCESS)) {
                statusBar.log("Đăng ký thành công: " + msg);
                JOptionPane.showMessageDialog(this, msg.isEmpty() ? "Đăng ký thành công!" : msg,
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                statusBar.log("Đăng ký thất bại: " + msg);
                JOptionPane.showMessageDialog(this, msg.isEmpty() ? "Đăng ký thất bại!" : msg,
                    "Thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            statusBar.log("Lỗi kết nối: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Lỗi kết nối!\n" + e.getMessage(), "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }
}
