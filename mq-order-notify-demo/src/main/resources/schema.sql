DROP TABLE IF EXISTS t_order_notify_log;
DROP TABLE IF EXISTS t_order_info;

CREATE TABLE t_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    amount DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
    status VARCHAR(32) NOT NULL COMMENT '订单状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);

CREATE TABLE t_order_notify_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    notify_type VARCHAR(32) NOT NULL COMMENT '通知类型',
    notify_status VARCHAR(32) NOT NULL COMMENT '通知状态',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);