/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.timer;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Terry Packer
 *
 */
public abstract class Task {

	public static final int UNLIMITED_QUEUE_SIZE = -1;

	/**
     * This object is used to control access to the TimerTask internals.
     */
    Object lock = new Object();

    /**
     * The state of this task, chosen from the constants below.
     */
    volatile int state = VIRGIN;

    /**
     * This task has not yet been scheduled.
     */
    static final int VIRGIN = 0;

    /**
     * This task is scheduled for execution. If it is a non-repeating task, it has not yet been executed.
     */
    static final int SCHEDULED = 1;

    /**
     * This non-repeating task has already executed (or is currently executing) and has not been cancelled.
     */
    static final int EXECUTED = 2;

    /**
     * This task has been cancelled (with a call to TimerTask.cancel).
     */
    static final int CANCELLED = 3;

    /**
     * Short description of what the task is doing,
     * this will be used as the thread's name while it
     * is executing.
     */
    final String name;

    /**
     * Unique ID for task if it is to be ordered behind
     * other tasks with the same ID.  Leave null
     * if ordering is not desired.
     */
    final String id;

    /**
     * Queue Size for Ordered Tasks
     */
    final int queueSize;

    /**
     * Indicates that if the task is running at the moment it is cancelled, the cancellation should wait until the task
     * is done. This is useful if the task uses resources that need to be shut down before the timer is shutdown.
     */
    private boolean completeBeforeCancel;

    private final ReadWriteLock cancelLock = new ReentrantReadWriteLock();

    /**
     * Create a non-ordered Task
     * @param name
     */
    public Task(String name){
    	this.name = name;
    	this.id = null;
    	this.queueSize = 0;
    }

    /**
     * Ordred Task that is queuable
     * @param name
     * @param id
     * @param queueSize
     */
    public Task(String name, String id, int queueSize){
    	this.name = name;
    	this.id = id;
    	this.queueSize = queueSize;
    }



    public boolean isCompleteBeforeCancel() {
        return completeBeforeCancel;
    }

    public void setCompleteBeforeCancel(boolean completeBeforeCancel) {
        this.completeBeforeCancel = completeBeforeCancel;
    }

    /**
     * Cancels this timer task. If the task has been scheduled for one-time execution and has not yet run, or has not
     * yet been scheduled, it will never run. If the task has been scheduled for repeated execution, it will never run
     * again. (If the task is running when this call occurs, the task will run to completion, but will never run again.)
     *
     * <p>
     * Note that calling this method from within the <tt>run</tt> method of a repeating timer task absolutely guarantees
     * that the timer task will not run again.
     *
     * <p>
     * This method may be called repeatedly; the second and subsequent calls have no effect.
     *
     * @return true if this task is scheduled for one-time execution and has not yet run, or this task is scheduled for
     *         repeated execution. Returns false if the task was scheduled for one-time execution and has already run,
     *         or if the task was never scheduled, or if the task was already cancelled. (Loosely speaking, this method
     *         returns <tt>true</tt> if it prevents one or more scheduled executions from taking place.)
     */
    public boolean cancel() {
        synchronized (lock) {
            boolean result = (state == SCHEDULED);

            if (completeBeforeCancel) {
                cancelLock.writeLock().lock();
                try {
                    state = CANCELLED;
                }
                finally {
                    cancelLock.writeLock().unlock();
                }
            }
            else
                state = CANCELLED;

            return result;
        }
    }

    /**
     * Perform the task
     * @param runtime
     */
    abstract public void run(long runtime);


    final public void runTask(long runtime) {
    	//System.out.println("Task: " + this.hashCode() + " scheduled: " + runtime + " now: " + System.currentTimeMillis() + " Running");

        String originalName = null;
        try {
            if (!StringUtils.isBlank(name)) {
                // This uses roughly the same code as in NamedRunnable to rename
                // the thread for the duration of the task execution.
                originalName = Thread.currentThread().getName();

                // Append the given name to the original name.
                Thread.currentThread().setName(originalName + " --> " + name);
            }

            if (completeBeforeCancel) {
                cancelLock.readLock().lock();
                try {
                    if (state != CANCELLED)
                        run(runtime);
                }
                finally {
                    cancelLock.readLock().unlock();
                }
            }
            else
                // Ok, go ahead and run the thingy.
                run(runtime);
        }
        finally {
            if (originalName != null)
                // Return the name to its original.
                Thread.currentThread().setName(originalName);
        }
    }

    public boolean isCancelled() {
        return state == CANCELLED;
    }

    /**
     * Called if task is rejected from Timer Thread's Executor service
     */
    public abstract void rejected(RejectedTaskReason reason);

    /**
     * Get a short description of what the task does
     * @return
     */
    public String getName(){
    	return this.name;
    }

    /**
     * Get the unique ID for the task, used for tracking etc.
     * If this is null, the task cannot be queued
     * @return
     */
    public String getId(){
    	return this.id;
    }

    /**
     * Get the Queue Size
     * @return
     */
    public int getQueueSize(){
    	return this.queueSize;
    }

}
