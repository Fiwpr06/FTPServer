package com.syncvault.shared.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

// Tiện ích tính toán mã băm SHA-256 xác thực tính toàn vẹn của tệp tin
public final class ChecksumUtil {
    private ChecksumUtil() {}

    public static String calculateSHA256(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return "";
        }
        try (InputStream is = new FileInputStream(file)) {
            return calculateSHA256(is);
        } catch (Exception e) {
            return "";
        }
    }

    public static String calculateSHA256(InputStream is) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            return "";
        }
    }

    public static String calculateSHA256(byte[] data) {
        if (data == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
