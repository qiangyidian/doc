import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportAggregator {
    public static void main(String[] args) throws InterruptedException {
        // 设置计数器为 3，代表有 3 个子任务
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        System.out.println("主线程：开始分配拉取数据的子任务...");

        // 任务 1：拉取用户数据
        executor.execute(() -> {
            try {
                System.out.println("子线程 1：开始拉取用户数据...");
                Thread.sleep(1500); // 模拟网络耗时
                System.out.println("子线程 1：用户数据拉取完毕");
            } catch (InterruptedException e) { e.printStackTrace(); } 
            finally {
                latch.countDown(); // 关键操作：任务完成，计数器减 1
            }
        });

        // 任务 2：拉取订单数据
        executor.execute(() -> {
            try {
                System.out.println("子线程 2：开始拉取订单数据...");
                Thread.sleep(2000);
                System.out.println("子线程 2：订单数据拉取完毕");
            } catch (InterruptedException e) { e.printStackTrace(); } 
            finally {
                latch.countDown();
            }
        });

        // 任务 3：拉取库存数据
        executor.execute(() -> {
            try {
                System.out.println("子线程 3：开始拉取库存数据...");
                Thread.sleep(1000);
                System.out.println("子线程 3：库存数据拉取完毕");
            } catch (InterruptedException e) { e.printStackTrace(); } 
            finally {
                latch.countDown();
            }
        });

        // 主线程阻塞等待，直到 latch 的 count 减到 0
        System.out.println("主线程：等待所有子任务完成...");
        latch.await(); 
        
        System.out.println("主线程：所有数据拉取完毕，开始合并生成最终报表！");
        executor.shutdown();
    }
}
