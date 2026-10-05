package common;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import static common.AppConstants.*;

/**
 * Factory tạo các UI component đã styled sẵn.
 * Dùng chung cho cả Server GUI và Client GUI.
 */
public final class UIFactory {
    private UIFactory() {}

    /** Tạo nút bấm phẳng có hiệu ứng hover. */
    public static JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(bg.darker());
            }
            @Override public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(bg);
            }
        });
        return btn;
    }

    /** Tạo panel dạng card trắng có viền nhẹ. */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));
        return card;
    }

    /** Tạo badge hiển thị trạng thái. */
    public static JLabel createBadge(String text, Color bg) {
        JLabel badge = new JLabel(text);
        badge.setFont(FONT_BADGE);
        badge.setForeground(Color.WHITE);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bg.darker(), 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
        return badge;
    }

    /** Cập nhật nội dung và màu cho badge. */
    public static void updateBadge(JLabel badge, String text, Color bg) {
        badge.setText(text);
        badge.setBackground(bg);
        badge.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bg.darker(), 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
    }

    /** Tạo header panel tối cho phần đầu cửa sổ. */
    public static JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(12, 18, 12, 18));
        return header;
    }
}
