package server;

import common.Protocol;
import java.io.*;
import java.util.function.Consumer;

/**
 * Xử lý logic nghiệp vụ cho từng request UDP packet nhận được.
 * Nhận 1 String request → trả về 1 String response.
 * Dữ liệu nhiều dòng (SEND_MAIL) được gộp trong 1 packet dùng "\n" phân tách.
 */
public class UDPRequestHandler {

    private final MailStorage storage;
    private final Consumer<String> logger;
    private final String serverIP;

    public UDPRequestHandler(MailStorage storage, String serverIP, Consumer<String> logger) {
        this.storage  = storage;
        this.serverIP = serverIP;
        this.logger   = logger;
    }

    /**
     * Nhận nội dung 1 UDP packet (dạng String), phân tích lệnh,
     * xử lý và trả về response (cũng dạng String) để gửi lại.
     *
     * Dòng đầu tiên = lệnh (PING, REGISTER, LOGIN, SEND_MAIL, READ_MAIL)
     * Các dòng sau (nếu có) = dữ liệu kèm theo
     */
    public String xuLyRequest(String requestData, String clientIP, int clientPort) {
        // Tách dòng đầu (lệnh) và phần còn lại (dữ liệu)
        String[] tachDong = requestData.split("\n", 2);
        String dongLenh   = tachDong[0].trim();
        String phanDuLieu = tachDong.length > 1 ? tachDong[1] : "";

        // Phân loại lệnh và gọi hàm xử lý tương ứng
        if (dongLenh.equals(Protocol.PING)) {
            return Protocol.PONG;
        }
        if (dongLenh.startsWith(Protocol.REGISTER)) {
            return xuLyDangKy(dongLenh, clientIP);
        }
        if (dongLenh.startsWith(Protocol.LOGIN)) {
            return xuLyDangNhap(dongLenh, clientIP);
        }
        if (dongLenh.equals(Protocol.SEND_MAIL)) {
            return xuLyGuiMail(phanDuLieu, clientIP);
        }
        if (dongLenh.startsWith(Protocol.READ_MAIL)) {
            return xuLyDocMail(dongLenh, clientIP);
        }

        return Protocol.FAIL + "|Lệnh không hợp lệ";
    }

    /* ── XỬ LÝ ĐĂNG KÝ TÀI KHOẢN ────────────────────── */

    private String xuLyDangKy(String dongLenh, String clientIP) {
        String[] phan = dongLenh.split("\\|", 2);
        if (phan.length < 2 || phan[1].trim().isEmpty()) {
            return "FAIL|Tên tài khoản không hợp lệ";
        }
        String tenTaiKhoan = phan[1].trim();
        if (storage.createUser(tenTaiKhoan, serverIP)) {
            logger.accept("[ĐĂNG KÝ] Client " + clientIP
                + " tạo tài khoản: " + tenTaiKhoan + " → Thành công");
            return "SUCCESS|Đăng ký tài khoản '" + tenTaiKhoan + "' thành công!";
        } else {
            logger.accept("[ĐĂNG KÝ] Client " + clientIP
                + " tạo tài khoản: " + tenTaiKhoan + " → Thất bại (trùng tên)");
            return "FAIL|Tài khoản '" + tenTaiKhoan + "' đã tồn tại trên hệ thống";
        }
    }

    /* ── XỬ LÝ ĐĂNG NHẬP ─────────────────────────────── */

    private String xuLyDangNhap(String dongLenh, String clientIP) {
        String[] phan = dongLenh.split("\\|", 2);
        if (phan.length < 2 || phan[1].trim().isEmpty()) {
            return "FAIL|Tên tài khoản không hợp lệ";
        }
        String tenTaiKhoan = phan[1].trim();
        if (!storage.userExists(tenTaiKhoan)) {
            logger.accept("[ĐĂNG NHẬP] Client " + clientIP
                + " đăng nhập: " + tenTaiKhoan + " → Thất bại");
            return "FAIL|Tài khoản không tồn tại trên hệ thống!";
        }
        String[] danhSachFile = storage.listMails(tenTaiKhoan);
        StringBuilder sb = new StringBuilder("SUCCESS");
        for (String f : danhSachFile) sb.append("|").append(f);
        logger.accept("[ĐĂNG NHẬP] Client " + clientIP
            + " đăng nhập: " + tenTaiKhoan + " → Thành công (" + danhSachFile.length + " thư)");
        return sb.toString();
    }

    /* ── XỬ LÝ GỬI MAIL ──────────────────────────────── */

    /**
     * Dữ liệu mail được gộp trong 1 UDP packet.
     * Format (phanDuLieu - các dòng sau "SEND_MAIL"):
     *   Dòng 1: Tên người nhận
     *   Dòng 2: IP người gửi
     *   Dòng 3: Thời gian
     *   Dòng 4: Tiêu đề
     *   Dòng 5+: Nội dung thư, kết thúc bằng <<END_CONTENT>>
     */
    private String xuLyGuiMail(String phanDuLieu, String clientIP) {
        String[] cacDong = phanDuLieu.split("\n");
        if (cacDong.length < 5) {
            return "FAIL|Dữ liệu thư không hợp lệ";
        }

        String nguoiNhan = cacDong[0].trim();
        String ipGuiThu  = cacDong[1].trim();
        String thoiGian  = cacDong[2].trim();
        String tieuDe    = cacDong[3].trim();

        // Ghép nội dung thư từ dòng 5 trở đi
        StringBuilder noiDung = new StringBuilder();
        for (int i = 4; i < cacDong.length; i++) {
            if (cacDong[i].trim().equals(Protocol.END_CONTENT)) break;
            if (noiDung.length() > 0) noiDung.append("\n");
            noiDung.append(cacDong[i]);
        }

        String tenFile = storage.saveMail(nguoiNhan, ipGuiThu, thoiGian, tieuDe, noiDung.toString());
        if (tenFile != null) {
            logger.accept("[GỬI THƯ] Từ IP " + ipGuiThu
                + " gửi đến [" + nguoiNhan + "], Tiêu đề: " + tieuDe
                + " → Đã lưu: " + tenFile);
            return "SUCCESS|Gửi email đến '" + nguoiNhan + "' thành công!";
        } else {
            logger.accept("[GỬI THƯ] Từ IP " + ipGuiThu
                + " gửi đến [" + nguoiNhan + "] → Thất bại (không tồn tại)");
            return "FAIL|Người nhận '" + nguoiNhan + "' không tồn tại trên hệ thống!";
        }
    }

    /* ── XỬ LÝ ĐỌC MAIL ──────────────────────────────── */

    /**
     * Trả toàn bộ nội dung mail trong 1 response packet.
     * Format response: MAIL_START\n<nội dung>\nMAIL_END
     */
    private String xuLyDocMail(String dongLenh, String clientIP) {
        String[] phan = dongLenh.split("\\|", 3);
        if (phan.length < 3) return "FAIL|Thiếu tham số";
        String tenTaiKhoan = phan[1].trim();
        String tenFile     = phan[2].trim();
        try {
            String noiDungMail = storage.readMail(tenTaiKhoan, tenFile);
            if (noiDungMail == null) return "FAIL|File không tồn tại";

            // Gộp header + nội dung + footer vào 1 response packet
            String response = Protocol.MAIL_START + "\n" + noiDungMail + "\n" + Protocol.MAIL_END;
            logger.accept("[ĐỌC THƯ] Client đọc " + tenFile + " trong hộp thư [" + tenTaiKhoan + "]");
            return response;
        } catch (IOException e) {
            return "FAIL|Lỗi đọc file: " + e.getMessage();
        }
    }
}
