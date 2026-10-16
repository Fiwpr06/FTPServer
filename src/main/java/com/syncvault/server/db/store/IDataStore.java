package com.syncvault.server.db.store;

import com.syncvault.server.db.model.FileMetadataRecord;
import com.syncvault.server.db.model.SessionRecord;
import com.syncvault.server.db.model.SyncLogRecord;
import com.syncvault.server.db.model.UserRecord;

import java.util.List;

// Giao diện trừu tượng hóa kho dữ liệu của SyncVault
public interface IDataStore {
    boolean init();
    boolean isAvailable();

    // Tài khoản
    UserRecord getUserByUsername(String username);
    boolean createUser(UserRecord user);

    // Phiên đăng nhập
    SessionRecord getActiveSessionByUsername(String username);
    boolean createSession(SessionRecord session);
    boolean removeSession(String username);
    boolean updateSessionHeartbeat(String username);
    List<SessionRecord> getAllActiveSessions();
    int cleanupExpiredSessions(int timeoutSeconds);

    // Tệp tin Metadata
    FileMetadataRecord getFileMetadata(String owner, String virtualPath);
    boolean saveOrUpdateFileMetadata(FileMetadataRecord meta);
    boolean deleteFileMetadata(String owner, String virtualPath);
    boolean renameFileMetadata(String owner, String oldVirtualPath, String newVirtualPath);
    List<FileMetadataRecord> listFiles(String owner, String parentVirtualPath);

    // Nhật ký đồng bộ
    boolean recordSyncLog(SyncLogRecord log);
    List<SyncLogRecord> getRecentSyncLogs(int limit);

    void close();
}
