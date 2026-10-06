package server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static common.AppConstants.STORAGE_DIR;

/**
 * Quản lý I/O file cho hệ thống mail.
 * Tạo user, lưu mail, đọc mail, liệt kê thư.
 * Thông tin tài khoản (tên, mật khẩu, thời gian tạo) lưu trong mail_storage/accounts.txt
 */
public class MailStorage {

    /** File lưu danh sách tài khoản: username|mật khẩu|thời gian tạo */
    private static final String ACCOUNT_FILE = "accounts.txt";
    private static final String TIME_FORMAT  = "yyyy-MM-dd HH:mm:ss";

    /** Bộ nhớ phụ của file tài khoản, nạp 1 lần khi khởi động. */
    private final Map<String, TaiKhoan> accounts = new LinkedHashMap<>();

    public MailStorage() {
        new File(STORAGE_DIR).mkdirs();
        docFileAccounts();
    }

    /** Kiểm tra user đã tồn tại chưa. */
    public boolean userExists(String username) {
        if (!isValidName(username)) return false;
        return new File(STORAGE_DIR, username).isDirectory();
    }

    /**
     * Tạo tài khoản mới + ghi vào file tài khoản + file chào mừng.
     * @return Thời gian tạo tài khoản (đã ghi vào file), hoặc null nếu thất bại.
     */
    public String createUser(String username, String password, String serverIP) {
        if (!isValidName(username) || !isValidPassword(password)) return null;

        File dir = new File(STORAGE_DIR, username);
        TaiKhoan acc = accounts.get(username);
        // Đã có bản ghi và đã đặt mật khẩu → không cho tạo trùng
        if (acc != null && !acc.matKhau.isEmpty()) return null;

        boolean moi = !dir.exists();
        if (moi && !dir.mkdirs()) return null;

        // Tài khoản cũ (tạo trước khi có mật khẩu) → chỉ cập nhật mật khẩu,
        // giữ nguyên thời gian tạo ban đầu.
        if (acc == null) {
            Date ngayTao = new Date(moi ? System.currentTimeMillis() : dir.lastModified());
            acc = new TaiKhoan("", new SimpleDateFormat(TIME_FORMAT).format(ngayTao));
            accounts.put(username, acc);
        }

        acc.matKhau = password;
        luuFileAccounts();

        if (moi) createWelcomeEmail(dir, serverIP);
        return acc.taoLuc;
    }

    /**
     * Kiểm tra mật khẩu đăng nhập.
     * @return null nếu đúng mật khẩu, ngược lại trả về lý do thất bại.
     */
    public String kiemTraMatKhau(String username, String password) {
        if (password == null || password.isEmpty()) return "Thiếu mật khẩu";
        TaiKhoan acc = accounts.get(username);
        if (acc == null) return "Tài khoản chưa có trong file tài khoản (accounts.txt)";
        if (acc.matKhau.isEmpty()) {
            return "Tài khoản chưa đặt mật khẩu - hãy bấm Đăng ký để đặt mật khẩu";
        }
        if (!acc.matKhau.equals(password)) return "Mật khẩu không đúng!";
        return null;
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

    /** Thông tin 1 tài khoản trong file accounts.txt. */
    private static final class TaiKhoan {
        String matKhau;   // rỗng = tài khoản cũ chưa đặt mật khẩu
        String taoLuc;    // thời gian tạo tài khoản
        TaiKhoan(String matKhau, String taoLuc) {
            this.matKhau = matKhau;
            this.taoLuc  = taoLuc;
        }
    }

    private boolean isValidPassword(String password) {
        return password != null && !password.isEmpty()
            && !password.contains("|") && !password.contains("\n");
    }

    /** Nạp file tài khoản + tạo bản ghi cho thư mục user cũ chưa có mật khẩu. */
    private void docFileAccounts() {
        File file = new File(STORAGE_DIR, ACCOUNT_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] phan = line.split("\\|", 3);
                    if (phan.length == 3) {
                        accounts.put(phan[0], new TaiKhoan(phan[1], phan[2]));
                    } else if (phan.length == 2) {
                        accounts.put(phan[0], new TaiKhoan(phan[1], ""));
                    }
                }
            } catch (IOException e) {
                System.err.println("[MailStorage] Lỗi đọc file tài khoản: " + e.getMessage());
            }
        }

        // Thư mục user tồn tại nhưng chưa có bản ghi → bổ sung (mật khẩu trống)
        boolean coDoi = false;
        File[] cacThuMuc = new File(STORAGE_DIR).listFiles(File::isDirectory);
        if (cacThuMuc != null) {
            for (File d : cacThuMuc) {
                if (!accounts.containsKey(d.getName())) {
                    String taoLuc = new SimpleDateFormat(TIME_FORMAT).format(new Date(d.lastModified()));
                    accounts.put(d.getName(), new TaiKhoan("", taoLuc));
                    coDoi = true;
                }
            }
        }
        if (coDoi) luuFileAccounts();
    }

    /** Ghi/ghi lại toàn bộ file tài khoản. */
    private void luuFileAccounts() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(new File(STORAGE_DIR, ACCOUNT_FILE)), "UTF-8"))) {
            for (Map.Entry<String, TaiKhoan> e : accounts.entrySet()) {
                pw.println(e.getKey() + "|" + e.getValue().matKhau + "|" + e.getValue().taoLuc);
            }
        } catch (IOException e) {
            System.err.println("[MailStorage] Lỗi ghi file tài khoản: " + e.getMessage());
        }
    }

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
