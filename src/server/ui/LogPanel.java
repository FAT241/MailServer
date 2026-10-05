package server.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static common.AppConstants.*;
import common.UIFactory;

/**
 * Panel console log nền đen, chữ xanh lá.
 * Hiển thị nhật ký hoạt động real-time của Server.
 */
public class LogPanel extends JPanel {

    private final JTextArea textArea;

    public LogPanel() {
        setLayout(new BorderLayout());
        setBackground(LOG_BG);
        setBorder(new LineBorder(BORDER_COLOR, 1, true));

        /* ── Text Area ───────────────────────────────────── */
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(LOG_BG);
        textArea.setForeground(LOG_FG);
        textArea.setFont(FONT_LOG);
        textArea.setCaretColor(LOG_FG);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(10, 14, 10, 14));

        /* ── Header bar ──────────────────────────────────── */
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x2D, 0x2D, 0x2D));
        header.setBorder(new EmptyBorder(8, 14, 8, 14));

        JLabel title = new JLabel("\uD83D\uDCCB  NHẬT KÝ HOẠT ĐỘNG (LIVE LOG)");
        title.setFont(FONT_HEADER);
        title.setForeground(LOG_FG);
        header.add(title, BorderLayout.WEST);

        JButton clearBtn = UIFactory.createButton("Xoá Log", GRAY_BADGE, Color.WHITE);
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearBtn.addActionListener(e -> textArea.setText(""));
        header.add(clearBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.CENTER);
    }

    /** Ghi một dòng log kèm timestamp, thread-safe. */
    public void log(String message) {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String line = "[" + ts + "] " + message + "\n";
        SwingUtilities.invokeLater(() -> {
            textArea.append(line);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}
