-- SyncVault Database Schema (Auto-migrated by MySqlDataStore)

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    salt VARCHAR(64) DEFAULT '',
    home_dir VARCHAR(255) DEFAULT '/',
    max_quota_bytes BIGINT DEFAULT 104857600,
    can_upload TINYINT(1) DEFAULT 1,
    can_download TINYINT(1) DEFAULT 1,
    can_delete TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS active_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    server_id VARCHAR(20) NOT NULL,
    client_ip VARCHAR(50) NOT NULL,
    control_port INT NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    session_token VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_username VARCHAR(50) NOT NULL,
    virtual_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256_checksum VARCHAR(64) NOT NULL,
    is_directory TINYINT(1) DEFAULT 0,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_user_path (owner_username, virtual_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sync_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(20) NOT NULL,
    source_server VARCHAR(20) NOT NULL,
    target_server VARCHAR(20) NOT NULL,
    virtual_path VARCHAR(500) NOT NULL,
    checksum VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    message VARCHAR(500),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
