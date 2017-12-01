/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.timer.test;

import static org.junit.Assert.fail;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.serotonin.m2m2.rt.maint.MangoThreadFactory;
import com.serotonin.timer.OrderedThreadPoolExecutor;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.SystemTimeSource;
import com.serotonin.timer.Task;
import com.serotonin.timer.TaskWrapper;

/**
 *
 * @author Terry Packer
 */
public class OrderedThreadPoolExecutorTest {
    
    @Test
    public void testFailedExecutions() throws InterruptedException {
        
        boolean flushOnReject = false;
        
        OrderedThreadPoolExecutor exe = new OrderedThreadPoolExecutor(
                0,
                3,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new MangoThreadFactory("medium", Thread.MAX_PRIORITY - 2),
                new RejectedExecutionHandler() {
                    
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                       System.out.println("Rejected.");
                    }
                },
                flushOnReject,
                new SystemTimeSource());
        
        //Starup a new thread that inserts failing tasks
        
        new Thread() {
            public void run() {
                long time = 10000;
                for(int i=0; i<10; i++) {
                    Task task = new Task("Failure", "TSK_FAIL", -1) {

                        @Override
                        public void run(long runtime) {
                            try { Thread.sleep(20); } catch (InterruptedException e) { }
                            throw new RuntimeException("oops");
                        }

                        @Override
                        public void rejected(RejectedTaskReason reason) {
                           System.out.println("Task Rejected");
                        }
                        
                    };
                    exe.execute(new TaskWrapper(task, time));
                    time += 100;
                }
            };
        }.start();
        
        
        Thread.sleep(1000);
        if(exe.queueExists("TSK_FAIL"))
            fail("non empty queue");
    }
    
}
