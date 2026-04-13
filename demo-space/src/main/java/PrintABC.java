import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PrintABC {
    private final ReentrantLock lock = new ReentrantLock();
    // 定义三个 Condition，分别对应 A、B、C 三个线程的阻塞和唤醒
    private final Condition conditionA = lock.newCondition();
    private final Condition conditionB = lock.newCondition();
    private final Condition conditionC = lock.newCondition();
    
    // 状态标志位，控制当前该谁打印：1->A, 2->B, 3->C
    private int state = 1;

    public void printA() {
        lock.lock();
        try {
            for (int i = 0; i < 10; i++) {
                // 1. 如果 state 不是 1，说明没轮到 A，A 线程挂起等待
                while (state != 1) {
                    conditionA.await();
                }
                // 2. 轮到 A 打印
                System.out.print("A");
                // 3. 状态流转给 B
                state = 2;
                // 4. 精确唤醒 B 线程
                conditionB.signal();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void printB() {
        lock.lock();
        try {
            for (int i = 0; i < 10; i++) {
                while (state != 2) {
                    conditionB.await();
                }
                System.out.print("B");
                state = 3;
                // 精确唤醒 C 线程
                conditionC.signal();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void printC() {
        lock.lock();
        try {
            for (int i = 0; i < 10; i++) {
                while (state != 3) {
                    conditionC.await();
                }
                System.out.println("C"); // 打印并换行，便于观察结果
                state = 1;
                // 精确唤醒 A 线程
                conditionA.signal();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        PrintABC printer = new PrintABC();
        // 启动三个线程，启动顺序无关紧要，依靠 state 保证打印顺序
        new Thread(printer::printC, "Thread-C").start();
        new Thread(printer::printB, "Thread-B").start();
        new Thread(printer::printA, "Thread-A").start();
    }
}
