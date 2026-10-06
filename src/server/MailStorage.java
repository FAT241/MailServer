package server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static common.AppConstants.STORAGE_DIR;

/**
 * Quản lý I/O file cho hệ thống mail.
 * Tạo user, lưu mail, đọc mail, liệt kê thư.
 * Thông tin tài khoản (username, mật khẩu, thời gian tạo) lưu trong
 * file RIÊNG account.txt nằm trong thư mục của từng user.
 */
public class MailStorage {

    /** File thông tin tài khoản riêng của mỗi user (nằm trong folder user). */
    private static final String ACCOUNT_FILE = "account.txt";
    /** File tài khoản cũ (một thời gian) - chỉ dùng để migrate sang folder. */
    private static final String LEGACY_ACCOUNT_FILE = "accounts.txt";
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
     * Tạo tài khoản mới: tạo thư mục + file account.txt chứa 3 trường
     * (username, password, thời gian tạo). KHÔNG tạo mail chào mừng.
     * @return Thời gian tạo tài khoản, hoặc null nếu thất bại/trùng.
     */
    public String createUser(String username, String password, String serverIP) {
        if (!isValidName(username) || !isValidPassword(password)) return null;

        File dir = new File(STORAGE_DIR, username);
        if (new File(dir, ACCOUNT_FILE).exists()) return null; // đã có tài khoản

        boolean moi = !dir.exists();
        if (moi && !dir.mkdirs()) return null;

        String taoLuc = new SimpleDateFormat(TIME_FORMAT).format(
            new Date(moi ? System.currentTimeMillis() : dir.lastModified()));
        TaiKhoan acc = new TaiKhoan(password, taoLuc);
        accounts.put(username, acc);
        return ghiAccountFile(username, acc) ? taoLuc : null;
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

    /** Liệt kê tất cả file mail trong hộp thư của user (ẩn file account.txt). */
    public String[] listMails(String username) {
        if (!isValidName(username)) return new String[0];
        File dir = new File(STORAGE_DIR, username);
        String[] files = dir.list((d, name) -> !name.equals(ACCOUNT_FILE));
        if (files != null) Arrays.sort(files);
        return files != null ? files : new String[0];
    }

    /**
     * Lưu email vào thư mục người nhận.
     * File mail chứa: tên người gửi, IP người gửi, thời gian, tiêu đề, nội dung.
     * @return Tên file đã lưu, hoặc null nếu thất bại.
     */
    public String saveMail(String tenNguoiGui, String toUser, String senderIP,
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
            pw.println("Sender: " + tenNguoiGui);
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

    /** Đọc nội dung file mail. Trả null nếu file không tồn tại hoặc là file tài khoản. */
    public String readMail(String username, String filename) throws IOException {
        if (!isValidName(username) || !isValidName(filename)) return null;
        if (filename.equals(ACCOUNT_FILE)) return null; // không lộ mật khẩu
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

    private boolean isValidName(String name) {
        if (name == null || name.isEmpty() || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return false;
        }
        return true;
    }

    /**
     * Nạp tài khoản từ account.txt trong từng folder user.
     * Folder chưa có account.txt → migrate từ file accounts.txt cũ (nếu có),
     * không mất dữ liệu tài khoản cũ.
     */
    private void docFileAccounts() {
        // 1. Nạp file cũ (nếu có) làm nguồn migrate
        Map<String, TaiKhoan> cu = new LinkedHashMap<>();
        File legacyFile = new File(STORAGE_DIR, LEGACY_ACCOUNT_FILE);
        if (legacyFile.exists()) {
            try {
                for (String line : docUTF8(legacyFile).split("\r?\n")) {
                    if (line.trim().isEmpty()) continue;
                    String[] phan = line.split("\\|", 3);
                    if (phan.length == 3) {
                        cu.put(phan[0], new TaiKhoan(phan[1], phan[2]));
                    } else if (phan.length == 2) {
                        cu.put(phan[0], new TaiKhoan(phan[1], ""));
                    }
                }
            } catch (IOException e) {
                System.err.println("[MailStorage] Lỗi đọc file tài khoản cũ: " + e.getMessage());
            }
        }

        // 2. Mọi user từ file cũ phải có folder + account.txt (kể cả chưa từng có folder)
        for (Map.Entry<String, TaiKhoan> e : cu.entrySet()) {
            accounts.put(e.getKey(), e.getValue());
            File accFile = new File(new File(STORAGE_DIR, e.getKey()), ACCOUNT_FILE);
            if (!accFile.exists()) ghiAccountFile(e.getKey(), e.getValue());
        }

        // 3. Quét từng folder user, đọc account.txt riêng (nguồn chính khi đã migrate)
        File[] cacThuMuc = new File(STORAGE_DIR).listFiles(File::isDirectory);
        if (cacThuMuc == null) return;
        for (File d : cacThuMuc) {
            String ten = d.getName();
            File accFile = new File(d, ACCOUNT_FILE);
            TaiKhoan tk;

            if (accFile.exists()) {
                tk = docAccountFile(accFile);
                if (tk == null) continue;
            } else if (cu.containsKey(ten)) {
                // Migrate từ file cũ sang account.txt trong folder
                tk = cu.get(ten);
                ghiAccountFile(ten, tk);
            } else {
                // Folder chưa có thông tin → tạo với mật khẩu trống
                tk = new TaiKhoan("", new SimpleDateFormat(TIME_FORMAT).format(new Date(d.lastModified())));
                ghiAccountFile(ten, tk);
            }
            accounts.put(ten, tk);
        }
    }

    /** Đọc toàn bộ file UTF-8, bỏ BOM (nếu có) ở đầu. */
    private String docUTF8(File f) throws IOException {
        String s = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
        return s;
    }

    /** Đọc 1 file account.txt (username=, password=, created=). Trả null nếu hỏng. */
    private TaiKhoan docAccountFile(File accFile) {
        try {
            String all = docUTF8(accFile);
            String matKhau = null, taoLuc = null;
            for (String line : all.split("\r?\n")) {
                if (line.startsWith("password=")) matKhau = line.substring(9);
                else if (line.startsWith("created=")) taoLuc = line.substring(8);
            }
            if (matKhau == null) {
                // Lỗi cũ: file ghi kiểu user|pass|time → đọc thêm fallback
                String[] phan = all.trim().split("\\|", 3);
                if (phan.length == 3) return new TaiKhoan(phan[1], phan[2]);
                if (phan.length == 2) return new TaiKhoan(phan[1], "");
                return null;
            }
            return new TaiKhoan(matKhau, taoLuc == null ? "" : taoLuc);
        } catch (IOException e) {
            System.err.println("[MailStorage] Lỗi đọc account.txt: " + e.getMessage());
            return null;
        }
    }

    /** Ghi account.txt (3 trường) vào thư mục của user. */
    private boolean ghiAccountFile(String username, TaiKhoan tk) {
        File dir = new File(STORAGE_DIR, username);
        if (!dir.exists() && !dir.mkdirs()) return false;
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(new File(dir, ACCOUNT_FILE)), "UTF-8"))) {
            pw.println("username=" + username);
            pw.println("password=" + tk.matKhau);
            pw.println("created=" + tk.taoLuc);
            return true;
        } catch (IOException e) {
            System.err.println("[MailStorage] Lỗi ghi account.txt: " + e.getMessage());
            return false;
        }
    }
}
