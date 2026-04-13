package com.example.lockdemo.controller;

import com.example.lockdemo.common.Result;
import com.example.lockdemo.dto.ConfigUpdateRequest;
import com.example.lockdemo.dto.DeductStockRequest;
import com.example.lockdemo.dto.DistributedOrderRequest;
import com.example.lockdemo.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/locks")
public class LockDemoController {

    private final SynchronizedStockService synchronizedStockService;
    private final ReentrantLockStockService reentrantLockStockService;
    private final ReadWriteConfigService readWriteConfigService;
    private final ConditionTaskQueueService conditionTaskQueueService;
    private final StampedLockPriceService stampedLockPriceService;
    private final AtomicCounterService atomicCounterService;
    private final RedissonOrderService redissonOrderService;
    private final RedissonFairLockService redissonFairLockService;
    private final RedissonReadWriteService redissonReadWriteService;

    public LockDemoController(SynchronizedStockService synchronizedStockService,
                              ReentrantLockStockService reentrantLockStockService,
                              ReadWriteConfigService readWriteConfigService,
                              ConditionTaskQueueService conditionTaskQueueService,
                              StampedLockPriceService stampedLockPriceService,
                              AtomicCounterService atomicCounterService,
                              RedissonOrderService redissonOrderService,
                              RedissonFairLockService redissonFairLockService,
                              RedissonReadWriteService redissonReadWriteService) {
        this.synchronizedStockService = synchronizedStockService;
        this.reentrantLockStockService = reentrantLockStockService;
        this.readWriteConfigService = readWriteConfigService;
        this.conditionTaskQueueService = conditionTaskQueueService;
        this.stampedLockPriceService = stampedLockPriceService;
        this.atomicCounterService = atomicCounterService;
        this.redissonOrderService = redissonOrderService;
        this.redissonFairLockService = redissonFairLockService;
        this.redissonReadWriteService = redissonReadWriteService;
    }

    @PostMapping("/synchronized/deduct")
    public Result<Integer> deductBySynchronized(@Valid @RequestBody DeductStockRequest request) {
        return Result.success(synchronizedStockService.deduct(request.getCount()));
    }

    @PostMapping("/reentrant/deduct")
    public Result<Integer> deductByReentrantLock(@Valid @RequestBody DeductStockRequest request) throws Exception {
        return Result.success(reentrantLockStockService.deduct(request.getCount()));
    }

    @GetMapping("/config/read")
    public Result<String> readConfig(@RequestParam String key) {
        return Result.success(readWriteConfigService.read(key));
    }

    @PostMapping("/config/write")
    public Result<String> writeConfig(@Valid @RequestBody ConfigUpdateRequest request) {
        readWriteConfigService.write(request.getKey(), request.getValue());
        return Result.success("写入成功");
    }

    @PostMapping("/queue/produce")
    public Result<String> produce(@RequestParam String taskName) throws Exception {
        conditionTaskQueueService.produce(taskName);
        return Result.success("任务入队成功");
    }

    @PostMapping("/queue/consume")
    public Result<String> consume() throws Exception {
        return Result.success(conditionTaskQueueService.consume());
    }

    @PostMapping("/price/write")
    public Result<String> updatePrice(@RequestParam Long productId, @RequestParam Integer price) {
        stampedLockPriceService.updatePrice(productId, price);
        return Result.success("价格更新成功");
    }

    @GetMapping("/price/read")
    public Result<Integer> getPrice(@RequestParam Long productId) {
        return Result.success(stampedLockPriceService.getPrice(productId));
    }

    @PostMapping("/counter/increment")
    public Result<Integer> incrementCounter() {
        return Result.success(atomicCounterService.increment());
    }

    @PostMapping("/distributed/order")
    public Result<String> createDistributedOrder(@Valid @RequestBody DistributedOrderRequest request) {
        return Result.success(redissonOrderService.createOrder(request.getUserId(), request.getProductId()));
    }

    @PostMapping("/distributed/fair-grab")
    public Result<String> fairGrab(@RequestParam Long userId) {
        return Result.success(redissonFairLockService.grab(userId));
    }

    @GetMapping("/distributed/config/read")
    public Result<String> distributedRead(@RequestParam String key) {
        return Result.success(redissonReadWriteService.readConfig(key));
    }

    @PostMapping("/distributed/config/write")
    public Result<String> distributedWrite(@Valid @RequestBody ConfigUpdateRequest request) {
        return Result.success(redissonReadWriteService.writeConfig(request.getKey(), request.getValue()));
    }
}