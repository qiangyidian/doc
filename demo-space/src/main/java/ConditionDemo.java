import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 使用 ReentrantLock 和 Condition 实现的自定义阻塞队列
 */
class MyBlockingQueue<T> {
    private final LinkedList<T> buffer = new LinkedList<>();
    private final int capacity;
    
    // 1. 定义锁
    private final Lock lock = new ReentrantLock();
    
    // 2. 定义两个条件变量：分别对应“不满”和“不空”的状态
    // notFull 队列存放的是：因为队列满了而阻塞的【生产者】
    private final Condition notFull = lock.newCondition();
    // notEmpty 队列存放的是：因为队列空了而阻塞的【消费者】
    private final Condition notEmpty = lock.newCondition();

    public MyBlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    /**
     * 生产者方法
     */
    public void put(T item) throws InterruptedException {
        lock.lock(); // 获取锁，保证线程安全
        try {
            // 【讲解】：这里必须使用 while 而不是 if，防止“虚假唤醒”
            // 如果队列已满，生产者线程进入 notFull 条件队列挂起
            while (buffer.size() == capacity) {
                System.out.println("队列满，生产者等待...");
                notFull.await(); 
            }

            // 执行实际的入队操作
            buffer.add(item);
            System.out.println("生产了: " + item + "，当前库存: " + buffer.size());

            // 【精准唤醒】：生产完后，队列一定不为空。
            // 因此，只需唤醒那些因为“队列空”而等待的【消费者】
            // 此时不会惊动其他在 notFull 中等待的【生产者】，提高效率
            notEmpty.signal(); 
            
        } finally {
            lock.unlock(); // 必须在 finally 中释放锁
        }
    }

    /**
     * 消费者方法
     */
    public T take() throws InterruptedException {
        lock.lock();
        try {
            // 如果队列为空，消费者线程进入 notEmpty 条件队列挂起
            while (buffer.isEmpty()) {
                System.out.println("队列空，消费者等待...");
                notEmpty.await();
            }

            // 执行实际的出队操作
            T item = buffer.removeFirst();
            System.out.println("消费了: " + item + "，当前库存: " + buffer.size());

            // 【精准唤醒】：消费完后，队列一定不为满。
            // 因此，只需唤醒那些因为“队列满”而等待的【生产者】
            // 此时不会唤醒其他在 notEmpty 中等待的【消费者】
            notFull.signal();
            
            return item;
        } finally {
            lock.unlock();
        }
    }
}

/**
 * 测试运行类
 */
public class ConditionDemo {
    public static void main(String[] args) {
        MyBlockingQueue<Integer> queue = new MyBlockingQueue<>(5);

        // 创建生产者线程
        new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    queue.put(i);
                    Thread.sleep(100); // 模拟生产耗时
                }
            } catch (InterruptedException e) { e.printStackTrace(); }
        }, "Producer").start();

        // 创建消费者线程
        new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    queue.take();
                    Thread.sleep(500); // 模拟消费较慢，会触发队列满
                }
            } catch (InterruptedException e) { e.printStackTrace(); }
        }, "Consumer").start();
    }
}