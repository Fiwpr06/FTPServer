package com.syncvault.server.rmi.service;

import com.syncvault.server.ServerContext;
import com.syncvault.server.db.model.SessionRecord;
import com.syncvault.server.rmi.common.ISessionCoordinator;
import com.syncvault.server.rmi.dto.SessionCheckRequest;
import com.syncvault.server.rmi.dto.SessionCheckResponse;
import com.syncvault.shared.util.DateTimeUtil;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

// Hiện thực dịch vụ RMI điều phối phiên đăng nhập độc quyền
public class SessionCoordinatorImpl extends UnicastRemoteObject implements ISessionCoordinator {
    private static final Logger LOGGER = Logger.getLogger(SessionCoordinatorImpl.class.getName());
    private final ServerContext context;

    public SessionCoordinatorImpl(ServerContext context) throws RemoteException {
        super(context.getRmiPort());
        this.context = context;
    }

    @Override
    public synchronized SessionCheckResponse requestSessionAcquisition(SessionCheckRequest request) throws RemoteException {
        String username = request.getUsername();
        SessionRecord existing = context.getDataStore().getActiveSessionByUsername(username);

        if (existing != null) {
            // Tài khoản đang đăng nhập tại server này
            LOGGER.warning(String.format("[RMI_SESSION] Từ chối cấp phiên cho '%s': Đang online tại %s (IP: %s)!",
                    username, context.getServerId(), existing.getClientIp()));
            return SessionCheckResponse.conflict(
                    context.getServerId(),
                    existing.getClientIp(),
                    DateTimeUtil.format(existing.getLoginTime())
            );
        }

        // Không có phiên xung đột tại server này
        return SessionCheckResponse.success();
    }

    @Override
    public synchronized void notifySessionReleased(String username, String serverId) throws RemoteException {
        LOGGER.info(String.format("[RMI_SESSION] Nhận thông báo giải phóng phiên cho '%s' từ %s", username, serverId));
        context.getDataStore().removeSession(username);
    }

    @Override
    public synchronized boolean isUserActive(String username) throws RemoteException {
        return context.getDataStore().getActiveSessionByUsername(username) != null;
    }
}
