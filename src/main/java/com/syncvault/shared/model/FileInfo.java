package com.syncvault.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Thông tin tệp tin / thư mục logic
public class FileInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String virtualPath;
    private long size;
    private boolean isDirectory;
    private LocalDateTime lastModified;
    private String sha256;

    public FileInfo() {}

    public FileInfo(String name, String virtualPath, long size, boolean isDirectory, LocalDateTime lastModified, String sha256) {
        this.name = name;
        this.virtualPath = virtualPath;
        this.size = size;
        this.isDirectory = isDirectory;
        this.lastModified = lastModified;
        this.sha256 = sha256;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVirtualPath() {
        return virtualPath;
    }

    public void setVirtualPath(String virtualPath) {
        this.virtualPath = virtualPath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    // Định dạng dòng hiển thị theo chuẩn LIST UNIX: drwxr-xr-x 1 owner group 4096 Oct 26 12:00 filename
    public String toUnixListFormat(String owner) {
        String permissions = isDirectory ? "drwxr-xr-x" : "-rw-r--r--";
        String dateFormatted = (lastModified != null)
                ? String.format("%s %02d %02d:%02d", lastModified.getMonth().name().substring(0, 3), lastModified.getDayOfMonth(), lastModified.getHour(), lastModified.getMinute())
                : "Jan 01 00:00";
        return String.format("%s 1 %s %s %10d %s %s", permissions, owner != null ? owner : "syncvault", owner != null ? owner : "syncvault", size, dateFormatted, name);
    }
}
