package client;

import javax.swing.*;
import client.ui.ClientFrame;

/**
 * Điểm vào (Entry Point) của ứng dụng Mail Client.
 * Chạy: java -cp build client.MailClientApp
 */
public class MailClientApp {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ClientFrame::new);
    }
}
