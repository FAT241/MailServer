package server;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static common.AppConstants.DEFAULT_PORT;

/**
 * Quản lý vòng đời ServerSocket: khởi động, chấp nhận kết nối, dừng.
 * Mỗi client kết nối được spawn 1 ClientHandler thread riêng.
 */
public class ServerEngine {

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread acceptThread;

    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final MailStorage storage;
    private final Consumer<String> logger;
    private final Consumer<Integer> clientCountListener;

    private String serverIP;
    private int port = DEFAULT_PORT;

    public ServerEngine(Consumer<String> logger, Consumer<Integer> clientCountListener) {
        this.logger = logger;
        this.clientCountListener = clientCountListener;
        this.storage = new MailStorage();
    }

    public String getServerIP() { return serverIP; }
    public int getPort() { return port; }
    public boolean isRunning()  { return running; }

    /** Lấy danh sách IP:Port của các Client đang kết nối. */
    public java.util.List<String> getConnectedClientsInfo() {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (ClientHandler ch : clients) {
            list.add(ch.clientIP + ":" + ch.clientPort);
        }
        return list;
    }

    /** Khởi động server, bắt đầu lắng nghe kết nối. */
    public void start(String bindIp, int port) throws IOException {
        if (running) return;
        this.serverIP = bindIp;
        this.port = port;
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(bindIp));
        running = true;
        logger.accept("[SERVER] Máy chủ đã khởi động thành công tại "
            + serverIP + ":" + port);

        acceptThread = new Thread(this::acceptLoop, "AcceptThread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /** Dừng server, đóng tất cả client. */
    public void stop() {
        running = false;
        for (ClientHandler ch : clients) ch.close();
        clients.clear();
        clientCountListener.accept(0);
        try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); }
        catch (IOException ignored) {}
        logger.accept("[SERVER] Máy chủ đã dừng hoạt động.");
    }

    /* ── Accept Loop ───────────────────────────────────── */

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                ClientHandler handler = new ClientHandler(s, storage, serverIP, logger);
                clients.add(handler);
                clientCountListener.accept(clients.size());
                logger.accept("[CONNECT] Nhận kết nối từ Client IP: "
                    + handler.clientIP + " - Port: " + handler.clientPort);

                Thread t = new Thread(() -> {
                    try { handler.run(); }
                    finally {
                        clients.remove(handler);
                        clientCountListener.accept(clients.size());
                    }
                }, "Client-" + handler.clientIP);
                t.setDaemon(true);
                t.start();

            } catch (IOException e) {
                if (running) logger.accept("[ERROR] Lỗi accept: " + e.getMessage());
            }
        }
    }
}
