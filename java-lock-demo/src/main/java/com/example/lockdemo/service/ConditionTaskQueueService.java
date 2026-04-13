package com.example.lockdemo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ConditionTaskQueueService {

    private final Deque<String> taskQueue = new ArrayDeque<>();

    // 用一把显式锁保护队列。
    private final ReentrantLock lock = new ReentrantLock();

    // 队列为空时，消费者在这里等待。
    private final Condition notEmpty = lock.newCondition();

    // 队列已满时，生产者在这里等待。
    private final Condition notFull = lock.newCondition();

    private final int maxSize = 5;

    public void produce(String taskName) throws InterruptedException {
        lock.lock();
        try {
            while (taskQueue.size() >= maxSize) {
                // 队列满了，生产者进入等待。
                notFull.await();
            }

            taskQueue.addLast(taskName);

            // 新增任务后，唤醒一个等待中的消费者。
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public String consume() throws InterruptedException {
        lock.lock();
        try {
            while (taskQueue.isEmpty()) {
                // 队列为空，消费者进入等待。
                notEmpty.await();
            }

            String task = taskQueue.removeFirst();

            // 消费掉一个任务后，唤醒一个等待中的生产者。
            notFull.signal();
            return task;
        } finally {
            lock.unlock();
        }
    }
}