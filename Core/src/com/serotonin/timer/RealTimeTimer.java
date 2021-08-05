package com.serotonin.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealTimeTimer extends AbstractTimer {
    protected static final Logger LOG = LoggerFactory.getLogger(RealTimeTimer.class);

    /**
     * The timer task queue. This data structure is shared with the timer thread. The timer produces tasks, via its
     * various schedule calls, and the timer thread consumes, executing timer tasks as appropriate, and removing them
     * from the queue when they're obsolete.
     */
    protected final TaskQueue queue = new TaskQueue();

    /**
     * The timer thread.
     */
    protected TimerThread thread;

    // Do i own the executor?
    private boolean ownsExecutor;
    private Exception cancelStack;

    protected TimeSource timeSource = new SystemTimeSource();

    public void setTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    @Override
    public void init() {
        ownsExecutor = true;
        init(new ThreadPoolExecutor(0, 1000, 30L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()));
    }

    @Override
    public void init(ExecutorService executorService) {
        thread = new TimerThread(queue, executorService, timeSource);
        thread.setName("Serotonin Timer");
        thread.setDaemon(false);
        thread.start();
    }
    
    @Override
    public void init(TimerThread timer){
    	this.thread = timer;
    	this.thread.start();
    }
    
    @Override
    public boolean isInitialized() {
        return thread != null;
    }

    /**
     * This object causes the timer's task execution thread to exit gracefully when there are no live references to the
     * Timer object and no tasks in the timer queue. It is used in preference to a finalizer on Timer as such a
     * finalizer would be susceptible to a subclass's finalizer forgetting to call it.
     */
    @Override
    protected void finalize() {
        synchronized (queue) {
            if (thread != null)
                thread.newTasksMayBeScheduled = false;
            if (cancelStack == null)
                cancelStack = new Exception();
            queue.notify();
        }
    }

    @Override
    public void execute(Task command) {
        if (thread == null)
            throw new IllegalStateException("Run init first");
        thread.execute(new TaskWrapper(command, this.currentTimeMillis()));
    }

    /**
     * Schedule the specified timer task for execution at the specified time with the specified period, in milliseconds.
     * If period is positive, the task is scheduled for repeated execution; if period is zero, the task is scheduled for
     * one-time execution. Time is specified in Date.getTime() format. This method checks timer state, task state, and
     * initial execution time, but not period.
     * 
     * @throws IllegalArgumentException
     *             if <tt>time()</tt> is negative.
     * @throws IllegalStateException
     *             if task was already scheduled or cancelled, timer was cancelled, or timer thread terminated.
     */
    @Override
    protected void scheduleImpl(TimerTask task) {
        if (thread == null)
            throw new IllegalStateException("Run init first");

        if (task.state == TimerTask.CANCELLED || task.state == TimerTask.EXECUTED)
            throw new IllegalStateException("Task already executed or cancelled");

        synchronized (queue) {
            if (!thread.newTasksMayBeScheduled) {
                if (cancelStack != null) {
                    LOG.error("Timer already cancelled.");
                    LOG.error("   Cancel stack:", cancelStack);
                    LOG.error("   Current stack:", new Exception());
                    throw new IllegalStateException("Timer already cancelled.", cancelStack);
                }
                throw new IllegalStateException("Timer already cancelled.");
            }

            synchronized (task.lock) {
                if (task.state == TimerTask.VIRGIN) {
                    long time = task.trigger.getFirstExecutionTime();

                    if (time < 0)
                        throw new IllegalArgumentException("Illegal execution time.");

                    task.trigger.nextExecutionTime = time;
                    task.state = TimerTask.SCHEDULED;
                }
            }

            queue.add(task);
            if (queue.getMin() == task)
                queue.notify();
        }
    }

    /**
     * Terminates this timer, discarding any currently scheduled tasks. Does not interfere with a currently executing
     * task (if it exists). Once a timer has been terminated, its execution thread terminates gracefully, and no more
     * tasks may be scheduled on it.
     * 
     * <p>
     * Note that calling this method from within the run method of a timer task that was invoked by this timer
     * absolutely guarantees that the ongoing task execution is the last task execution that will ever be performed by
     * this timer.
     * 
     * <p>
     * This method may be called repeatedly; the second and subsequent calls have no effect.
     */
    @Override
    public List<TimerTask> cancel() {
        List<TimerTask> tasks;
        synchronized (queue) {
            thread.newTasksMayBeScheduled = false;
            if (cancelStack == null)
                cancelStack = new Exception();
            tasks = getTasks();
            queue.clear();
            queue.notify(); // In case queue was already empty.
        }

        if (ownsExecutor)
            getExecutorService().shutdown();

        return tasks;
    }

    public ExecutorService getExecutorService() {
        return thread.getExecutorService();
    }

    /**
     * Removes all canceled tasks from this timer's task queue. <i>Calling this method has no effect on the behavior of
     * the timer</i>, but eliminates the references to the canceled tasks from the queue. If there are no external
     * references to these tasks, they become eligible for garbage collection.
     * 
     * <p>
     * Most programs will have no need to call this method. It is designed for use by the rare application that cancels
     * a large number of tasks. Calling this method trades time for space: the runtime of the method may be proportional
     * to n + c log n, where n is the number of tasks in the queue and c is the number of canceled tasks.
     * 
     * <p>
     * Note that it is permissible to call this method from within a a task scheduled on this timer.
     * 
     * @return the number of tasks removed from the queue.
     * @since 1.5
     */
    @Override
    public int purge() {
        int result = 0;

        synchronized (queue) {
            for (int i = queue.size(); i > 0; i--) {
                if (queue.get(i).state == TimerTask.CANCELLED) {
                    queue.quickRemove(i);
                    result++;
                }
            }

            if (result != 0)
                queue.heapify();
        }

        return result;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public List<TimerTask> getTasks() {
        List<TimerTask> result = new ArrayList<TimerTask>();
        synchronized (queue) {
            for (int i = 0; i < queue.size(); i++)
                result.add(queue.get(i + 1));
        }
        return result;
    }

    @Override
    public long currentTimeMillis() {
        return timeSource.currentTimeMillis();
    }
   
    @Override
    public TimeSource getTimeSource() {
        return timeSource;
    }
    
}
