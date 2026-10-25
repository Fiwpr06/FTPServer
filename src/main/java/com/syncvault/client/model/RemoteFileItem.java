package com.syncvault.client.model;

// Mô hình tệp tin / thư mục trên máy chủ FTP từ xa
public class RemoteFileItem {
    private final String name;
    private final long size;
    private final boolean directory;
    private final String permissions;
    private final String lastModifiedRaw;

    public RemoteFileItem(String name, long size, boolean directory, String permissions, String lastModifiedRaw) {
        this.name = name;
        this.size = size;
        this.directory = directory;
        this.permissions = permissions;
        this.lastModifiedRaw = lastModifiedRaw;
    }

    public String getName() { return name; }
    public long getSize() { return size; }
    public boolean isDirectory() { return directory; }
    public String getPermissions() { return permissions; }
    public String getLastModifiedRaw() { return lastModifiedRaw; }

    public String getFormattedSize() {
        if (directory) return "<DIR>";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }
}
