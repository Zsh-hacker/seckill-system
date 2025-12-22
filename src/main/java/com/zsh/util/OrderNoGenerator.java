package com.zsh.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderNoGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static final int MAX_SEQUENCE = 9999;

    /**
     * 生成订单号：时间戳 + 序列号 + 随机码
     */
    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int sequence = SEQUENCE.getAndUpdate(prev -> (prev >= MAX_SEQUENCE) ? 0 : prev + 1);

        // 添加随机码防止重复
        int random = (int) (Math.random() * 90) + 10;   // 10-99

        return String.format("%s%04d%02d", timestamp, sequence, random);
    }

    /**
     * 生成带前缀的订单号
     */
    public static String generateWithPrefix(String prefix) {
        return prefix + generate();
    }

    /**
     * 生成秒杀订单号
     */
    public static String generateSeckillOrderNo() {
        return generateWithPrefix("SK");
    }
}
