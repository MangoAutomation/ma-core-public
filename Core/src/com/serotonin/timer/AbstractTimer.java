package com.serotonin.timer;

import java.util.List;
import java.util.concurrent.ExecutorService;

abstract public class AbstractTimer {
    abstract public boolean isInitialized();

    abstract public long currentTimeMillis();

    /**
     * Execute a task with optional ordering if the Timer implementation supports this.
     */
    abstract public void execute(Task command);

    /**
     * Schedule a task to run on this timer
     */
    final public TimerTask schedule(TimerTask task) {
        if (task.getTimer() == this)
            throw new IllegalStateException("Task already scheduled or cancelled");

        task.setTimer(this);
        scheduleImpl(task);

        return task;
    }

    public void scheduleAll(AbstractTimer that) {
        for (TimerTask task : that.cancel())
            schedule(task);
    }

    abstract protected void scheduleImpl(TimerTask task);

    abstract public List<TimerTask> cancel();

    abstract public int purge();

    abstract public int size();

    abstract public List<TimerTask> getTasks();
    
    abstract public void init();

    abstract public void init(ExecutorService executorService);

    abstract public void init(TimerThread timer);

    abstract public ExecutorService getExecutorService();
    
    abstract public TimeSource getTimeSource();
}
