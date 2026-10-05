package common;

import java.awt.*;

/**
 * Hằng số dùng chung cho cả Server và Client.
 * Mọi thay đổi về màu sắc, font, cổng... chỉ cần sửa ở đây.
 */
public final class AppConstants {
    private AppConstants() {}

    /* ── Network ───────────────────────────────────────── */
    public static final int    DEFAULT_PORT = 8888;
    public static final String STORAGE_DIR  = "mail_storage";

    /* ── Color Palette ─────────────────────────────────── */
    public static final Color BG_MAIN       = new Color(0xF4, 0xF6, 0xF9);
    public static final Color BG_CARD       = Color.WHITE;
    public static final Color BORDER_COLOR  = new Color(0xCB, 0xD5, 0xE1);
    public static final Color HEADER_BG     = new Color(0x1E, 0x29, 0x3B);
    public static final Color ACCENT_BLUE   = new Color(0x25, 0x63, 0xEB);
    public static final Color ACCENT_GREEN  = new Color(0x10, 0xB9, 0x81);
    public static final Color ACCENT_RED    = new Color(0xEF, 0x44, 0x44);
    public static final Color TEXT_MUTED    = new Color(0x94, 0xA3, 0xB8);
    public static final Color LOG_BG        = new Color(0x1E, 0x1E, 0x1E);
    public static final Color LOG_FG        = new Color(0x4A, 0xDE, 0x80);
    public static final Color STATUS_OK_BG  = new Color(0x05, 0x6E, 0x50);
    public static final Color STATUS_ERR_BG = new Color(0x7F, 0x1D, 0x1D);
    public static final Color GRAY_BADGE    = new Color(0x64, 0x74, 0x8B);

    /* ── Fonts ─────────────────────────────────────────── */
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_LOG    = new Font("Consolas", Font.PLAIN, 13);
    public static final Font FONT_BADGE  = new Font("Segoe UI", Font.BOLD, 14);
}
