package server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static common.AppConstants.STORAGE_DIR;

/**
 * Quản lý I/O file cho hệ thống mail.
 * Tạo user, lưu mail, đọc mail, liệt kê thư.
 */
public class MailStorage {

    public MailStorage() {
        new File(STORAGE_DIR).mkdirs();
    }

    /** Kiểm tra user đã tồn tại chưa. */
    public boolean userExists(String username) {
        if (!isValidName(username)) return false;
        return new File(STORAGE_DIR, username).isDirectory();
    }

    /** Tạo tài khoản mới + file chào mừng. Trả về false nếu đã tồn tại. */
    public boolean createUser(String username, String serverIP) {
        if (!isValidName(username)) return false;
        File dir = new File(STORAGE_DIR, username);
        if (dir.exists()) return false;
        dir.mkdirs();
        createWelcomeEmail(dir, serverIP);
        return true;
    }

    /** Liệt kê tất cả file mail trong hộp thư của user. */
    public String[] listMails(String username) {
        if (!isValidName(username)) return new String[0];
        File dir = new File(STORAGE_DIR, username);
        String[] files = dir.list();
        if (files != null) Arrays.sort(files);
        return files != null ? files : new String[0];
    }

    /**
     * Lưu email vào thư mục người nhận.
     * @return Tên file đã lưu, hoặc null nếu thất bại.
     */
    public String saveMail(String toUser, String senderIP,
                           String time, String subject, String content) {
        if (!isValidName(toUser)) return null;
        File dir = new File(STORAGE_DIR, toUser);
        if (!dir.isDirectory()) return null;

        String safeSubject = subject.replaceAll(
            "[^a-zA-Z0-9_\\-\\u00C0-\\u024F\\u1E00-\\u1EFF]", "_");
        if (safeSubject.length() > 40) safeSubject = safeSubject.substring(0, 40);

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "mail_" + safeSubject + "_" + ts + ".txt";

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(new File(dir, filename)), "UTF-8"))) {
            pw.println("Sender IP: " + senderIP);
            pw.println("Time: " + time);
            pw.println("Subject: " + subject);
            pw.println("Content:");
            pw.println(content);
        } catch (IOException e) {
            return null;
        }
        return filename;
    }

    /** Đọc nội dung file mail. Trả null nếu file không tồn tại. */
    public String readMail(String username, String filename) throws IOException {
        if (!isValidName(username) || !isValidName(filename)) return null;
        File file = new File(STORAGE_DIR + File.separator + username, filename);
        if (!file.exists()) return null;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /* ── Private ───────────────────────────────────────── */

    private boolean isValidName(String name) {
        if (name == null || name.isEmpty() || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return false;
        }
        return true;
    }

    private void createWelcomeEmail(File userDir, String serverIP) {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(new File(userDir, "new_email.txt")), "UTF-8"))) {
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            pw.println("Sender IP: " + serverIP);
            pw.println("Time: " + now);
            pw.println("Subject: Chào mừng bạn đến với Mail System!");
            pw.println("Content:");
            pw.println("Thank you for using this service. We hope that you will feel comfortable using our mail system.");
            pw.println("Hãy bắt đầu gửi và nhận email ngay bây giờ!");
        } catch (IOException e) {
            System.err.println("[MailStorage] Lỗi tạo welcome email: " + e.getMessage());
        }
    }
}
