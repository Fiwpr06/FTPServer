package com.syncvault.server.core;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.command.CommandFactory;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.shared.constant.FtpReplyCode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

// Máy chủ FTP đa luồng chuẩn RFC 959
public class FtpServer {
    private static final Logger LOGGER = Logger.getLogger(FtpServer.class.getName());

    private final ServerContext context;
    private final CommandFactory commandFactory;
    private final ExecutorService threadPool;
    private final ConcurrentHashMap<String, ClientSession> activeClientSessions = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public FtpServer(ServerContext context) {
        this.context = context;
        this.commandFactory = new CommandFactory();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public synchronized boolean start() {
        if (running) return true;

        try {
            this.serverSocket = new ServerSocket(context.getFtpPort());
            this.running = true;
            LOGGER.info(String.format("[FTP_SERVER] %s đã khởi động thành công trên cổng %d", context.getServerId(), context.getFtpPort()));

            threadPool.submit(this::listenForClients);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[FTP_START_ERR] Không thể mở cổng FTP " + context.getFtpPort() + ": " + e.getMessage(), e);
            return false;
        }
    }

    private void listenForClients() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    LOGGER.warning("[FTP_ACCEPT_WARN] " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        ClientSession session = new ClientSession(socket, context);
        String sessionKey = session.getClientIp() + ":" + session.getClientPort();
        activeClientSessions.put(sessionKey, session);

        try {
            // Chào mừng theo chuẩn RFC 959
            session.sendReply(FtpReplyCode.SERVICE_READY,
                    String.format("SyncVault FTP Service Ready (%s - Mode: %s)", context.getServerId(), context.getMode()));

            String line;
            while ((line = session.readCommand()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int spaceIdx = line.indexOf(' ');
                String cmdName = (spaceIdx != -1) ? line.substring(0, spaceIdx) : line;
                String args = (spaceIdx != -1) ? line.substring(spaceIdx + 1).trim() : null;

                FtpCommand command = commandFactory.getCommand(cmdName);
                if (command != null) {
                    command.execute(session, args);
                } else {
                    session.sendReply(FtpReplyCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, "Command '" + cmdName + "' not recognized");
                }
            }
        } catch (Exception e) {
            LOGGER.fine("[CLIENT_ERR] " + e.getMessage());
        } finally {
            activeClientSessions.remove(sessionKey);
            session.close();
        }
    }

    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}

        for (ClientSession session : activeClientSessions.values()) {
            session.close();
        }
        activeClientSessions.clear();
        threadPool.shutdownNow();
        LOGGER.info(String.format("[FTP_SERVER] %s đã dừng hoạt động", context.getServerId()));
    }

    public boolean isRunning() { return running; }
    public ConcurrentHashMap<String, ClientSession> getActiveClientSessions() { return activeClientSessions; }
}
