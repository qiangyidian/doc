import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class RateLimiterDemo {
    // 创建一个信号量，代表只有 5 个许可证（允许 5 个并发）
    // true 表示公平模式（先等待的先获取许可证）
    private final Semaphore semaphore = new Semaphore(5, true);

    public void accessExternalService() {
        try {
            // 1. 请求一个许可证，如果没有可用的，当前线程将被阻塞
            semaphore.acquire();
            System.out.println(Thread.currentThread().getName() + " 获取到了资源，开始处理...");
            
            // 模拟业务处理耗时
            Thread.sleep(2000); 
            
            System.out.println(Thread.currentThread().getName() + " 处理完毕，释放资源。");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 2. 无论是否发生异常，务必释放许可证
            semaphore.release();
        }
    }

    public static void main(String[] args) {
        RateLimiterDemo demo = new RateLimiterDemo();
        // 模拟 10 个客户端并发请求
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            threadPool.execute(demo::accessExternalService);
        }
        threadPool.shutdown();
    }
}
