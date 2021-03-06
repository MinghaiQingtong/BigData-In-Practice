package com.whirly.recipes.locks;

import com.whirly.recipes.ZKUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.utils.CloseableUtils;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @program: curator-example
 * @description: 与 Shared Reentrant Lock  相似，但是不可重入
 * @author: 赖键锋
 * @create: 2019-01-24 16:05
 **/
public class SharedLockTest {
    private static final String lockPath = "/testZK/sharedlock";
    private static final Integer clientNums = 5;
    final static FakeLimitedResource resource = new FakeLimitedResource(); // 共享的资源
    private static CountDownLatch countDownLatch = new CountDownLatch(clientNums);

    /**
     * 请求锁，使用资源，释放锁
     */
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < clientNums; i++) {
            String clientName = "client#" + i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CuratorFramework client = ZKUtils.getClient();
                    client.start();
                    Random random = new Random();
                    try {
                        final InterProcessSemaphoreMutex lock = new InterProcessSemaphoreMutex(client, lockPath);
                        // 每个客户端请求10次共享资源
                        for (int j = 0; j < 10; j++) {
                            if (!lock.acquire(10, TimeUnit.SECONDS)) {
                                throw new IllegalStateException(j + ". " + clientName + " 不能得到互斥锁");
                            }
                            try {
                                System.out.println(j + ". " + clientName + " 已获取到互斥锁");
                                resource.use(); // 使用资源
//                                if (!lock.acquire(10, TimeUnit.SECONDS)) {
//                                    throw new IllegalStateException(j + ". " + clientName + " 不能再次得到互斥锁");
//                                }
//                                System.out.println(j + ". " + clientName + " 已再次获取到互斥锁");
//                                lock.release(); // 申请几次锁就要释放几次锁
                            } finally {
                                System.out.println(j + ". " + clientName + " 释放互斥锁");
                                lock.release(); // 总是在finally中释放
                            }
                            Thread.sleep(random.nextInt(100));
                        }
                    } catch (Throwable e) {
                        System.out.println(e.getMessage());
                    } finally {
                        CloseableUtils.closeQuietly(client);
                        System.out.println(clientName + " 客户端关闭！");
                        countDownLatch.countDown();
                    }
                }
            }).start();
        }
        countDownLatch.await();
        System.out.println("结束！");
    }
}
