USE sa_token_demo;

-- 用户表。
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 角色表。
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL
);

-- 权限表。
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL
);

-- 用户角色关系表。
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- 角色权限关系表。
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);

-- 初始化用户：admin 和普通用户。
INSERT INTO sys_user (id, username, password, nickname, status)
VALUES
    (1, 'admin', '123456', '超级管理员', 1),
    (2, 'test', '123456', '普通测试用户', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

-- 初始化角色。
INSERT INTO sys_role (id, role_code, role_name)
VALUES
    (1, 'admin', '管理员'),
    (2, 'user', '普通用户')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 初始化权限。
INSERT INTO sys_permission (id, permission_code, permission_name)
VALUES
    (1, 'user.query', '查看用户'),
    (2, 'user.add', '新增用户'),
    (3, 'user.kickout', '踢人下线'),
    (4, 'user.disable', '封禁用户')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 用户与角色关系：admin 拥有 admin 角色，test 拥有 user 角色。
INSERT INTO sys_user_role (user_id, role_id)
VALUES
    (1, 1),
    (2, 2)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 角色与权限关系：管理员拥有全部权限，普通用户只有查看权限。
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 1),
    (1, 2),
    (1, 3),
    (1, 4),
    (2, 1)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);