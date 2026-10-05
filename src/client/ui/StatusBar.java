package client.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static common.AppConstants.*;

/**
 * Thanh trạng thái mini-log phía dưới cửa sổ Client.
 * Hiển thị sự kiện đang xảy ra theo thời gian thực.
 */
public class StatusBar extends JPanel {

    private final JTextArea logArea;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(HEADER_BG);
        setBorder(new EmptyBorder(6, 14, 6, 14));
        setPreferredSize(new Dimension(0, 80));

        JLabel icon = new JLabel("\uD83D\uDCCB ");
        icon.setFont(FONT_BODY);
        icon.setForeground(TEXT_MUTED);
        add(icon, BorderLayout.WEST);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(HEADER_BG);
        logArea.setForeground(LOG_FG);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setRows(3);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    /** Ghi 1 dòng log kèm timestamp, thread-safe. */
    public void log(String message) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + ts + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
