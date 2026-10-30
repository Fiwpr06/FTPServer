package com.syncvault.server.controller;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.FtpServer;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.db.connection.DatabaseConfig;
import com.syncvault.server.db.model.SessionRecord;
import com.syncvault.server.db.store.IDataStore;
import com.syncvault.server.db.store.InMemoryDataStore;
import com.syncvault.server.db.store.MySqlDataStore;
import com.syncvault.server.rmi.registry.RmiRegistryManager;
import com.syncvault.shared.constant.SystemConfig;
import com.syncvault.shared.model.ServerMode;
import com.syncvault.shared.util.NetworkUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// Điều khiển Trung tâm Giám sát và Điều hành Máy chủ SyncVault
public class ServerController {
    private static final Logger LOGGER = Logger.getLogger("com.syncvault");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public record SessionRow(String username, String clientIp, String loginTime, String serverId) {}

    @FXML private Label serverRoleLabel;
    @FXML private Label statusPillLabel;
    @FXML private Label dbStatusLabel;
    @FXML private Label peerStatusLabel;
    @FXML private Button serverToggleBtn;

    @FXML private Label ipDisplayLabel;
    @FXML private TextField ftpPortField;
    @FXML private TextField rmiPortField;
    @FXML private TextField peerHostField;
    @FXML private Button openStorageBtn;

    @FXML private Label sessionCountLabel;
    @FXML private TableView<SessionRow> sessionsTableView;
    @FXML private TableColumn<SessionRow, String> colUsername;
    @FXML private TableColumn<SessionRow, String> colClientIp;
    @FXML private TableColumn<SessionRow, String> colLoginTime;
    @FXML private TableColumn<SessionRow, String> colServerId;

    @FXML private TextArea logTextArea;
    @FXML private Label footerSessionsLabel;
    @FXML private Label footerStorageLabel;
    @FXML private Label serverClockLabel;

    private final ObservableList<SessionRow> sessionRows = FXCollections.observableArrayList();

    private ServerContext context;
    private RmiRegistryManager rmiManager;
    private FtpServer ftpServer;
    private IDataStore dataStore;

    private Timeline clockTimeline;
    private Timeline refreshTimeline;
    private Handler logHandler;

