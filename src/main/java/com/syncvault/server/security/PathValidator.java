package com.syncvault.server.security;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

// Chống tấn công Directory Traversal (Chroot Jail)
public final class PathValidator {
    private PathValidator() {}

    // Chuẩn hóa đường dẫn ảo logic của Client (luôn bắt đầu bằng / và dùng forward slash)
    public static String normalizeVirtualPath(String currentVirtualDir, String inputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            return currentVirtualDir;
        }
        inputPath = inputPath.trim().replace('\\', '/');

        String combined;
        if (inputPath.startsWith("/")) {
            combined = inputPath;
        } else {
            combined = (currentVirtualDir.endsWith("/") ? currentVirtualDir : currentVirtualDir + "/") + inputPath;
        }

        Path normalized = Paths.get(combined).normalize();
        String result = normalized.toString().replace('\\', '/');
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        return result;
    }

    // Ánh xạ đường dẫn ảo sang đường dẫn file thực tế trên đĩa cứng
    // Đảm bảo không bao giờ trỏ ra ngoài userRootFolder
    public static File resolvePhysicalFile(File userRootFolder, String virtualPath) {
        String relative = virtualPath.startsWith("/") ? virtualPath.substring(1) : virtualPath;
        Path targetPath = userRootFolder.toPath().resolve(relative).normalize();

        // Kiểm tra an toàn: targetPath bắt buộc phải là con của userRootFolder
        if (!targetPath.startsWith(userRootFolder.toPath().normalize())) {
            return null; // Bị phát hiện vượt quyền Chroot Jail
        }
        return targetPath.toFile();
    }
}
