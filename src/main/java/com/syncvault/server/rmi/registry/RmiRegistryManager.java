package com.syncvault.server.rmi.registry;

import com.syncvault.server.ServerContext;
import com.syncvault.server.rmi.common.IReplicationService;
import com.syncvault.server.rmi.common.ISessionCoordinator;
import com.syncvault.server.rmi.common.IWriteForwarder;
import com.syncvault.server.rmi.service.ReplicationServiceImpl;
import com.syncvault.server.rmi.service.SessionCoordinatorImpl;
import com.syncvault.server.rmi.service.WriteForwarderImpl;
import com.syncvault.shared.constant.SystemConfig;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

// Quản lý khởi tạo và liên kết RMI Registry giữa các Server
public class RmiRegistryManager {
    private static final Logger LOGGER = Logger.getLogger(RmiRegistryManager.class.getName());

    private final ServerContext context;
    private Registry localRegistry;

    public RmiRegistryManager(ServerContext context) {
        this.context = context;
    }

    public boolean startLocalServices() {
        try {
            // Thiết lập hostname cho RMI stub để máy khác trong LAN có thể kết nối
            String localIp = com.syncvault.shared.util.NetworkUtil.getLocalIpAddress();
            System.setProperty("java.rmi.server.hostname", localIp);

            int port = context.getRmiPort();
            try {
                localRegistry = LocateRegistry.createRegistry(port);
            } catch (Exception e) {
                localRegistry = LocateRegistry.getRegistry(port);
            }

            // 1. Luôn đăng ký dịch vụ điều phối phiên trên mọi server
            SessionCoordinatorImpl sessionCoord = new SessionCoordinatorImpl(context);
            localRegistry.rebind(SystemConfig.RMI_SESSION_SERVICE, sessionCoord);

            // 2. Server A (Primary) đăng ký WriteForwarder
            if (context.isPrimary()) {
                WriteForwarderImpl writeForwarder = new WriteForwarderImpl(context);
                localRegistry.rebind(SystemConfig.RMI_WRITE_FORWARDER_SERVICE, writeForwarder);
            } else {
                // Server B (Secondary) đăng ký ReplicationService
                ReplicationServiceImpl replService = new ReplicationServiceImpl(context);
                localRegistry.rebind(SystemConfig.RMI_REPLICATION_SERVICE, replService);
            }

            LOGGER.info(String.format("[RMI] Khởi chạy RMI Services thành công tại Port %d cho %s", port, context.getServerId()));
            return true;
        } catch (Exception e) {
            LOGGER.severe("[RMI_INIT_ERR] Lỗi khởi chạy RMI Registry: " + e.getMessage());
            return false;
        }
    }

    // Kết nối đến Server đối tác để gọi RMI
    public boolean connectToPeer() {
        try {
            Registry peerRegistry = LocateRegistry.getRegistry(context.getPeerHost(), context.getPeerRmiPort());

            // Lấy Session Coordinator của đối tác
            try {
                ISessionCoordinator peerSession = (ISessionCoordinator) peerRegistry.lookup(SystemConfig.RMI_SESSION_SERVICE);
                context.setPeerSessionCoordinator(peerSession);
            } catch (Exception ignored) {}

            // Nếu Server B -> Lấy WriteForwarder của Server A
            if (!context.isPrimary()) {
                try {
                    IWriteForwarder writeForwarder = (IWriteForwarder) peerRegistry.lookup(SystemConfig.RMI_WRITE_FORWARDER_SERVICE);
                    context.setPeerWriteForwarder(writeForwarder);
                } catch (Exception ignored) {}
            } else {
                // Nếu Server A -> Lấy ReplicationService của Server B
                try {
                    IReplicationService replicationService = (IReplicationService) peerRegistry.lookup(SystemConfig.RMI_REPLICATION_SERVICE);
                    context.setPeerReplicationService(replicationService);
                } catch (Exception ignored) {}
            }

            LOGGER.info(String.format("[RMI_PEER] Đã liên kết tới Server đối tác %s:%d", context.getPeerHost(), context.getPeerRmiPort()));
            return true;
        } catch (Exception e) {
            LOGGER.warning(String.format("[RMI_PEER_WARN] Chưa thể kết nối tới Server đối tác %s:%d (Sẽ thử lại khi cần)",
                    context.getPeerHost(), context.getPeerRmiPort()));
            return false;
        }
    }
}
