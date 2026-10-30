package com.syncvault.server.db.store;

import com.syncvault.server.db.model.FileMetadataRecord;
import com.syncvault.server.db.model.SessionRecord;
import com.syncvault.server.db.model.SyncLogRecord;
import com.syncvault.server.db.model.UserRecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// Kho dữ liệu lưu trên bộ nhớ RAM (Thread-safe Fallback khi MySQL không khả dụng)
public class InMemoryDataStore implements IDataStore {
    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();
    private final Map<String, SessionRecord> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, FileMetadataRecord> fileMetadataMap = new ConcurrentHashMap<>();
    private final List<SyncLogRecord> syncLogs = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger userIdCounter = new AtomicInteger(1);
    private final AtomicInteger sessionIdCounter = new AtomicInteger(1);
    private final AtomicLong fileIdCounter = new AtomicLong(1);
    private final AtomicLong logIdCounter = new AtomicLong(1);

    @Override
    public boolean init() {
        seedDefaultUsers();
        return true;
    }

    private void seedDefaultUsers() {
        if (!users.containsKey("student01")) {
            UserRecord student = new UserRecord("student01", "123456");
            student.setId(userIdCounter.getAndIncrement());
            student.setHomeDir("/student01");
            users.put("student01", student);
        }
        if (!users.containsKey("student02")) {
            UserRecord student2 = new UserRecord("student02", "123456");
            student2.setId(userIdCounter.getAndIncrement());
            student2.setHomeDir("/student02");
            users.put("student02", student2);
        }
        if (!users.containsKey("admin")) {
            UserRecord admin = new UserRecord("admin", "admin123");
            admin.setId(userIdCounter.getAndIncrement());
            admin.setHomeDir("/admin");
            users.put("admin", admin);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public UserRecord getUserByUsername(String username) {
        return users.get(username);
    }

    @Override
    public boolean createUser(UserRecord user) {
        if (users.containsKey(user.getUsername())) return false;
        user.setId(userIdCounter.getAndIncrement());
        users.put(user.getUsername(), user);
        return true;
    }

    @Override
    public SessionRecord getActiveSessionByUsername(String username) {
        return activeSessions.get(username);
    }

    @Override
    public boolean createSession(SessionRecord session) {
        if (activeSessions.containsKey(session.getUsername())) {
            return false; // Ngăn chặn tạo 2 session cùng username
        }
        session.setId(sessionIdCounter.getAndIncrement());
        session.setLoginTime(LocalDateTime.now());
        session.setLastHeartbeat(LocalDateTime.now());
        activeSessions.put(session.getUsername(), session);
        return true;
    }

    @Override
    public boolean removeSession(String username) {
        return activeSessions.remove(username) != null;
    }

    @Override
    public boolean updateSessionHeartbeat(String username) {
        SessionRecord session = activeSessions.get(username);
        if (session != null) {
            session.setLastHeartbeat(LocalDateTime.now());
            return true;
        }
        return false;
    }

    @Override
    public List<SessionRecord> getAllActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    @Override
    public int cleanupExpiredSessions(int timeoutSeconds) {
        LocalDateTime now = LocalDateTime.now();
        List<String> expired = activeSessions.values().stream()
                .filter(s -> Duration.between(s.getLastHeartbeat(), now).getSeconds() > timeoutSeconds)
                .map(SessionRecord::getUsername)
                .collect(Collectors.toList());
        expired.forEach(activeSessions::remove);
        return expired.size();
    }

    private String makeMetaKey(String owner, String virtualPath) {
        return owner + ":" + virtualPath;
    }

    @Override
    public FileMetadataRecord getFileMetadata(String owner, String virtualPath) {
        FileMetadataRecord meta = fileMetadataMap.get(makeMetaKey(owner, virtualPath));
        if (meta != null && !meta.isDeleted()) return meta;
        return null;
    }

    @Override
    public boolean saveOrUpdateFileMetadata(FileMetadataRecord meta) {
        if (meta.getId() <= 0) {
            meta.setId(fileIdCounter.getAndIncrement());
        }
        meta.setLastModified(LocalDateTime.now());
        meta.setDeleted(false);
        fileMetadataMap.put(makeMetaKey(meta.getOwnerUsername(), meta.getVirtualPath()), meta);
        return true;
    }

    @Override
    public boolean deleteFileMetadata(String owner, String virtualPath) {
        FileMetadataRecord meta = fileMetadataMap.get(makeMetaKey(owner, virtualPath));
        if (meta != null) {
            meta.setDeleted(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean renameFileMetadata(String owner, String oldVirtualPath, String newVirtualPath) {
        FileMetadataRecord meta = fileMetadataMap.remove(makeMetaKey(owner, oldVirtualPath));
        if (meta != null) {
            meta.setVirtualPath(newVirtualPath);
            meta.setLastModified(LocalDateTime.now());
            fileMetadataMap.put(makeMetaKey(owner, newVirtualPath), meta);
            return true;
        }
        return false;
    }

    @Override
    public List<FileMetadataRecord> listFiles(String owner, String parentVirtualPath) {
        String prefix = parentVirtualPath.endsWith("/") ? parentVirtualPath : parentVirtualPath + "/";
        return fileMetadataMap.values().stream()
                .filter(m -> m.getOwnerUsername().equals(owner) && !m.isDeleted())
                .filter(m -> m.getVirtualPath().startsWith(prefix))
                .collect(Collectors.toList());
    }

    @Override
    public boolean recordSyncLog(SyncLogRecord log) {
        log.setId(logIdCounter.getAndIncrement());
        log.setTimestamp(LocalDateTime.now());
        syncLogs.add(0, log);
        if (syncLogs.size() > 500) {
            syncLogs.remove(syncLogs.size() - 1);
        }
        return true;
    }

    @Override
    public List<SyncLogRecord> getRecentSyncLogs(int limit) {
        synchronized (syncLogs) {
            return syncLogs.stream().limit(limit).collect(Collectors.toList());
        }
    }

    @Override
    public void close() {
        activeSessions.clear();
        fileMetadataMap.clear();
    }
}
