DROP TABLE IF EXISTS t_user_points_log;
DROP TABLE IF EXISTS t_payment_info;

CREATE TABLE t_payment_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    pay_amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);

CREATE TABLE t_user_points_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    points INT NOT NULL COMMENT '积分数量',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);