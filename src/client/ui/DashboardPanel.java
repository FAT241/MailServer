package client.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import client.NetworkClient;
import common.Protocol;
import common.UIFactory;

import static common.AppConstants.*;

/**
 * Dashboard sau đăng nhập: Hộp thư (trái) + Soạn thư (phải).
 */
public class DashboardPanel extends JPanel {

    /** Callback khi đăng xuất. */
    public interface LogoutCallback { void onLogout(); }

    private final NetworkClient network;
    private final StatusBar statusBar;
    private final LogoutCallback logoutCallback;
    private String username;

    /* ── Mailbox components ─────────────────────────────── */
    private final DefaultListModel<String> mailListModel = new DefaultListModel<>();
    private final JList<String> mailList = new JList<>(mailListModel);
    private final JTextArea mailContentArea = new JTextArea();

    /* ── Compose components ────────────────────────────── */
    private final JTextField toField      = new JTextField();
    private final JTextField subjectField = new JTextField();
    private final JTextArea  contentArea  = new JTextArea();
    private JButton sendButton;

    /* ── Header ────────────────────────────────────────── */
    private JLabel headerLabel;

    public DashboardPanel(NetworkClient network, StatusBar statusBar, LogoutCallback logoutCallback) {
        this.network        = network;
        this.statusBar      = statusBar;
        this.logoutCallback = logoutCallback;

        setLayout(new BorderLayout());
        setBackground(BG_MAIN);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
    }

    /** Nạp dữ liệu dashboard khi đăng nhập thành công. */
    public void load(String username, String[] files) {
        this.username = username;
        headerLabel.setText("   Tài khoản: " + username
            + "   |   Máy chủ: " + network.getHost()
            + "   |   Trạng thái: Đã kết nối");
        mailListModel.clear();
        for (String f : files) mailListModel.addElement(f);
        mailContentArea.setText("");
        toField.setText("");
        subjectField.setText("");
        contentArea.setText("");
    }

