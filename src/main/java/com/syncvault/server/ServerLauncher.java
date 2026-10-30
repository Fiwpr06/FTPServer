package com.syncvault.server;

import com.syncvault.server.core.FtpServer;
import com.syncvault.server.db.connection.DatabaseConfig;
import com.syncvault.server.db.store.IDataStore;
import com.syncvault.server.db.store.InMemoryDataStore;
import com.syncvault.server.db.store.MySqlDataStore;
import com.syncvault.server.rmi.registry.RmiRegistryManager;
import com.syncvault.shared.constant.SystemConfig;
import com.syncvault.shared.model.ServerMode;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

// Khởi chạy Máy chủ SyncVault Server (Hỗ trợ cả chế độ đồ họa GUI YoRHa lẫn Headless CLI)
public class ServerLauncher {
    private static final Logger LOGGER = Logger.getLogger(ServerLauncher.class.getName());

    public static void main(String[] args) {
        // Cấu hình UTF-8 cho console terminal
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
            for (Handler h : Logger.getLogger("").getHandlers()) {
                if (h instanceof ConsoleHandler) {
                    h.setEncoding("UTF-8");
                }
            }
        } catch (Exception ignored) {}

        boolean headless = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--headless") || arg.equalsIgnoreCase("--cli") || arg.equalsIgnoreCase("-h")) {
                headless = true;
                break;
            }
        }

        if (headless) {
            runHeadless(args);
        } else {
            // Mặc định khởi chạy giao diện đồ họa Server Dashboard UI/UX
            ServerApp.main(args);
        }
    }

    public static void runHeadless(String[] args) {
        String modeArg = "primary";
        String peerHost = "127.0.0.1";

        for (String arg : args) {
            if (arg.equalsIgnoreCase("--secondary") || arg.equalsIgnoreCase("-s")) {
                modeArg = "secondary";
            } else if (arg.startsWith("--peer=")) {
                peerHost = arg.substring("--peer=".length()).trim();
            } else if (!arg.startsWith("-")) {
                peerHost = arg.trim();
            }
        }

        boolean isPrimary = modeArg.equalsIgnoreCase("primary");
        String serverId = isPrimary ? SystemConfig.SERVER_A_ID : SystemConfig.SERVER_B_ID;
        ServerMode mode = isPrimary ? ServerMode.PRIMARY : ServerMode.SECONDARY;
        int ftpPort = isPrimary ? SystemConfig.DEFAULT_SERVER_A_PORT : SystemConfig.DEFAULT_SERVER_B_PORT;
        int rmiPort = isPrimary ? SystemConfig.DEFAULT_SERVER_A_RMI_PORT : SystemConfig.DEFAULT_SERVER_B_RMI_PORT;
        int peerRmiPort = isPrimary ? SystemConfig.DEFAULT_SERVER_B_RMI_PORT : SystemConfig.DEFAULT_SERVER_A_RMI_PORT;

        File storageRoot = new File("server_storage_" + serverId.toLowerCase());

        // 1. Khởi tạo Cơ sở dữ liệu (Thử MySQL trước, fallback sang InMemory nếu MySQL chưa bật)
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setDatabaseName("syncvault_db_" + serverId.toLowerCase());
        IDataStore dataStore = new MySqlDataStore(dbConfig);
        if (!dataStore.init()) {
            LOGGER.warning("[DB_FALLBACK] MySQL không khả dụng. Sử dụng In-Memory DataStore dự phòng.");
            dataStore = new InMemoryDataStore();
            dataStore.init();
        }

        // 2. Tạo Server Context
        ServerContext context = new ServerContext(serverId, mode, ftpPort, rmiPort, storageRoot, dataStore);
        context.setPeerHost(peerHost);
        context.setPeerRmiPort(peerRmiPort);
        context.setPeerServerId(isPrimary ? SystemConfig.SERVER_B_ID : SystemConfig.SERVER_A_ID);

        // 3. Khởi tạo RMI Services
        RmiRegistryManager rmiManager = new RmiRegistryManager(context);
        rmiManager.startLocalServices();

        // 4. Thử liên kết RMI sang đối tác (Peer)
        rmiManager.connectToPeer();

        // 5. Khởi chạy FTP Server Socket
        FtpServer ftpServer = new FtpServer(context);
        ftpServer.start();

        String localIp = com.syncvault.shared.util.NetworkUtil.getLocalIpAddress();
        LOGGER.info(String.format("====================================================\n" +
                        "  SYNCVAULT // %s ĐÃ SẴN SÀNG HOẠT ĐỘNG (HEADLESS)\n" +
                        "  - Vai trò: %s\n" +
                        "  - Địa chỉ IP máy này (LAN): %s\n" +
                        "  - Kênh điều khiển FTP: Port %d\n" +
                        "  - Kênh phân tán RMI: Port %d\n" +
                        "  - Server đối tác: %s:%d\n" +
                        "  - Kho lưu trữ: %s\n" +
                        "====================================================",
                serverId, mode, localIp, ftpPort, rmiPort, peerHost, peerRmiPort, storageRoot.getAbsolutePath()));

        // Hook dọn dẹp khi tắt tiến trình (Ctrl+C)
        final IDataStore finalDataStore = dataStore;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[SHUTDOWN] Đang dừng " + serverId + "...");
            ftpServer.stop();
            finalDataStore.close();
        }));
    }
}
