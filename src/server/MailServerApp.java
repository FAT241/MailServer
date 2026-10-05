package server;

import javax.swing.*;
import server.ui.ServerFrame;

/**
 * Điểm vào (Entry Point) của ứng dụng Mail Server.
 * Chạy: java -cp build server.MailServerApp
 */
public class MailServerApp {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ServerFrame::new);
    }
}
