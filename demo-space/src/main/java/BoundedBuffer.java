import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedBuffer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 5;
    private final Lock lock = new ReentrantLock();
    // 定义两个条件：队列满和队列空
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public void produce(int value) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                System.out.println("队列满，生产者等待...");
                notFull.await(); // 生产者进入等待状态
            }
            queue.add(value);
            System.out.println("生产: " + value);
            notEmpty.signal(); // 唤醒消费者
        } finally {
            lock.unlock();
        }
    }

    public void consume() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                System.out.println("队列空，消费者等待...");
                notEmpty.await(); // 消费者进入等待状态
            }
            int value = queue.poll();
            System.out.println("消费: " + value);
            notFull.signal(); // 唤醒生产者
        } finally {
            lock.unlock();
        }
    }
}