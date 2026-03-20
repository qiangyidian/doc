package com.example.takeaway.schedule;

import com.example.takeaway.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealCacheWarmScheduler {

    private final CacheService cacheService;

    // 每天凌晨 3 点执行一次。
    @Scheduled(cron = "0 0 3 * * ?")
    public void warmHotMeals() {
        log.info("开始执行热门菜单缓存预热任务");
        // 传 null 表示预热全部热门菜单。
        cacheService.warmHotMeals(null);
        log.info("热门菜单缓存预热任务执行完成");
    }
}