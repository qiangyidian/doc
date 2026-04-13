DROP TABLE IF EXISTS t_product_info;

CREATE TABLE t_product_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    stock_count INT NOT NULL COMMENT '库存数量',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);