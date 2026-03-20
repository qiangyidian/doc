-- 先切换到业务库。
USE takeaway_demo;

-- 菜品主表。
CREATE TABLE IF NOT EXISTS t_meal (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    meal_name VARCHAR(100) NOT NULL,
    meal_desc VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    hot_flag TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 菜品库存表。
CREATE TABLE IF NOT EXISTS t_meal_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    meal_id BIGINT NOT NULL,
    stock INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_meal_id (meal_id)
);

-- 订单表。
CREATE TABLE IF NOT EXISTS t_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    meal_id BIGINT NOT NULL,
    order_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_user_meal (user_id, meal_id)
);

-- 初始化几条菜品数据。
INSERT INTO t_meal (id, merchant_id, meal_name, meal_desc, price, hot_flag, status)
VALUES
    (1001, 1, '1分钱霸王餐', '高并发秒杀示例商品', 0.01, 1, 1),
    (1002, 1, '超级鸡腿饭', '热门菜品，用于缓存预热', 18.80, 1, 1),
    (1003, 2, '藤椒牛肉饭', '普通菜品', 22.90, 0, 1)
ON DUPLICATE KEY UPDATE meal_name = VALUES(meal_name);

-- 初始化库存。
INSERT INTO t_meal_stock (meal_id, stock)
VALUES
    (1001, 20),
    (1002, 200),
    (1003, 100)
ON DUPLICATE KEY UPDATE stock = VALUES(stock);