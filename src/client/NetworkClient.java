package client;

import common.Protocol;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Bọc TCP Socket, cung cấp API giao tiếp cao cấp với Server.
 * Tất cả phương thức đều synchronized để thread-safe.
 */
public class NetworkClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String host;
    private int port;

    /** Kết nối đến server. Timeout 5 giây. */
    public synchronized void connect(String host, int port) throws IOException {
        disconnect();
        this.host = host;
        this.port = port;
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setSoTimeout(10000);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
    }

    /** Ngắt kết nối. */
    public synchronized void disconnect() {
        try { if (socket != null && !socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
        socket = null; in = null; out = null;
    }

    /** Ping server để kiểm tra socket còn sống. */
    public synchronized boolean isConnected() {
        if (socket == null || socket.isClosed()) return false;
        try {
            out.println(Protocol.PING);
            if (out.checkError()) return false;
            socket.setSoTimeout(3000);
            String resp = in.readLine();
            socket.setSoTimeout(10000);
            return Protocol.PONG.equals(resp);
        } catch (Exception e) {
            return false;
        }
    }

    /** Gửi 1 dòng lệnh, nhận 1 dòng phản hồi. */
    public synchronized String sendCommand(String command) throws IOException {
        if (out == null) throw new IOException("Vui lòng kết nối đến Server trước!");
        out.println(command);
        if (out.checkError()) throw new IOException("Socket write error");
        String resp = in.readLine();
        if (resp == null) throw new IOException("Server đã đóng kết nối");
        return resp;
    }

    /** Gửi email theo giao thức SEND_MAIL. Trả về mảng [status, message]. */
    public synchronized String[] sendMail(String toUser, String senderIP,
                                          String time, String subject,
                                          String content) throws IOException {
        if (out == null) throw new IOException("Vui lòng kết nối đến Server trước!");
        out.println(Protocol.SEND_MAIL);
        out.println(toUser);
        out.println(senderIP);
        out.println(time);
        out.println(subject);
        out.println(content);
        out.println(Protocol.END_CONTENT);
        if (out.checkError()) throw new IOException("Socket write error");

        String resp = in.readLine();
        if (resp == null) throw new IOException("Server đã đóng kết nối");
        return resp.split("\\|", 2);
    }

    /** Đọc nội dung 1 file mail. Trả về danh sách dòng. */
    public synchronized List<String> readMail(String username, String filename) throws IOException {
        if (out == null) throw new IOException("Vui lòng kết nối đến Server trước!");
        out.println(Protocol.READ_MAIL + "|" + username + "|" + filename);
        if (out.checkError()) throw new IOException("Socket write error");

        String first = in.readLine();
        if (first == null) throw new IOException("Server đã đóng kết nối");
        if (first.startsWith(Protocol.FAIL)) {
            throw new IOException(first.length() > 5 ? first.substring(5) : "Lỗi đọc mail");
        }

        List<String> lines = new ArrayList<>();
        if (first.equals(Protocol.MAIL_START)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals(Protocol.MAIL_END)) break;
                lines.add(line);
            }
        }
        return lines;
    }

    /* ── Getters ───────────────────────────────────────── */
    public String getHost() { return host; }
    public int getPort()    { return port; }

    public String getLocalIP() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "Unknown IP"; }
    }
}
