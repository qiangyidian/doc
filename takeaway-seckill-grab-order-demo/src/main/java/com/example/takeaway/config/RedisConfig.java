package com.example.takeaway.config;

import com.example.takeaway.lua.StockOrderLuaScripts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

// @Configuration 表示这是一个配置类，Spring 启动时会读取它。
@Configuration
public class RedisConfig {

    @Bean
    public DefaultRedisScript<Long> stockOrderRedisScript() {
        // DefaultRedisScript 是 Spring Data Redis 对 Lua 脚本的封装。
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // 这里把脚本正文设置进去。
        script.setScriptText(StockOrderLuaScripts.STOCK_ORDER_LUA);
        // 指定脚本返回值类型为 Long。
        script.setResultType(Long.class);
        return script;
    }
}