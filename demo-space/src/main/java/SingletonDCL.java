public class SingletonDCL {
    // 关键点 1：必须加上 volatile。
    // 原因：instance = new SingletonDCL() 在 JVM 中分 3 步：
    // 1. 分配内存空间 2. 初始化对象 3. 将 instance 指向分配的内存。
    // 如果没有 volatile，指令可能会重排为 1 -> 3 -> 2。
    // 此时另一个线程进来判断 instance != null，直接返回使用，但对象还没初始化完，会报 NullPointerException。
    private static volatile SingletonDCL instance;

    // 私有化构造器，防止外部 new
    private SingletonDCL() {
        System.out.println("单例被初始化");
    }

    public static SingletonDCL getInstance() {
        // 第一重检查：如果不为空，直接返回，避免所有线程都去竞争锁，提高性能
        if (instance == null) {
            // 只在第一次初始化时加锁
            synchronized (SingletonDCL.class) {
                // 第二重检查：拿到锁后再次检查。
                // 因为可能多个线程同时过了第一重检查，排队等待锁。
                // 第一个线程拿到锁初始化后释放，第二个线程拿到锁如果不做检查，就会重复初始化。
                if (instance == null) {
                    instance = new SingletonDCL();
                }
            }
        }
        return instance;
    }
}