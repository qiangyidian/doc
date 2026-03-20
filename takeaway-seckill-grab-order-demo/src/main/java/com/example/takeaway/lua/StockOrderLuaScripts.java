package com.example.takeaway.lua;

public final class StockOrderLuaScripts {

    private StockOrderLuaScripts() {
    }

    public static final String STOCK_ORDER_LUA = """
            -- KEYS[1] 是库存 key，例如 {meal:1001}:stock
            local stockKey = KEYS[1]
            -- KEYS[2] 是用户下单标记 key，例如 {meal:1001}:order:10001
            local orderKey = KEYS[2]

            -- 先判断这个用户是不是已经抢过了。
            if redis.call('EXISTS', orderKey) == 1 then
                -- 返回 2 表示重复下单。
                return 2
            end

            -- 读取当前库存。
            local stock = tonumber(redis.call('GET', stockKey))

            -- 如果库存不存在，说明预热没做好，或者 key 被删了。
            if stock == nil then
                -- 返回 3 表示库存 key 不存在。
                return 3
            end

            -- 如果库存已经小于等于 0，直接返回抢空。
            if stock <= 0 then
                -- 返回 0 表示库存不足。
                return 0
            end

            -- 扣减 1 份库存。
            redis.call('DECR', stockKey)

            -- 写入“这个用户已经抢到过”的标记。
            -- 这里用 SET 而不是 Hash，只是为了让新手更容易理解。
            redis.call('SET', orderKey, '1')

            -- 给下单标记一个过期时间，避免长期堆积无用 key。
            redis.call('EXPIRE', orderKey, 3600)

            -- 返回 1 表示抢单成功。
            return 1
            """;
}