    /* ═══════════════════════════════════════════════════ */
    /*                    BUILD UI                         */
    /* ═══════════════════════════════════════════════════ */

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x0F, 0x17, 0x2A));
        header.setBorder(new EmptyBorder(10, 14, 10, 14));

        headerLabel = new JLabel("   Tài khoản: ---");
        headerLabel.setFont(FONT_BODY);
        headerLabel.setForeground(Color.WHITE);
        header.add(headerLabel, BorderLayout.CENTER);

        JButton logoutBtn = UIFactory.createButton("Đăng xuất", ACCENT_RED, Color.WHITE);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.addActionListener(e -> logoutCallback.onLogout());
        header.add(logoutBtn, BorderLayout.EAST);

        return header;
    }

    private JSplitPane buildBody() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildMailbox(), buildCompose());
        split.setResizeWeight(0.45);
        split.setDividerSize(6);
        split.setBorder(null);
        return split;
    }

    /* ── LEFT: Hộp thư ─────────────────────────────────── */
    private JPanel buildMailbox() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(10, 10, 10, 5));

        // Title + Refresh
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("\uD83D\uDCE5  HỘP THƯ ĐẾN");
        title.setFont(FONT_HEADER);
        top.add(title, BorderLayout.WEST);

        JButton refreshBtn = UIFactory.createButton("\uD83D\uDD04 Làm mới", ACCENT_BLUE, Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshBtn.addActionListener(e -> doRefresh());
        top.add(refreshBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // Mail list
        mailList.setFont(FONT_BODY);
        mailList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mailList.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        mailList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) doReadMail();
        });
        JScrollPane listScroll = new JScrollPane(mailList);
        listScroll.setPreferredSize(new Dimension(0, 180));

        // Mail content viewer
        mailContentArea.setEditable(false);
        mailContentArea.setFont(FONT_BODY);
        mailContentArea.setLineWrap(true);
        mailContentArea.setWrapStyleWord(true);
        mailContentArea.setBackground(new Color(0xFA, 0xFA, 0xFA));
        mailContentArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));
        JScrollPane contentScroll = new JScrollPane(mailContentArea);

        JSplitPane vertSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScroll, contentScroll);
        vertSplit.setResizeWeight(0.35);
        vertSplit.setDividerSize(5);
        vertSplit.setBorder(null);
        panel.add(vertSplit, BorderLayout.CENTER);

        return panel;
    }

    /* ── RIGHT: Soạn thư ───────────────────────────────── */
    private JTextField senderField;

    private JPanel buildCompose() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(10, 5, 10, 10));

        JLabel title = new JLabel("\u270D  SOẠN THƯ MỚI");
        title.setFont(FONT_HEADER);
        panel.add(title, BorderLayout.NORTH);

        // Form
        JPanel form = UIFactory.createCardPanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Sender IP
        addLabel(form, gbc, row, "IP người gửi (Của bạn):");
        senderField = new JTextField(network.getLocalIP());
        senderField.setFont(FONT_BODY);
        addField(form, gbc, row++, senderField);

        // Time (auto)
        addLabel(form, gbc, row, "Thời gian:");
        JTextField timeField = new JTextField("Tự động ghi nhận lúc gửi");
        timeField.setEditable(false);
        timeField.setFont(FONT_BODY);
        timeField.setBackground(new Color(0xF1, 0xF5, 0xF9));
        addField(form, gbc, row++, timeField);

        // To
        addLabel(form, gbc, row, "Người nhận:");
        toField.setFont(FONT_BODY);
        addField(form, gbc, row++, toField);

        // Subject
        addLabel(form, gbc, row, "Tiêu đề:");
        subjectField.setFont(FONT_BODY);
        addField(form, gbc, row++, subjectField);

        // Content
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        contentArea.setFont(FONT_BODY);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(6, 8, 6, 8)
        ));
        JScrollPane contentScroll = new JScrollPane(contentArea);
        form.add(contentScroll, gbc);

        panel.add(form, BorderLayout.CENTER);

        // Send button
        sendButton = UIFactory.createButton("\uD83D\uDCE8  GỬI EMAIL", ACCENT_GREEN, Color.WHITE);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setPreferredSize(new Dimension(0, 42));
        sendButton.addActionListener(e -> doSendMail());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(6, 0, 0, 0));
        bottom.add(sendButton, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    /* ═══════════════════════════════════════════════════ */
    /*                    ACTIONS                          */
    /* ═══════════════════════════════════════════════════ */

    private void doRefresh() {
        statusBar.log("Đang làm mới hộp thư...");
        try {
            String resp = network.sendCommand(Protocol.LOGIN + "|" + username);
            String[] parts = resp.split("\\|");
            if (parts[0].equals(Protocol.SUCCESS)) {
                mailListModel.clear();
                for (int i = 1; i < parts.length; i++) mailListModel.addElement(parts[i]);
                statusBar.log("Đã tải " + (parts.length - 1) + " thư từ máy chủ.");
            }
        } catch (Exception e) {
            statusBar.log("Lỗi làm mới: " + e.getMessage());
        }
    }

    private void doReadMail() {
        String selected = mailList.getSelectedValue();
        if (selected == null || username == null) return;
        statusBar.log("Đang đọc thư: " + selected);
        try {
            List<String> lines = network.readMail(username, selected);
            mailContentArea.setText(String.join("\n", lines));
            mailContentArea.setCaretPosition(0);
            statusBar.log("Đã mở thư: " + selected);
        } catch (Exception e) {
            mailContentArea.setText("Lỗi đọc thư: " + e.getMessage());
            statusBar.log("Lỗi đọc thư: " + e.getMessage());
        }
    }

    private void doSendMail() {
        String to      = toField.getText().trim();
        String sender  = senderField.getText().trim();
        String subject = subjectField.getText().trim();
        String content = contentArea.getText().trim();

        if (to.isEmpty() || sender.isEmpty() || subject.isEmpty() || content.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng điền đầy đủ Người nhận, IP, Tiêu đề và Nội dung!",
                "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        statusBar.log("Đang gửi email đến " + to + "...");

        try {
            String[] result = network.sendMail(to, sender, time, subject, content);
            if (result[0].equals(Protocol.SUCCESS)) {
                String msg = result.length > 1 ? result[1] : "Gửi thành công!";
                statusBar.log("Đã gửi email thành công đến user: " + to);
                JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                toField.setText("");
                subjectField.setText("");
                contentArea.setText("");
            } else {
                String msg = result.length > 1 ? result[1] : "Gửi thất bại!";
                statusBar.log("Gửi thất bại: " + msg);
                JOptionPane.showMessageDialog(this, msg, "Thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            statusBar.log("Lỗi gửi mail: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Lỗi kết nối!\n" + e.getMessage(), "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ── Grid helpers ──────────────────────────────────── */
    private void addLabel(JPanel p, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        p.add(l, gbc);
    }

    private void addField(JPanel p, GridBagConstraints gbc, int row, JTextField field) {
        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 1;
        gbc.weightx = 1; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, gbc);
    }
}
