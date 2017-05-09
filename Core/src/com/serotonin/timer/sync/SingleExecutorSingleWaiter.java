/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.timer.sync;

import com.serotonin.timer.AbstractTimer;

/**
 * This class is useful when an exclusive, long-running task gets called more often than is practical. For example, a
 * large object needs to be persisted, but it changes fairly often. This class will execute the submitted runnable
 * if there is currently nothing running. If another task is already running, the new task will wait. If there is
 * already a task waiting, the new task will replace the waiting task. So, tasks that are started are always allowed to
 * complete, but tasks that are no necessary to run are discarded.
 * 
 * @author Matthew Lohbihler
 */
public class SingleExecutorSingleWaiter {
    final AbstractTimer timer;
    final Object lock = new Object();

    Runnable executing;
    Runnable waiting;

    public SingleExecutorSingleWaiter(AbstractTimer timer) {
        this.timer = timer;
    }

    public void execute(Runnable task) {
        synchronized (lock) {
            if (executing != null) {
                waiting = task;
                return;
            }

            executing = task;
            executeImpl();
        }
    }

    void executeImpl() {
        timer.execute(new TaskWrapper(executing));
    }

    class TaskWrapper implements Runnable {
        private final Runnable command;

        public TaskWrapper(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                command.run();
            }
            finally {
                synchronized (lock) {
                    if (waiting != null) {
                        executing = waiting;
                        waiting = null;
                        executeImpl();
                    }
                    else
                        executing = null;
                }
            }
        }
    }
    //
    //    public static void main(String[] args) throws Exception {
    //        ExecutorService executorService = Executors.newCachedThreadPool();
    //        RealTimeTimer timer = new RealTimeTimer();
    //        timer.init(executorService);
    //
    //        SingleExecutorSingleWaiter e = new SingleExecutorSingleWaiter(timer);
    //        TestRunnable r1 = new TestRunnable(1);
    //        TestRunnable r2 = new TestRunnable(2);
    //        TestRunnable r3 = new TestRunnable(3);
    //        TestRunnable r4 = new TestRunnable(4);
    //        TestRunnable r5 = new TestRunnable(5);
    //        TestRunnable r6 = new TestRunnable(6);
    //        TestRunnable r7 = new TestRunnable(7);
    //        TestRunnable r8 = new TestRunnable(8);
    //        TestRunnable r9 = new TestRunnable(9);
    //
    //        e.execute(r1); // 1
    //        e.execute(r2); // 2
    //        e.execute(r3); // 3
    //        e.execute(r4); // 3
    //
    //        Thread.sleep(1000);
    //        e.execute(r5); // 3
    //
    //        Thread.sleep(1500);
    //        e.execute(r6); // 2
    //
    //        Thread.sleep(4000);
    //        e.execute(r7); // 1
    //        e.execute(r8); // 2
    //        e.execute(r9); // 3
    //
    //        //        Running 1
    //        //        Finished 1
    //        //        Running 5
    //        //        Finished 5
    //        //        Running 6
    //        //        Finished 6
    //        //        Running 7
    //        //        Finished 7
    //        //        Running 9
    //        //        Finished 9
    //
    //        Thread.sleep(4000);
    //        timer.cancel();
    //        executorService.shutdown();
    //    }
    //
    //    static class TestRunnable implements Runnable {
    //        int id;
    //
    //        public TestRunnable(int id) {
    //            this.id = id;
    //        }
    //
    //        @Override
    //        public void run() {
    //            System.out.println("Running " + id);
    //            try {
    //                Thread.sleep(2000);
    //            }
    //            catch (InterruptedException e) {
    //            }
    //            System.out.println("Finished " + id);
    //        }
    //    }
}
