package client;

import common.Protocol;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp giao tiếp mạng phía Client sử dụng giao thức UDP.
 * Dùng DatagramSocket để gửi/nhận DatagramPacket với Server.
 * UDP là connectionless - mỗi request/response là 1 packet riêng biệt.
 */
public class UDPClient {

    // DatagramSocket dùng để gửi/nhận gói tin UDP
    private DatagramSocket udpSocket;

    // Địa chỉ IP của server để gửi packet đến
    private InetAddress diaChiServer;

    private String host;
    private int port;

    // Kích thước tối đa của 1 gói tin UDP (64KB)
    private static final int MAX_UDP_PACKET_SIZE = 65535;

    // Thời gian chờ nhận response tối đa (ms)
    private static final int TIMEOUT_NHAN_RESPONSE = 10000;

    /**
     * "Kết nối" đến UDP Server.
     * Tạo DatagramSocket, lưu địa chỉ server, gửi PING kiểm tra.
     */
    public synchronized void ketNoiDenUDPServer(String host, int port) throws IOException {
        ngonKetNoi();
        this.host = host;
        this.port = port;

        // Phân giải tên miền/IP thành InetAddress
        diaChiServer = InetAddress.getByName(host);

        // Tạo DatagramSocket (OS tự chọn port cho client)
        udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(TIMEOUT_NHAN_RESPONSE);

        // Gửi PING để xác nhận server đang hoạt động
        String response = guiUDPPacketVaNhanResponse(Protocol.PING);
        if (!Protocol.PONG.equals(response.trim())) {
            ngonKetNoi();
            throw new IOException("Server không phản hồi PING");
        }
    }

    /**
     * Ngắt kết nối - đóng DatagramSocket.
     */
    public synchronized void ngonKetNoi() {
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        udpSocket = null;
        diaChiServer = null;
    }

    /**
     * Kiểm tra server còn phản hồi không bằng cách gửi PING packet.
     * Nhận được PONG → server còn sống. Timeout → server không phản hồi.
     */
    public synchronized boolean kiemTraKetNoi() {
        if (udpSocket == null || udpSocket.isClosed() || diaChiServer == null) return false;
        try {
            int timeoutCu = udpSocket.getSoTimeout();
            udpSocket.setSoTimeout(3000);
            String response = guiUDPPacketVaNhanResponse(Protocol.PING);
            udpSocket.setSoTimeout(timeoutCu);
            return Protocol.PONG.equals(response.trim());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gửi 1 lệnh đơn giản (REGISTER, LOGIN...) qua UDP và nhận phản hồi.
     */
    public synchronized String guiLenhDon(String command) throws IOException {
        if (udpSocket == null) throw new IOException("Vui lòng kết nối đến Server trước!");
        return guiUDPPacketVaNhanResponse(command);
    }

    /**
     * Gửi email qua UDP - gộp tất cả thông tin vào 1 packet.
     *
     * Format packet gửi đi:
     *   SEND_MAIL\n
     *   nguoiNhan\n
     *   ipNguoiGui\n
     *   thoiGian\n
     *   tieuDe\n
     *   noiDung\n
     *   <<END_CONTENT>>
     */
    public synchronized String[] guiEmail(String nguoiNhan, String ipNguoiGui,
                                          String thoiGian, String tieuDe,
                                          String noiDung) throws IOException {
        if (udpSocket == null) throw new IOException("Vui lòng kết nối đến Server trước!");

        // Đóng gói toàn bộ email vào 1 chuỗi, phân tách bằng "\n"
        StringBuilder goiTin = new StringBuilder();
        goiTin.append(Protocol.SEND_MAIL).append("\n");
        goiTin.append(nguoiNhan).append("\n");
        goiTin.append(ipNguoiGui).append("\n");
        goiTin.append(thoiGian).append("\n");
        goiTin.append(tieuDe).append("\n");
        goiTin.append(noiDung).append("\n");
        goiTin.append(Protocol.END_CONTENT);

        String response = guiUDPPacketVaNhanResponse(goiTin.toString());
        return response.split("\\|", 2);
    }

    /**
     * Đọc nội dung 1 email - gửi lệnh, nhận toàn bộ trong 1 response packet.
     * Response format: MAIL_START\n<nội dung nhiều dòng>\nMAIL_END
     */
    public synchronized List<String> docEmail(String tenTaiKhoan, String tenFile) throws IOException {
        if (udpSocket == null) throw new IOException("Vui lòng kết nối đến Server trước!");

        String response = guiUDPPacketVaNhanResponse(
            Protocol.READ_MAIL + "|" + tenTaiKhoan + "|" + tenFile);

        if (response.startsWith(Protocol.FAIL)) {
            throw new IOException(response.length() > 5 ? response.substring(5) : "Lỗi đọc mail");
        }

        // Parse nội dung từ response packet
        List<String> cacDong = new ArrayList<>();
        String[] mangDong = response.split("\n");
        boolean dangDocNoiDung = false;
        for (String dong : mangDong) {
            if (dong.equals(Protocol.MAIL_START)) {
                dangDocNoiDung = true;
                continue;
            }
            if (dong.equals(Protocol.MAIL_END)) break;
            if (dangDocNoiDung) cacDong.add(dong);
        }
        return cacDong;
    }

    /**
     * HÀM CỐT LÕI UDP: Gửi 1 DatagramPacket và nhận 1 DatagramPacket phản hồi.
     *
     * Quy trình:
     *   1. Chuyển String → byte[] (UTF-8)
     *   2. Tạo DatagramPacket chứa data + địa chỉ server
     *   3. udpSocket.send() - gửi packet qua mạng
     *   4. Tạo DatagramPacket rỗng để nhận response
     *   5. udpSocket.receive() - chờ nhận packet phản hồi
     *   6. Chuyển byte[] nhận được → String
     */
    private String guiUDPPacketVaNhanResponse(String noiDungGui) throws IOException {

        // Bước 1: Chuyển String thành byte[]
        byte[] duLieuGui = noiDungGui.getBytes("UTF-8");

        // Bước 2: Tạo DatagramPacket gửi đi (chứa data + địa chỉ + port server)
        DatagramPacket packetGui = new DatagramPacket(
            duLieuGui, duLieuGui.length, diaChiServer, port);

        // Bước 3: Gửi packet
        udpSocket.send(packetGui);

        // Bước 4: Tạo buffer và DatagramPacket rỗng để nhận response
        byte[] bufferNhan = new byte[MAX_UDP_PACKET_SIZE];
        DatagramPacket packetNhan = new DatagramPacket(bufferNhan, bufferNhan.length);

        // Bước 5: Chờ nhận response packet từ server
        udpSocket.receive(packetNhan);

        // Bước 6: Chuyển byte[] thành String
        return new String(packetNhan.getData(), 0, packetNhan.getLength(), "UTF-8");
    }

    /* ── Getters ───────────────────────────────────────── */
    public String getHost() { return host; }
    public int getPort()    { return port; }

    public String getLocalIP() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "Unknown IP"; }
    }
}
