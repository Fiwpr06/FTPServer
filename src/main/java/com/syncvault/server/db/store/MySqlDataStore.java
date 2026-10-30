package com.syncvault.server.db.store;

import com.syncvault.server.db.connection.DatabaseConfig;
import com.syncvault.server.db.model.FileMetadataRecord;
import com.syncvault.server.db.model.SessionRecord;
import com.syncvault.server.db.model.SyncLogRecord;
import com.syncvault.server.db.model.UserRecord;
import com.syncvault.shared.model.SyncAction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Cài đặt kho dữ liệu MySQL kế thừa kiến trúc kết nối thông minh từ PeerChat
public class MySqlDataStore implements IDataStore {
    private static final Logger LOGGER = Logger.getLogger(MySqlDataStore.class.getName());

    private static final String[][] CANDIDATE_CREDS = {
            {"root", ""},
            {"root", "root"},
            {"root", "123456"},
            {"root", "admin"},
            {"root", "password"}
    };

    private final DatabaseConfig config;
    private Connection connection;
    private volatile boolean available = false;

    public MySqlDataStore(DatabaseConfig config) {
        this.config = (config != null) ? config : new DatabaseConfig();
    }

    @Override
    public synchronized boolean init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("[MYSQL_ERR] Không tìm thấy MySQL JDBC Driver: " + e.getMessage());
            available = false;
            return false;
        }

        // Bước 1: Thử kết nối trực tiếp với thông tin cấu hình
        if (tryConnect(config.getUser(), config.getPassword())) {
            available = true;
            setupDatabaseAndTables();
            seedDefaultUsersIfEmpty();
            return true;
        }

        // Bước 2: Quét danh sách Candidate Credentials
        LOGGER.info("[MYSQL] Thử quét thông tin đăng nhập mặc định phổ biến...");
        for (String[] cred : CANDIDATE_CREDS) {
            if (tryConnect(cred[0], cred[1])) {
                config.setUser(cred[0]);
                config.setPassword(cred[1]);
                available = true;
                setupDatabaseAndTables();
                seedDefaultUsersIfEmpty();
                LOGGER.info(String.format("[MYSQL] Kết nối thành công bằng tài khoản '%s'!", cred[0]));
                return true;
            }
        }

        LOGGER.warning("[MYSQL] Không thể kết nối tới MySQL Server trên " + config.getHost() + ":" + config.getPort());
        available = false;
        return false;
    }

    private boolean tryConnect(String user, String pass) {
        try (Connection testConn = DriverManager.getConnection(config.getServerJdbcUrl(), user, pass)) {
            return testConn != null && !testConn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private void setupDatabaseAndTables() {
        try {
            // Tạo CSDL nếu chưa tồn tại
            try (Connection conn = DriverManager.getConnection(config.getServerJdbcUrl(), config.getUser(), config.getPassword());
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + config.getDatabaseName() +
                        " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }

            // Mở kết nối chính thức đến CSDL
            this.connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUser(), config.getPassword());

            // Nạp và thực thi script schema.sql
            InputStream is = getClass().getResourceAsStream("/db/schema.sql");
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    StringBuilder sql = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("--")) continue;
                        sql.append(line).append(" ");
                        if (line.endsWith(";")) {
                            try (Statement stmt = this.connection.createStatement()) {
                                stmt.execute(sql.toString());
                            } catch (SQLException ex) {
                                LOGGER.warning("[MYSQL_SCHEMA_WARN] " + ex.getMessage());
                            }
                            sql.setLength(0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MYSQL_SETUP_ERR] Lỗi khởi tạo bảng: " + e.getMessage(), e);
        }
    }

    private void seedDefaultUsersIfEmpty() {
        try {
            if (getUserByUsername("student01") == null) {
                UserRecord student = new UserRecord("student01", "123456");
                student.setHomeDir("/student01");
                createUser(student);
            }
            if (getUserByUsername("student02") == null) {
                UserRecord student2 = new UserRecord("student02", "123456");
                student2.setHomeDir("/student02");
                createUser(student2);
            }
            if (getUserByUsername("admin") == null) {
                UserRecord admin = new UserRecord("admin", "admin123");
                admin.setHomeDir("/admin");
                createUser(admin);
            }
        } catch (Exception e) {
            LOGGER.warning("[MYSQL_SEED_WARN] " + e.getMessage());
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUser(), config.getPassword());
        }
        return connection;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public synchronized UserRecord getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserRecord u = new UserRecord();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    
                    String pwd = "";
                    try {
                        pwd = rs.getString("password");
                    } catch (SQLException e) {
                        try { pwd = rs.getString("password_hash"); } catch (SQLException ignored) {}
                    }
                    u.setPassword(pwd);

                    try {
                        u.setSalt(rs.getString("salt"));
                    } catch (SQLException ignored) {}

                    u.setHomeDir(rs.getString("home_dir"));
                    u.setMaxQuotaBytes(rs.getLong("max_quota_bytes"));
                    u.setCanUpload(rs.getBoolean("can_upload"));
                    u.setCanDownload(rs.getBoolean("can_download"));
                    u.setCanDelete(rs.getBoolean("can_delete"));
                    return u;
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_USER_ERR] " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized boolean createUser(UserRecord user) {
        String sql = "INSERT INTO users (username, password, home_dir, max_quota_bytes, can_upload, can_download, can_delete) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getHomeDir());
            ps.setLong(4, user.getMaxQuotaBytes());
            ps.setBoolean(5, user.isCanUpload());
            ps.setBoolean(6, user.isCanDownload());
            ps.setBoolean(7, user.isCanDelete());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            try {
                String fallbackSql = "INSERT INTO users (username, password_hash, salt, home_dir, max_quota_bytes, can_upload, can_download, can_delete) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps2 = getConnection().prepareStatement(fallbackSql)) {
                    ps2.setString(1, user.getUsername());
                    ps2.setString(2, user.getPassword());
                    ps2.setString(3, "");
                    ps2.setString(4, user.getHomeDir());
                    ps2.setLong(5, user.getMaxQuotaBytes());
                    ps2.setBoolean(6, user.isCanUpload());
                    ps2.setBoolean(7, user.isCanDownload());
                    ps2.setBoolean(8, user.isCanDelete());
                    return ps2.executeUpdate() > 0;
                }
            } catch (SQLException ex) {
                LOGGER.warning("[MYSQL_CREATE_USER_ERR] " + ex.getMessage());
                return false;
            }
        }
    }

    @Override
    public synchronized SessionRecord getActiveSessionByUsername(String username) {
        String sql = "SELECT * FROM active_sessions WHERE username = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SessionRecord s = new SessionRecord();
                    s.setId(rs.getInt("id"));
                    s.setUsername(rs.getString("username"));
                    s.setServerId(rs.getString("server_id"));
                    s.setClientIp(rs.getString("client_ip"));
                    s.setControlPort(rs.getInt("control_port"));
                    s.setSessionToken(rs.getString("session_token"));
                    Timestamp lt = rs.getTimestamp("login_time");
                    if (lt != null) s.setLoginTime(lt.toLocalDateTime());
                    Timestamp lh = rs.getTimestamp("last_heartbeat");
                    if (lh != null) s.setLastHeartbeat(lh.toLocalDateTime());
                    return s;
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_SESSION_ERR] " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized boolean createSession(SessionRecord session) {
        String sql = "INSERT INTO active_sessions (username, server_id, client_ip, control_port, session_token) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, session.getUsername());
            ps.setString(2, session.getServerId());
            ps.setString(3, session.getClientIp());
            ps.setInt(4, session.getControlPort());
            ps.setString(5, session.getSessionToken());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_CREATE_SESSION_ERR] " + e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized boolean removeSession(String username) {
        String sql = "DELETE FROM active_sessions WHERE username = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_REMOVE_SESSION_ERR] " + e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized boolean updateSessionHeartbeat(String username) {
        String sql = "UPDATE active_sessions SET last_heartbeat = NOW() WHERE username = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public synchronized List<SessionRecord> getAllActiveSessions() {
        List<SessionRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM active_sessions";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                SessionRecord s = new SessionRecord();
                s.setId(rs.getInt("id"));
                s.setUsername(rs.getString("username"));
                s.setServerId(rs.getString("server_id"));
                s.setClientIp(rs.getString("client_ip"));
                s.setControlPort(rs.getInt("control_port"));
                s.setSessionToken(rs.getString("session_token"));
                Timestamp lt = rs.getTimestamp("login_time");
                if (lt != null) s.setLoginTime(lt.toLocalDateTime());
                Timestamp lh = rs.getTimestamp("last_heartbeat");
                if (lh != null) s.setLastHeartbeat(lh.toLocalDateTime());
                list.add(s);
            }
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_ALL_SESSIONS_ERR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized int cleanupExpiredSessions(int timeoutSeconds) {
        String sql = "DELETE FROM active_sessions WHERE TIMESTAMPDIFF(SECOND, last_heartbeat, NOW()) > ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, timeoutSeconds);
            return ps.executeUpdate();
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public synchronized FileMetadataRecord getFileMetadata(String owner, String virtualPath) {
        String sql = "SELECT * FROM file_metadata WHERE owner_username = ? AND virtual_path = ? AND is_deleted = 0";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, owner);
            ps.setString(2, virtualPath);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FileMetadataRecord f = new FileMetadataRecord();
                    f.setId(rs.getLong("id"));
                    f.setOwnerUsername(rs.getString("owner_username"));
                    f.setVirtualPath(rs.getString("virtual_path"));
                    f.setFileSize(rs.getLong("file_size"));
                    f.setSha256Checksum(rs.getString("sha256_checksum"));
                    f.setDirectory(rs.getBoolean("is_directory"));
                    Timestamp lm = rs.getTimestamp("last_modified");
                    if (lm != null) f.setLastModified(lm.toLocalDateTime());
                    return f;
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_META_ERR] " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized boolean saveOrUpdateFileMetadata(FileMetadataRecord meta) {
        String sql = "INSERT INTO file_metadata (owner_username, virtual_path, file_size, sha256_checksum, is_directory, is_deleted, last_modified) " +
                "VALUES (?, ?, ?, ?, ?, 0, NOW()) " +
                "ON DUPLICATE KEY UPDATE file_size = VALUES(file_size), sha256_checksum = VALUES(sha256_checksum), is_deleted = 0, last_modified = NOW()";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, meta.getOwnerUsername());
            ps.setString(2, meta.getVirtualPath());
            ps.setLong(3, meta.getFileSize());
            ps.setString(4, meta.getSha256Checksum());
            ps.setBoolean(5, meta.isDirectory());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_SAVE_META_ERR] " + e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized boolean deleteFileMetadata(String owner, String virtualPath) {
        String sql = "UPDATE file_metadata SET is_deleted = 1 WHERE owner_username = ? AND virtual_path = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, owner);
            ps.setString(2, virtualPath);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public synchronized boolean renameFileMetadata(String owner, String oldVirtualPath, String newVirtualPath) {
        String sql = "UPDATE file_metadata SET virtual_path = ? WHERE owner_username = ? AND virtual_path = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, newVirtualPath);
            ps.setString(2, owner);
            ps.setString(3, oldVirtualPath);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public synchronized List<FileMetadataRecord> listFiles(String owner, String parentVirtualPath) {
        List<FileMetadataRecord> list = new ArrayList<>();
        String prefix = parentVirtualPath.endsWith("/") ? parentVirtualPath : parentVirtualPath + "/";
        String sql = "SELECT * FROM file_metadata WHERE owner_username = ? AND virtual_path LIKE ? AND is_deleted = 0";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, owner);
            ps.setString(2, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FileMetadataRecord f = new FileMetadataRecord();
                    f.setId(rs.getLong("id"));
                    f.setOwnerUsername(rs.getString("owner_username"));
                    f.setVirtualPath(rs.getString("virtual_path"));
                    f.setFileSize(rs.getLong("file_size"));
                    f.setSha256Checksum(rs.getString("sha256_checksum"));
                    f.setDirectory(rs.getBoolean("is_directory"));
                    Timestamp lm = rs.getTimestamp("last_modified");
                    if (lm != null) f.setLastModified(lm.toLocalDateTime());
                    list.add(f);
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_LIST_ERR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized boolean recordSyncLog(SyncLogRecord log) {
        String sql = "INSERT INTO sync_logs (action, source_server, target_server, virtual_path, checksum, status, message) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, log.getAction().name());
            ps.setString(2, log.getSourceServer());
            ps.setString(3, log.getTargetServer());
            ps.setString(4, log.getVirtualPath());
            ps.setString(5, log.getChecksum());
            ps.setString(6, log.getStatus());
            ps.setString(7, log.getMessage());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public synchronized List<SyncLogRecord> getRecentSyncLogs(int limit) {
        List<SyncLogRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM sync_logs ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SyncLogRecord r = new SyncLogRecord();
                    r.setId(rs.getLong("id"));
                    try {
                        r.setAction(SyncAction.valueOf(rs.getString("action")));
                    } catch (Exception ex) {
                        r.setAction(SyncAction.UPLOAD);
                    }
                    r.setSourceServer(rs.getString("source_server"));
                    r.setTargetServer(rs.getString("target_server"));
                    r.setVirtualPath(rs.getString("virtual_path"));
                    r.setChecksum(rs.getString("checksum"));
                    r.setStatus(rs.getString("status"));
                    r.setMessage(rs.getString("message"));
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) r.setTimestamp(ts.toLocalDateTime());
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("[MYSQL_SYNC_LOG_ERR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }
}
