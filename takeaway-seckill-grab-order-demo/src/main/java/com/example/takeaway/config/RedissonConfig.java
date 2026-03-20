package com.example.takeaway.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        // Redisson 的总配置对象。
        Config config = new Config();

        // 这里使用 Cluster 模式连接 Redis。
        // 因为我们的业务读写最终是落到 Redis Cluster 上。
        config.useClusterServers()
                .addNodeAddress(
                        "redis://127.0.0.1:7001",
                        "redis://127.0.0.1:7002",
                        "redis://127.0.0.1:7003",
                        "redis://127.0.0.1:7004",
                        "redis://127.0.0.1:7005",
                        "redis://127.0.0.1:7006"
                )
                // 扫描槽位信息的间隔时间。
                .setScanInterval(2000);

        return Redisson.create(config);
    }
}