/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This Executor warrants task ordering for tasks with same id.
 *
 *
 * Generally periodic tasks will be scheduled and run as the same object, meaning that only one of them
 * can run at any given time and the first must finish before the next can run.  Single run tasks will always be
 * different instances if they are created when they are executed, so multiple instances can run at the same time.
 *
 * The optional defaultQueueSize parameter will allow multiple tasks with the same ID to be queued up and run in order.  The default queue size of 0
 * indicates that no tasks can be queued and that tasks submitted while another is running will be rejected with RejectedTaskReason.TASK_QUEUE_FULL
 *
 * The flushFullQueue parameter can be used to force a queue to flush its pending tasks and replace them with the incoming task.  This is useful
 * if pending tasks are deemed out-dated and become irrelevant where by running the most recently scheduled task is preferred to executing all old stale tasks.
 *
 * Note that every queue will be removed once it is empty.
 *
 */
public class OrderedThreadPoolExecutor extends ThreadPoolExecutor implements RejectedExecutionHandler {


    //Task to queue map
    private final Map<String, OrderedTaskQueue> keyedTasks = new ConcurrentHashMap<String, OrderedTaskQueue>();

    private final boolean flushFullQueue;
    private RejectedExecutionHandler handler;
    private TimeSource timer;

    /**
     */
    public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler, TimeSource timer) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.flushFullQueue = false;
        super.setRejectedExecutionHandler(this);
        this.handler = handler;
        this.timer = timer;
    }

    /**
     */
    public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler, TimeSource timer) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory);
        super.setRejectedExecutionHandler(this);
        this.handler = handler;
        this.flushFullQueue = false;
        this.timer = timer;
    }

    /**
     */
    public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, TimeSource timer) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory);
        this.flushFullQueue = false;
        this.timer = timer;
    }

    /**
     *
     * Overloaded constructor to allow tuning the task queues
     *
     */
    public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler,
            boolean flushFullQueue, TimeSource timer) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.flushFullQueue = flushFullQueue;
        super.setRejectedExecutionHandler(this);
        this.handler = handler;
        this.timer = timer;
    }

    /**
     *
     * Overloaded constructor to allow tuning the task queues
     *
     */
    public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            boolean flushFullQueue, TimeSource timer) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory);
        this.flushFullQueue = flushFullQueue;
        this.timer = timer;
    }

    /**
     */
    public OrderedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
            boolean flushFullQueue, TimeSource timer) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.flushFullQueue = false;
        this.timer = timer;
    }

    @Override
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        super.setRejectedExecutionHandler(this);
        this.handler = handler;
    }

    /**
     * Execute a task that may be ordered.  Tasks with null ID are run immediately and
     * potentially in parallel to other tasks of the same type.
     * @param worker is the task to execute
     */
    public void execute(TaskWrapper worker) {

        // No ordering ie, the ID is null
        if (worker.task.id == null) {
            execute((Runnable) worker);
            return;
        }

        AtomicBoolean first = new AtomicBoolean(true);
        AtomicReference<OrderedTaskCollection> wrappedTask = new AtomicReference<>();
        keyedTasks.compute(worker.task.id, (key, dependencyQueue) -> {
            first.set(dependencyQueue == null);
            if (dependencyQueue == null) {
                OrderedTaskInfo info = new OrderedTaskInfo(worker.task);
                if(info.queueSizeLimit == 0) {
                    dependencyQueue = new QueuelessTask(info);
                }else if (flushFullQueue) {
                    dependencyQueue = new TimePriorityLimitedTaskQueue(info);
                } else {
                    dependencyQueue = new LimitedTaskQueue(info);
                }
            }
            wrappedTask.set(wrap(worker, dependencyQueue));

            // Either add or reject
            if (!first.get()) {
                dependencyQueue.add(wrappedTask.get(), this);
            }

            return dependencyQueue;
        });

        // Either add or reject
        if (!first.get()) {
            return;
        }

        // execute and reject methods can block, call them outside synchronize block
        execute(wrappedTask.get());
    }

    /**
     * We need to ensure we remove the keyed tasks if we get rejected
     *
     */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

        if(r instanceof OrderedTaskCollection){
            final OrderedTaskCollection t  = (OrderedTaskCollection)r;
            OrderedTaskQueue depQueue = keyedTasks.compute(t.wrapper.task.id, (k, dependencyQueue) -> {
                //Don't bother trying to run the queue we've got a problem
                if (dependencyQueue.isEmpty())
                    return null;
                return dependencyQueue;
            });

            if(depQueue != null) {
                //We know it isn't empty or it would be null
                OrderedTaskCollection nextTask = depQueue.poll();
                execute(nextTask);
            }
            this.handler.rejectedExecution(t.wrapper, e);

        }else if(r instanceof TaskWrapper){
            TaskWrapper wrapper = (TaskWrapper)r;
            wrapper.task.rejectedAsDelegate(new RejectedTaskReason(RejectedTaskReason.POOL_FULL, wrapper.executionTime, wrapper.task, e));
        }else{
            if(this.handler != null){
                //Pass it along
                this.handler.rejectedExecution(r, e);
            }else{
                //Default Behavior
                throw new RejectedExecutionException("Task " + r.toString() +
                        " rejected from " +
                        e.toString());
            }
        }
    }

    /**
     * Removes the queue from the map and discards it's tasks.
     */
    public void removeTaskQueue(String taskId) {
        keyedTasks.remove(taskId);
    }

    /**
     * Get the task queue for observation
     */
    public OrderedTaskQueue getTaskQueue(String taskId) {
        return keyedTasks.get(taskId);
    }

    /**
     * Test to see if a queue for this task id exists
     */
    public boolean queueExists(String taskId){
        return keyedTasks.containsKey(taskId);
    }

    private OrderedTaskCollection wrap(TaskWrapper task, OrderedTaskQueue dependencyQueue) {
        return new OrderedTaskCollection(task, dependencyQueue);
    }

    /**
     * Class that ensures tasks of the same type are run in order
     *
     * When all tasks are run and removed from dependencyQueue
     * the entire queue is removed from the keyed tasks map
     *
     */
    public class OrderedTaskCollection implements Runnable {

        private final OrderedTaskQueue dependencyQueue;
        private final TaskWrapper wrapper;
        private int rejectedReason;

        public OrderedTaskCollection(TaskWrapper task, OrderedTaskQueue dependencyQueue) {
            this.wrapper = task;
            this.dependencyQueue = dependencyQueue;
        }

        @Override
        public void run() {
            long start = timer.currentTimeMillis();
            try {
                this.wrapper.run();
            } finally {
                Runnable nextTask = null;
                OrderedTaskQueue queue = this.dependencyQueue;
                if(queue.isEmpty()) {
                    queue = keyedTasks.compute(wrapper.task.id, (k, depQueue) -> {
                        //depQueue.lock();
                        if(depQueue.isEmpty())
                            return null; //no need to unlock, it is discarded
                        return depQueue;
                    });

                    if(queue != null) { //must not be empty
                        nextTask = queue.poll();
                    }
                } else {
                    nextTask = queue.poll();
                }

                // Update our task info
                this.dependencyQueue.getInfo().addExecutionTime(timer.currentTimeMillis() - start);
                this.dependencyQueue.getInfo().updateCurrentQueueSize(this.dependencyQueue.size());
                if (nextTask != null) {
                    execute(nextTask);
                }
            }
        }

        public void setRejectedReason(int reason){
            this.rejectedReason = reason;
        }

        public void rejected(Executor e){
            this.dependencyQueue.getInfo().rejections++;
            this.wrapper.task.rejectedAsDelegate(new RejectedTaskReason(this.rejectedReason, wrapper.executionTime, wrapper.task, e));
        }

        @Override
        public String toString(){
            return this.wrapper.task.toString();
        }

        public TaskWrapper getWrapper(){
            return this.wrapper;
        }
    }

    /**
     * Interface for all queued ordered tasks
     */
    public interface OrderedTaskQueue {
        public void add(OrderedTaskCollection c, Executor ex);
        public OrderedTaskCollection poll();
        public boolean isEmpty();
        public int size();
        public OrderedTaskInfo getInfo();
    }
    /**
     * Class to hold a limited size queue and reject incoming tasks
     * if the limit is reached false is returned from the add method.
     *
     * This class ensures that all submitted tasks that are not rejected
     * will run.  Which is different to TimePriorityLimitedTaskQueue where incoming tasks to a full queue
     * cause the queue to be cleared and the incoming task to take precedence.
     *
     * @author Terry Packer
     *
     */
    public class LimitedTaskQueue extends LinkedBlockingDeque<OrderedTaskCollection> implements OrderedTaskQueue {

        private static final long serialVersionUID = 1L;
        protected final OrderedTaskInfo info;

        public LimitedTaskQueue(OrderedTaskInfo info){
            super(info.queueSizeLimit == Task.UNLIMITED_QUEUE_SIZE ? Integer.MAX_VALUE : info.queueSizeLimit);
            this.info = info;
        }

        @Override
        public void add(OrderedTaskCollection c, Executor ex) {
            try{
                super.add(c);
                info.updateCurrentQueueSize(this.size());
            }catch(IllegalStateException e) {
                if(info.queueSizeLimit > 0){
                    c.setRejectedReason(RejectedTaskReason.TASK_QUEUE_FULL);
                }else{
                    c.setRejectedReason(RejectedTaskReason.CURRENTLY_RUNNING);
                }
                //Execute rejection in calling thread
                c.rejected(ex);
            }
        }

        @Override
        public OrderedTaskInfo getInfo() {
            return info;
        }
    }

    /**
     * Task with size 0 queue
     * @author Terry Packer
     *
     */
    public class QueuelessTask implements OrderedTaskQueue {

        private final OrderedTaskInfo info;

        public QueuelessTask(OrderedTaskInfo info) {
            this.info = info;
        }

        @Override
        public void add(OrderedTaskCollection c, Executor ex) {
            c.setRejectedReason(RejectedTaskReason.CURRENTLY_RUNNING);
            //Execute rejection in calling thread
            c.rejected(ex);
        }
        @Override
        public OrderedTaskCollection poll() {
            return null;
        }
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public OrderedTaskInfo getInfo() {
            return info;
        }
    }

    /**
     * Class to hold a limited size queue and replace entire queue with incoming task if the queue is full.
     *
     * This class gives priority to a new task when the queue is full and is ideally used with a size 1 queue
     * so that the most recent task will execute next.
     *
     * @author Terry Packer
     *
     */
    public class TimePriorityLimitedTaskQueue extends LimitedTaskQueue{

        private static final long serialVersionUID = 1L;

        public TimePriorityLimitedTaskQueue(OrderedTaskInfo info){
            super(info);
        }

        @Override
        public void add(OrderedTaskCollection c, Executor ex) {
            try {
                super.add(c);
                info.updateCurrentQueueSize(this.size());
            }catch(IllegalStateException e) {
                while(this.size() >= info.queueSizeLimit) {
                    OrderedTaskCollection t = this.poll();
                    if(info.queueSizeLimit > 0){
                        t.setRejectedReason(RejectedTaskReason.TASK_QUEUE_FULL);
                        //Execute rejection in thread pool
                        execute(() -> {
                            t.rejected(ex);
                        });
                    }else{
                        t.setRejectedReason(RejectedTaskReason.CURRENTLY_RUNNING);
                        //Execute rejection in thread pool
                        execute(() -> {
                            t.rejected(ex);
                        });
                    }
                }
                try {
                    super.add(c);
                    info.updateCurrentQueueSize(this.size());
                }catch(IllegalStateException e1) {

                }
            }
        }
    }

    /**
     * Get information on all tasks running in the ordered queue
     *
     */
    public List<OrderedTaskInfo> getOrderedQueueInfo() {
        List<OrderedTaskInfo> stats = new ArrayList<OrderedTaskInfo>(keyedTasks.size());
        Iterator<OrderedTaskQueue> iter = keyedTasks.values().iterator();
        while(iter.hasNext())
            stats.add(iter.next().getInfo());
        return stats;
    }

}
