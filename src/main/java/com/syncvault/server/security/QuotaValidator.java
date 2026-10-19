package com.syncvault.server.security;

import java.io.File;

// Kiểm soát dung lượng lưu trữ tối đa của người dùng
public final class QuotaValidator {
    private QuotaValidator() {}

    public static long calculateDirectorySize(File directory) {
        if (directory == null || !directory.exists()) return 0;
        if (directory.isFile()) return directory.length();

        long length = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += calculateDirectorySize(file);
                }
            }
        }
        return length;
    }

    public static boolean checkQuota(File userRootDir, long incomingBytes, long maxAllowedBytes) {
        long currentSize = calculateDirectorySize(userRootDir);
        return (currentSize + incomingBytes) <= maxAllowedBytes;
    }
}
