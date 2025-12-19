-- 清空表数据（按依赖顺序）
DELETE FROM user_seckill_record;
DELETE FROM seckill_order;
DELETE FROM seckill_activity;
DELETE FROM product_stock;
DELETE FROM product;

-- 插入测试商品数据
INSERT INTO product (id, name, description, price, image_url, status) VALUES
(1, 'iPhone 15 Pro Max', '苹果最新旗舰手机，钛金属边框，A17 Pro芯片', 9999.00, 'https://example.com/iphone.jpg', 1),
(2, '华为Mate 60 Pro', '华为旗舰手机，卫星通话，昆仑玻璃', 7999.00, 'https://example.com/mate60.jpg', 1),
(3, '小米14 Ultra', '徕卡影像，第二代骁龙8处理器', 5999.00, 'https://example.com/mi14.jpg', 1),
(4, 'MacBook Pro 16寸', 'M3 Max芯片，64GB内存，2TB SSD', 29999.00, 'https://example.com/macbook.jpg', 1),
(5, '索尼PlayStation 5', '光驱版，支持4K游戏', 3899.00, 'https://example.com/ps5.jpg', 1);

-- 插入商品库存数据
INSERT INTO product_stock (product_id, total_stock, available_stock, locked_stock) VALUES
(1, 10000, 10000, 0),
(2, 8000, 8000, 0),
(3, 6000, 6000, 0),
(4, 2000, 2000, 0),
(5, 5000, 5000, 0);

-- 插入秒杀活动数据
INSERT INTO seckill_activity (name, product_id, seckill_price, original_price, total_stock, available_stock, start_time, end_time, status, limit_per_user) VALUES
('iPhone 15 Pro Max 双11秒杀', 1, 8999.00, 9999.00, 1000, 1000, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR), 0, 1),
('华为Mate 60 Pro 限时秒杀', 2, 6999.00, 7999.00, 800, 800, DATE_ADD(NOW(), INTERVAL -1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1, 1),
('小米14 Ultra 零点抢购', 3, 5499.00, 5999.00, 600, 600, DATE_ADD(NOW(), INTERVAL -2 HOUR), DATE_ADD(NOW(), INTERVAL 1 HOUR), 1, 2),
('MacBook Pro 年终特惠', 4, 27999.00, 29999.00, 200, 200, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 0, 1),
('PS5 黑五秒杀活动', 5, 3499.00, 3899.00, 500, 500, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), 0, 1);

-- 创建用户表（如果需要）
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `username` varchar(50) NOT NULL COMMENT '用户名',
    `phone` varchar(20) COMMENT '手机号',
    `email` varchar(100) COMMENT '邮箱',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试用户
INSERT INTO user (username, phone, email) VALUES
('user1', '13800138001', 'user1@example.com'),
('user2', '13800138002', 'user2@example.com'),
('user3', '13800138003', 'user3@example.com'),
('user4', '13800138004', 'user4@example.com'),
('user5', '13800138005', 'user5@example.com');