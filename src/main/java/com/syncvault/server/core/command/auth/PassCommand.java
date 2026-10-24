package com.syncvault.server.core.command.auth;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.core.session.SessionState;
import com.syncvault.server.db.model.SessionRecord;
import com.syncvault.server.db.model.UserRecord;
import com.syncvault.server.rmi.common.ISessionCoordinator;
import com.syncvault.server.rmi.dto.SessionCheckRequest;
import com.syncvault.server.rmi.dto.SessionCheckResponse;
import com.syncvault.server.security.PasswordHasher;
import com.syncvault.shared.constant.FtpReplyCode;

import java.util.UUID;
import java.util.logging.Logger;

// Xử lý lệnh PASS và kiểm soát độc quyền phiên toàn cụm (Exclusive Single-Session)
public class PassCommand implements FtpCommand {
    private static final Logger LOGGER = Logger.getLogger(PassCommand.class.getName());

    @Override
    public void execute(ClientSession session, String argument) {
        if (session.getState() != SessionState.NEED_PASSWORD || session.getTempUsername() == null) {
            session.sendReply(FtpReplyCode.BAD_SEQUENCE_OF_COMMANDS, "Login with USER first");
            return;
        }

        String username = session.getTempUsername();
        String password = (argument != null) ? argument : "";
        ServerContext context = session.getContext();

        // 1. Xác thực tài khoản trong CSDL
        UserRecord user = context.getDataStore().getUserByUsername(username);
        if (user == null || !PasswordHasher.verify(password, user.getSalt(), user.getPasswordHash())) {
            LOGGER.warning(String.format("[AUTH_FAILED] Đăng nhập thất bại cho user '%s' từ IP %s", username, session.getClientIp()));
            session.setState(SessionState.CONNECTED);
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN, "Username or password incorrect");
            return;
        }

        // 2. Kiểm tra phiên đăng nhập cục bộ trên Server hiện tại
        SessionRecord localSession = context.getDataStore().getActiveSessionByUsername(username);
        if (localSession != null) {
            session.setState(SessionState.CONNECTED);
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN,
                    String.format("Tài khoản đang đăng nhập tại %s (IP: %s)!", context.getServerId(), localSession.getClientIp()));
            return;
        }

        // 3. Kiểm tra chéo phiên đăng nhập qua RMI với Server đối tác
        ISessionCoordinator peerSessionCoordinator = context.getPeerSessionCoordinator();
        if (peerSessionCoordinator != null) {
            try {
                String token = UUID.randomUUID().toString();
                SessionCheckRequest req = new SessionCheckRequest(
                        username,
                        context.getServerId(),
                        session.getClientIp(),
                        session.getClientPort(),
                        token
                );
                SessionCheckResponse response = peerSessionCoordinator.requestSessionAcquisition(req);
                if (!response.isAllowed()) {
                    LOGGER.warning(String.format("[CLUSTER_SESSION_REJECT] Từ chối đăng nhập '%s': %s", username, response.getMessage()));
                    session.setState(SessionState.CONNECTED);
                    session.sendReply(FtpReplyCode.NOT_LOGGED_IN, response.getMessage());
                    return;
                }
            } catch (Exception e) {
                LOGGER.warning("[RMI_CHECK_WARN] Không thể kết nối tới Server đối tác để kiểm tra phiên: " + e.getMessage());
                // Vẫn cho phép tiếp tục nếu peer offline, nhưng ghi log cảnh báo
            }
        }

        // 4. Cấp phiên thành công và lưu vào CSDL
        String token = UUID.randomUUID().toString();
        SessionRecord newSession = new SessionRecord(
                username,
                context.getServerId(),
                session.getClientIp(),
                session.getClientPort(),
                token
        );
        context.getDataStore().createSession(newSession);

        session.setAuthenticatedUser(username);
        session.setUserRecord(user);
        session.setState(SessionState.AUTHENTICATED);
        session.setCurrentVirtualDir("/");

        LOGGER.info(String.format("[AUTH_SUCCESS] User '%s' đăng nhập thành công vào %s từ IP %s:%d",
                username, context.getServerId(), session.getClientIp(), session.getClientPort()));
        session.sendReply(FtpReplyCode.USER_LOGGED_IN, "User " + username + " logged in successfully to " + context.getServerId());
    }
}