    private String serverId = SystemConfig.SERVER_A_ID;
    private ServerMode mode = ServerMode.PRIMARY;
    private int ftpPort = SystemConfig.DEFAULT_SERVER_A_PORT;
    private int rmiPort = SystemConfig.DEFAULT_SERVER_A_RMI_PORT;
    private int peerRmiPort = SystemConfig.DEFAULT_SERVER_B_RMI_PORT;
    private String peerHost = "127.0.0.1";

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().username()));
        colClientIp.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().clientIp()));
        colLoginTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().loginTime()));
        colServerId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().serverId()));
        sessionsTableView.setItems(sessionRows);

        String localIp = NetworkUtil.getLocalIpAddress();
        ipDisplayLabel.setText(localIp);

        setupLogCapture();
        startClock();
        startSessionPoller();
    }

    public void configure(String serverId, ServerMode mode, int ftpPort, int rmiPort, String peerHost, int peerRmiPort) {
        this.serverId = serverId;
        this.mode = mode;
        this.ftpPort = ftpPort;
        this.rmiPort = rmiPort;
        this.peerHost = peerHost;
        this.peerRmiPort = peerRmiPort;

        serverRoleLabel.setText(String.format("[ %s // %s ]", serverId, mode));
        ftpPortField.setText(String.valueOf(ftpPort));
        rmiPortField.setText(String.valueOf(rmiPort));
        peerHostField.setText(peerHost);

        // Khởi động các dịch vụ máy chủ
        startServerServices();
    }

    private void startServerServices() {
        File storageRoot = new File("server_storage_" + serverId.toLowerCase());
        if (!storageRoot.exists()) {
            storageRoot.mkdirs();
        }

        // 1. CSDL
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setDatabaseName("syncvault_db_" + serverId.toLowerCase());
        dataStore = new MySqlDataStore(dbConfig);
        boolean isMySql = dataStore.init();
        if (!isMySql) {
            appendLog("[DB_WARN] MySQL không khả dụng, kích hoạt In-Memory DataStore.");
            dataStore = new InMemoryDataStore();
            dataStore.init();
            dbStatusLabel.setText("IN-MEMORY");
            dbStatusLabel.setStyle("-fx-text-fill: #b8522e;");
        } else {
            dbStatusLabel.setText("MYSQL (3306)");
            dbStatusLabel.setStyle("-fx-text-fill: #276620;");
        }

        // 2. Server Context
        context = new ServerContext(serverId, mode, ftpPort, rmiPort, storageRoot, dataStore);
        context.setPeerHost(peerHost);
        context.setPeerRmiPort(peerRmiPort);
        context.setPeerServerId(mode == ServerMode.PRIMARY ? SystemConfig.SERVER_B_ID : SystemConfig.SERVER_A_ID);

        // 3. RMI
        rmiManager = new RmiRegistryManager(context);
        boolean rmiOk = rmiManager.startLocalServices();
        if (rmiOk) {
            appendLog("[RMI_OK] Dịch vụ RMI đã kích hoạt trên cổng " + rmiPort);
        } else {
            appendLog("[RMI_ERR] Không thể khởi tạo RMI Registry trên cổng " + rmiPort);
        }

        // 4. Kết nối sang server đối tác
        boolean peerConnected = rmiManager.connectToPeer();
        updatePeerStatus(peerConnected);

        // 5. FTP Server
        ftpServer = new FtpServer(context);
        boolean ftpOk = ftpServer.start();
        if (ftpOk) {
            appendLog(String.format("[FTP_READY] Server FTP %s lắng nghe tại cổng %d", serverId, ftpPort));
            statusPillLabel.setText("TRỰC TUYẾN");
            statusPillLabel.getStyleClass().clear();
            statusPillLabel.getStyleClass().add("server-status-pill-running");
            serverToggleBtn.setText("⏹ DỪNG MÁY CHỦ");
            serverToggleBtn.getStyleClass().clear();
            serverToggleBtn.getStyleClass().add("button-danger");
        } else {
            appendLog("[FTP_ERR] Không thể mở cổng FTP " + ftpPort);
            statusPillLabel.setText("LỖI CỔNG");
            statusPillLabel.getStyleClass().clear();
            statusPillLabel.getStyleClass().add("server-status-pill-stopped");
        }

        updateStorageCount();
    }

    private void updatePeerStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                peerStatusLabel.setText(String.format("KẾT NỐI (%s:%d)", peerHost, peerRmiPort));
                peerStatusLabel.setStyle("-fx-text-fill: #276620;");
            } else {
                peerStatusLabel.setText(String.format("CHỜ ĐỐI TÁC (%s:%d)", peerHost, peerRmiPort));
                peerStatusLabel.setStyle("-fx-text-fill: #b8522e;");
            }
        });
    }

    @FXML
    private void handleToggleServer() {
        if (ftpServer != null && ftpServer.isRunning()) {
            ftpServer.stop();
            statusPillLabel.setText("ĐÃ DỪNG");
            statusPillLabel.getStyleClass().clear();
            statusPillLabel.getStyleClass().add("server-status-pill-stopped");
            serverToggleBtn.setText("▶ KHỞI ĐỘNG");
            serverToggleBtn.getStyleClass().clear();
            serverToggleBtn.getStyleClass().add("button-action");
            appendLog("[SERVER] Đã dừng máy chủ FTP.");
        } else if (context != null) {
            ftpServer = new FtpServer(context);
            if (ftpServer.start()) {
                statusPillLabel.setText("TRỰC TUYẾN");
                statusPillLabel.getStyleClass().clear();
                statusPillLabel.getStyleClass().add("server-status-pill-running");
                serverToggleBtn.setText("⏹ DỪNG MÁY CHỦ");
                serverToggleBtn.getStyleClass().clear();
                serverToggleBtn.getStyleClass().add("button-danger");
                appendLog("[SERVER] Đã khởi động lại máy chủ FTP.");
            }
        }
    }

    @FXML
    private void handleCopyIp() {
        String ip = ipDisplayLabel.getText();
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(ip);
        clipboard.setContent(content);
        appendLog("[THÔNG TIN] Đã sao chép địa chỉ IP máy chủ: " + ip);
    }

    @FXML
    private void handleOpenStorage() {
        if (context == null) return;
        try {
            File root = context.getStorageRoot();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(root);
            }
        } catch (Exception e) {
            appendLog("[STORAGE_ERR] Không thể mở thư mục: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshSessions() {
        if (dataStore == null) return;
        List<SessionRecord> records = dataStore.getAllActiveSessions();
        sessionRows.clear();
        for (SessionRecord r : records) {
            sessionRows.add(new SessionRow(
                    r.getUsername(),
                    r.getClientIp(),
                    r.getLoginTime() != null ? r.getLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-",
                    r.getServerId()
            ));
        }
        sessionCountLabel.setText(sessionRows.size() + " PHIÊN");
        footerSessionsLabel.setText(String.format("Máy khách: %d phiên trực tuyến", sessionRows.size()));
    }

    @FXML
    private void handleDisconnectSession() {
        SessionRow selected = sessionsTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            appendLog("[SESSION] Vui lòng chọn một phiên cần ngắt kết nối.");
            return;
        }

        if (ftpServer != null) {
            for (ClientSession cs : ftpServer.getActiveClientSessions().values()) {
                if (selected.username().equals(cs.getAuthenticatedUser())) {
                    cs.close();
                    break;
                }
            }
        }
        if (dataStore != null) {
            dataStore.removeSession(selected.username());
        }
        appendLog("[DISCONNECT] Đã cưỡng chế ngắt kết nối tài khoản: " + selected.username());
        handleRefreshSessions();
    }

    @FXML
    private void handleClearLog() {
        logTextArea.clear();
    }

    private void setupLogCapture() {
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record == null || record.getMessage() == null) return;
                String msg = record.getMessage();
                Platform.runLater(() -> appendLog(msg));
            }
            @Override public void flush() {}
            @Override public void close() throws SecurityException {}
        };
        LOGGER.addHandler(logHandler);
    }

    private void appendLog(String message) {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        logTextArea.appendText(String.format("[%s] %s\n", time, message));
    }

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            serverClockLabel.setText("GIỜ HỆ THỐNG: " + LocalDateTime.now().format(TIME_FORMAT));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void startSessionPoller() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            handleRefreshSessions();
            updateStorageCount();
            if (rmiManager != null && context != null && context.getPeerSessionCoordinator() == null) {
                // Thử kết nối lại đối tác nếu chưa kết nối
                boolean reconnected = rmiManager.connectToPeer();
                updatePeerStatus(reconnected);
            }
        }));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    private void updateStorageCount() {
        if (context == null) return;
        File root = context.getStorageRoot();
        if (root.exists()) {
            File[] files = root.listFiles();
            int count = (files != null) ? files.length : 0;
            long bytes = 0;
            if (files != null) {
                for (File f : files) bytes += f.length();
            }
            double mb = bytes / (1024.0 * 1024.0);
            footerStorageLabel.setText(String.format("Kho lưu trữ: %d tệp (%.2f MB)", count, mb));
        }
    }

    public void cleanup() {
        if (clockTimeline != null) clockTimeline.stop();
        if (refreshTimeline != null) refreshTimeline.stop();
        if (logHandler != null) LOGGER.removeHandler(logHandler);

        if (ftpServer != null) {
            ftpServer.stop();
        }
        if (dataStore != null) {
            dataStore.close();
        }
    }
}
