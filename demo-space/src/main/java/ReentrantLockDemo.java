import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {
    // true 为公平锁（按排队顺序），false 为非公平锁（性能更高，默认值）
    private final ReentrantLock lock = new ReentrantLock(true);

    public void doWork() {
        try {
            // 1. 尝试获取锁，支持超时限制
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                try {
                    System.out.println(Thread.currentThread().getName() + " 成功获得锁，开始工作...");
                    Thread.sleep(1000);
                } finally {
                    // 2. 必须在 finally 中释放锁，防止异常导致死锁
                    lock.unlock();
                    System.out.println(Thread.currentThread().getName() + " 释放了锁");
                }
            } else {
                System.out.println(Thread.currentThread().getName() + " 未能在3秒内获得锁，放弃");
            }
        } catch (InterruptedException e) {
            System.err.println(Thread.currentThread().getName() + " 被中断了");
        }
    }
}