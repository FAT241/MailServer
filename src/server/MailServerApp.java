package server;

import javax.swing.*;
import server.ui.ServerFrame;

/**
 * ═══════════════════════════════════════════════════════════
 *   ĐIỂM VÀO (Entry Point) CỦA ỨNG DỤNG MAIL SERVER (UDP)
 * ═══════════════════════════════════════════════════════════
 *
 * Cách build và chạy:
 *   Bước 1 - Compile:
 *     javac -encoding UTF-8 -d build -sourcepath src src/server/MailServerApp.java src/client/MailClientApp.java
 *
 *   Bước 2 - Chạy Server:
 *     java -cp build server.MailServerApp
 *
 *   Bước 3 - Chạy Client (mở terminal khác):
 *     java -cp build client.MailClientApp
 */
public class MailServerApp {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ServerFrame::new);
    }
}
