package com.example.slideshow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class WorkRunner {
    /**
     * Customized background thread pool with priority
     * The number of core threads is: cpu core number + 1
     * The maximum number of threads is: cpu core number x 2 + 1
     * Thread idle keep-alive time: 60s
     */
    private static ExecutorService mThreadPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2);

    /**
     *
     * Add a task to run in the background thread queue
     *
     * @param task Tasks to be performed
     */
    public static void addTaskToBackground(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }

        mThreadPool.execute(task);
    }
}

