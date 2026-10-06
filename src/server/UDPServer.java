package server;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static common.AppConstants.DEFAULT_PORT;

/**
 * Lớp chính quản lý máy chủ Mail sử dụng giao thức UDP.
 * Dùng DatagramSocket để nhận/gửi gói tin (DatagramPacket).
 * UDP là connectionless - không cần tạo kết nối riêng cho mỗi client.
 */
public class UDPServer {

    // DatagramSocket dùng để gửi/nhận gói tin UDP
    private DatagramSocket udpSocket;

    private volatile boolean dangChay = false;
    private Thread luongNhanPacket;

    // Client đang kết nối: "IP:Port" -> thời điểm hoạt động gần nhất.
    // QUIT xóa ngay; không hoạt động quá CLIENT_TIMEOUT_MS thì bị loại (app tắt → hết ping).
    private final Map<String, Long> danhSachClient = new ConcurrentHashMap<>();
    private static final long CLIENT_TIMEOUT_MS = 20_000;

    private final MailStorage storage;
    private final Consumer<String> logger;

    // Bộ xử lý logic cho từng packet nhận được
    private UDPRequestHandler udpRequestHandler;

    private String serverIP;
    private int port = DEFAULT_PORT;

    // Kích thước tối đa của 1 gói tin UDP (64KB)
    private static final int MAX_UDP_PACKET_SIZE = 65535;

    public UDPServer(Consumer<String> logger) {
        this.logger = logger;
        this.storage = new MailStorage();
    }

    public String getServerIP() { return serverIP; }
    public int getPort()        { return port; }
    public boolean isRunning()  { return dangChay; }

    /**
     * Khởi động UDP Server.
     * Tạo DatagramSocket bind vào IP:Port, rồi chạy thread lắng nghe packet.
     */
    public void startUDPServer(String bindIp, int port) throws IOException {
        if (dangChay) return;
        this.serverIP = bindIp;
        this.port = port;

        // Tạo DatagramSocket và bind vào địa chỉ IP:Port
        InetAddress diaChiBind = InetAddress.getByName(bindIp);
        udpSocket = new DatagramSocket(port, diaChiBind);

        udpRequestHandler = new UDPRequestHandler(storage, serverIP, logger);
        dangChay = true;

        logger.accept("[SERVER] Máy chủ UDP đã khởi động tại " + serverIP + ":" + port);

        // Tạo thread liên tục nhận packet
        luongNhanPacket = new Thread(this::voiLapNhanUDPPacket, "UDP-Receive-Thread");
        luongNhanPacket.setDaemon(true);
        luongNhanPacket.start();
    }

    /**
     * Dừng UDP Server. Đóng DatagramSocket, xóa danh sách client.
     */
    public void stopUDPServer() {
        dangChay = false;
        danhSachClient.clear();

        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }

        logger.accept("[SERVER] Máy chủ UDP đã dừng hoạt động.");
    }

    /**
     * Vòng lặp chính của UDP Server:
     *   1. Chờ nhận DatagramPacket từ client  (udpSocket.receive)
     *   2. Đọc nội dung packet thành String
     *   3. Gọi UDPRequestHandler.xuLyRequest() để xử lý
     *   4. Đóng gói response vào DatagramPacket và gửi lại (udpSocket.send)
     */
    private void voiLapNhanUDPPacket() {
        byte[] bufferNhan = new byte[MAX_UDP_PACKET_SIZE];

        while (dangChay) {
            try {
                // Bước 1: Nhận 1 DatagramPacket từ client
                DatagramPacket packetNhanDuoc = new DatagramPacket(bufferNhan, bufferNhan.length);
                udpSocket.receive(packetNhanDuoc);

                // Bước 2: Lấy thông tin client từ packet
                InetAddress ipClient  = packetNhanDuoc.getAddress();
                int portClient        = packetNhanDuoc.getPort();
                String clientKey      = ipClient.getHostAddress() + ":" + portClient;

                // Bước 3: Đọc nội dung packet thành String
                String noiDungRequest = new String(
                    packetNhanDuoc.getData(), 0,
                    packetNhanDuoc.getLength(), "UTF-8"
                ).trim();

                if (noiDungRequest.isEmpty()) continue;

                long nowMs = System.currentTimeMillis();

                // Loại client không hoạt động quá 20s (đóng app là hết ping → tự mất)
                danhSachClient.entrySet().removeIf(e -> nowMs - e.getValue() > CLIENT_TIMEOUT_MS);

                // QUIT: client đăng xuất/ngắt kết nối → xóa ngay khỏi danh sách
                if (noiDungRequest.equals(common.Protocol.QUIT)
                        || noiDungRequest.startsWith(common.Protocol.QUIT + "|")) {
                    String tenUser = noiDungRequest.contains("|")
                        ? noiDungRequest.substring(noiDungRequest.indexOf('|') + 1).trim() : "";
                    if (danhSachClient.remove(clientKey) != null) {
                        logger.accept("[ĐĂNG XUẤT] '" + (tenUser.isEmpty() ? "?" : tenUser)
                            + "' → Ngắt kết nối");
                    }
                    byte[] dataGui = common.Protocol.SUCCESS.getBytes("UTF-8");
                    udpSocket.send(new DatagramPacket(dataGui, dataGui.length, ipClient, portClient));
                    continue;
                }

                // Ghi nhận client đang hoạt động (gia hạn thời gian)
                if (danhSachClient.put(clientKey, nowMs) == null) {
                    logger.accept("[KẾT NỐI] Client mới kết nối");
                }

                // Bước 4: Xử lý request và lấy response
                String noiDungResponse = udpRequestHandler.xuLyRequest(
                    noiDungRequest, ipClient.getHostAddress(), portClient);

                // Bước 5: Gửi response packet về cho client
                byte[] dataGui = noiDungResponse.getBytes("UTF-8");
                DatagramPacket packetGui = new DatagramPacket(
                    dataGui, dataGui.length, ipClient, portClient);
                udpSocket.send(packetGui);

            } catch (IOException e) {
                if (dangChay) {
                    logger.accept("[LỖI] Lỗi nhận/gửi UDP packet: " + e.getMessage());
                }
            }
        }
    }
}
