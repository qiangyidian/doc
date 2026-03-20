package com.example.ordernotify.common;

/**
 * 常量类。
 *
 * 这类固定值单独抽出来，可以避免在多个文件里重复写字符串。
 */
public final class Constants {

    private Constants() {
    }

    public static final String ORDER_NOTIFY_EXCHANGE = "order.notify.exchange";
    public static final String ORDER_NOTIFY_QUEUE = "order.notify.queue";
    public static final String ORDER_NOTIFY_ROUTING_KEY = "order.notify.routing";
}