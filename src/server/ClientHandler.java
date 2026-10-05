package server;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Xử lý kết nối của 1 Client trên thread riêng.
 * Đọc lệnh từ socket, dispatch đến handler tương ứng.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final MailStorage storage;
    private final Consumer<String> logger;
    private final String serverIP;
    private BufferedReader in;
    private PrintWriter out;

    public final String clientIP;
    public final int clientPort;

    public ClientHandler(Socket socket, MailStorage storage,
                         String serverIP, Consumer<String> logger) {
        this.socket   = socket;
        this.storage  = storage;
        this.serverIP = serverIP;
        this.logger   = logger;
        this.clientIP = socket.getInetAddress().getHostAddress();
        this.clientPort = socket.getPort();
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if      (line.equals(Protocol.PING))             out.println(Protocol.PONG);
                else if (line.startsWith(Protocol.REGISTER))     handleRegister(line);
                else if (line.startsWith(Protocol.LOGIN))        handleLogin(line);
                else if (line.equals(Protocol.SEND_MAIL))        handleSendMail();
                else if (line.startsWith(Protocol.READ_MAIL))    handleReadMail(line);
            }
        } catch (IOException ignored) {
            // Client đã ngắt kết nối
        } finally {
            logger.accept("[DISCONNECT] Client " + clientIP + ":" + clientPort + " đã đóng kết nối");
            close();
        }
    }

    public void close() {
        try { if (!socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }

    /* ── REGISTER ──────────────────────────────────────── */
    private void handleRegister(String line) {
        String[] p = line.split("\\|", 2);
        if (p.length < 2 || p[1].trim().isEmpty()) {
            out.println("FAIL|Tên tài khoản không hợp lệ");
            return;
        }
        String user = p[1].trim();
        if (storage.createUser(user, serverIP)) {
            out.println("SUCCESS|Đăng ký tài khoản '" + user + "' thành công!");
            logger.accept("[REGISTER] Client IP " + clientIP +
                " tạo tài khoản mới: " + user + " -> Thành công");
        } else {
            out.println("FAIL|Tài khoản '" + user + "' đã tồn tại trên hệ thống");
            logger.accept("[REGISTER] Client IP " + clientIP +
                " tạo tài khoản mới: " + user + " -> Thất bại (trùng tên)");
        }
    }

    /* ── LOGIN ─────────────────────────────────────────── */
    private void handleLogin(String line) {
        String[] p = line.split("\\|", 2);
        if (p.length < 2 || p[1].trim().isEmpty()) {
            out.println("FAIL|Tên tài khoản không hợp lệ");
            return;
        }
        String user = p[1].trim();
        if (!storage.userExists(user)) {
            out.println("FAIL|Tài khoản không tồn tại trên hệ thống!");
            logger.accept("[LOGIN] Client IP " + clientIP +
                " đăng nhập tài khoản: " + user + " -> Thất bại");
            return;
        }
        String[] files = storage.listMails(user);
        StringBuilder sb = new StringBuilder("SUCCESS");
        for (String f : files) sb.append("|").append(f);
        out.println(sb.toString());
        logger.accept("[LOGIN] Client IP " + clientIP +
            " đăng nhập tài khoản: " + user + " -> Thành công (" + files.length + " thư)");
    }

    /* ── SEND_MAIL ─────────────────────────────────────── */
    private void handleSendMail() throws IOException {
        String toUser   = in.readLine();
        String senderIP = in.readLine();
        String time     = in.readLine();
        String subject  = in.readLine();

        StringBuilder content = new StringBuilder();
        String cl;
        while ((cl = in.readLine()) != null) {
            if (cl.equals(Protocol.END_CONTENT)) break;
            if (content.length() > 0) content.append("\n");
            content.append(cl);
        }

        if (toUser == null || senderIP == null || time == null || subject == null) {
            out.println("FAIL|Dữ liệu thư không hợp lệ");
            return;
        }
        toUser = toUser.trim();
        subject = subject.trim();
        senderIP = senderIP.trim();

        String filename = storage.saveMail(toUser, senderIP, time.trim(), subject, content.toString());
        if (filename != null) {
            out.println("SUCCESS|Gửi email đến '" + toUser + "' thành công!");
            logger.accept("[SEND_MAIL] Thư mới từ IP " + senderIP +
                " gửi đến [" + toUser + "], Tiêu đề: " + subject +
                " -> Đã lưu file: " + filename);
        } else {
            out.println("FAIL|Người nhận '" + toUser + "' không tồn tại trên hệ thống!");
            logger.accept("[SEND_MAIL] Thư mới từ IP " + senderIP +
                " gửi đến [" + toUser + "] -> Thất bại (người nhận không tồn tại)");
        }
    }

    /* ── READ_MAIL ─────────────────────────────────────── */
    private void handleReadMail(String line) {
        String[] p = line.split("\\|", 3);
        if (p.length < 3) { out.println("FAIL|Thiếu tham số"); return; }
        String user = p[1].trim();
        String file = p[2].trim();
        try {
            String mailContent = storage.readMail(user, file);
            if (mailContent == null) { out.println("FAIL|File không tồn tại"); return; }
            out.println(Protocol.MAIL_START);
            out.println(mailContent);
            out.println(Protocol.MAIL_END);
            logger.accept("[READ_MAIL] Client đọc file " + file + " trong hộp thư [" + user + "]");
        } catch (IOException e) {
            out.println("FAIL|Lỗi đọc file: " + e.getMessage());
        }
    }
}
