package com.example.takeaway.common;

public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    // 菜品详情缓存 key。
    public static String mealKey(Long mealId) {
        return "takeaway:meal:" + mealId;
    }

    // 热门菜单缓存 key。
    public static String hotMealsKey(Long merchantId) {
        return "takeaway:hot:meals:" + merchantId;
    }

    // 缓存重建互斥锁 key。
    public static String rebuildCacheLockKey(Long mealId) {
        return "lock:rebuild_cache:" + mealId;
    }

    // 布隆过滤器位图 key。
    public static String mealBloomKey() {
        return "takeaway:bloom:meal";
    }

    // 秒杀商品锁 key。
    public static String mealLockKey(Long mealId) {
        return "heimalock:meal:" + mealId;
    }

    // 这个 key 使用了 Hash Tag。
    // 大括号里的 meal:1001 会参与槽位计算，从而强制多个 key 落到同一槽位。
    public static String stockKey(Long mealId) {
        return "{meal:" + mealId + "}:stock";
    }

    // 同样使用相同的 Hash Tag，保证和库存 key 处于同一个槽位。
    public static String orderMarkerKey(Long mealId, Long userId) {
        return "{meal:" + mealId + "}:order:" + userId;
    }
}