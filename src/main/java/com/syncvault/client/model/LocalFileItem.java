package com.syncvault.client.model;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

// Mô hình tệp tin / thư mục trên máy tính cục bộ
public class LocalFileItem {
    private final File file;
    private final String name;
    private final long size;
    private final boolean directory;
    private final LocalDateTime lastModified;

    public LocalFileItem(File file) {
        this.file = file;
        this.name = file.getName();
        this.size = file.length();
        this.directory = file.isDirectory();
        this.lastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault());
    }

    public File getFile() { return file; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public boolean isDirectory() { return directory; }
    public LocalDateTime getLastModified() { return lastModified; }

    public String getFormattedSize() {
        if (directory) return "<DIR>";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }
}
