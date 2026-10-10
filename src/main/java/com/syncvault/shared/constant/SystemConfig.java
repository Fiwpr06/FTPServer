package com.syncvault.shared.constant;

// Các thông số cấu hình mặc định toàn hệ thống SyncVault
public final class SystemConfig {
    private SystemConfig() {}

    public static final String SERVER_A_ID = "SERVER_A";
    public static final String SERVER_B_ID = "SERVER_B";

    public static final int DEFAULT_SERVER_A_PORT = 2121;
    public static final int DEFAULT_SERVER_B_PORT = 2122;

    public static final int DEFAULT_SERVER_A_RMI_PORT = 1099;
    public static final int DEFAULT_SERVER_B_RMI_PORT = 1100;

    public static final String RMI_SESSION_SERVICE = "SyncVault_SessionCoordinator";
    public static final String RMI_WRITE_FORWARDER_SERVICE = "SyncVault_WriteForwarder";
    public static final String RMI_REPLICATION_SERVICE = "SyncVault_ReplicationService";

    public static final int BUFFER_SIZE = 8192; // 8KB buffer for file streaming
    public static final int PASV_PORT_MIN = 30000;
    public static final int PASV_PORT_MAX = 30050;

    public static final int HEARTBEAT_TIMEOUT_SECONDS = 60;
}
