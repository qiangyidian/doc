package com.example.productcache.common;

public final class Constants {

    private Constants() {
    }

    /**
     * Redis 中商品缓存 key 的前缀。
     * 最终 key 形如：product:info:1
     */
    public static final String PRODUCT_CACHE_KEY_PREFIX = "product:info:";
}