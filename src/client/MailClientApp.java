package client;

import javax.swing.*;
import client.ui.ClientFrame;

/**
 * ═══════════════════════════════════════════════════════════
 *   ĐIỂM VÀO (Entry Point) CỦA ỨNG DỤNG MAIL CLIENT (UDP)
 * ═══════════════════════════════════════════════════════════
 *
 * Cách build và chạy:
 *   Bước 1 - Compile:
 *     javac -encoding UTF-8 -d build -sourcepath src src/server/MailServerApp.java src/client/MailClientApp.java
 *
 *   Bước 2 - Chạy Server trước:
 *     java -cp build server.MailServerApp
 *
 *   Bước 3 - Chạy Client (mở terminal khác):
 *     java -cp build client.MailClientApp
 */
public class MailClientApp {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ClientFrame::new);
    }
}
