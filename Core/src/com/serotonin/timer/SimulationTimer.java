package com.serotonin.timer;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * The simulation timer is optionally a single threaded timer under the temporal control of the next and fastForward methods. 
 * 
 * In single thread mode tasks are run in the same thread as the timer, so they will seem to complete instantly. 
 * 
 * In async mode running the tasks in an executor has the opposite effect of making them appear to take an awful long time to complete.
 * 
 * @author Matthew Lohbihler
 */
public class SimulationTimer extends AbstractTimer {
    private final List<TimerTask> queue = new ArrayList<>();
    private boolean cancelled;
    private OrderedThreadPoolExecutor executorService;
    private final SimulationTimeSource timeSource = new SimulationTimeSource();
    private final boolean async;
    private boolean ownsExecutor;
    
    /**
     * Create 
     */
    public SimulationTimer() {
        this(false);
    }

    /**
     * Optionally submit tasks to the ordered thread pool executor
     */
    public SimulationTimer(boolean async) {
        super();
        this.async = async;
    }

    public SimulationTimer(ZoneId zone) {
        this(zone, false);
    }

    public SimulationTimer(ZoneId zone, boolean async) {
        super(zone);
        this.async = async;
    }

    @Override
    public SimulationTimer withZone(ZoneId zone) {
        if (zone.equals(this.zone)) {
            return this;
        }
        return new SimulationTimer(zone, async);
    }
    
    @Override
    public boolean isInitialized() {
        return true;
    }

    public void setStartTime(long startTime) {
        timeSource.setTime(startTime);
    }

    public void next() {
        fastForwardTo(timeSource.currentTimeMillis() + 1);
    }

    /**
     * @param duration duration to increase the clock by (relative to clock's current instant)
     * @return the current instant (same as {@link #instant() would return}
     */
    public Instant fastForward(Duration duration) {
        return fastForwardTo(instant().plus(duration));
    }

    /**
     * @param time the new instant to set the clock to
     * @return the current instant (same as {@link #instant() would return}
     */
    public Instant fastForwardTo(Instant time) {
        fastForwardTo(time.toEpochMilli());
        return time;
    }

    public void fastForwardTo(long time) {
        while (!queue.isEmpty() && queue.get(0).trigger.nextExecutionTime <= time) {
            TimerTask task = queue.get(0);

            timeSource.setTime(task.trigger.nextExecutionTime);

            if (task.state == TimerTask.CANCELLED) {
                synchronized(this) {
                    queue.remove(0);
                }
            }else {
                long next = task.trigger.calculateNextExecutionTime();
                if (next <= 0) { // Non-repeating, remove
                    synchronized(this) {
                        queue.remove(0);
                    }
                    task.state = TimerTask.EXECUTED;
                }
                else {
                    // Repeating task, reschedule
                    task.trigger.nextExecutionTime = next;
                    synchronized(this) {
                        updateQueue();
                    }
                }
                if(async) {
                    TaskWrapper wrapper = new TaskWrapper(task, task.trigger.mostRecentExecutionTime());
                    try{
                        this.executorService.execute(wrapper);
                    }catch (RejectedExecutionException e) {
                        this.taskRejected(timeSource.currentTimeMillis(), task, e);
                    }
                }else
                    task.runTask(task.trigger.mostRecentExecutionTime());
                
            }
        }

        timeSource.setTime(time);
    }


    @Override
    protected void scheduleImpl(TimerTask task) {
        if (cancelled)
            throw new IllegalStateException("Timer already cancelled.");

        if (task.state == TimerTask.CANCELLED || task.state == TimerTask.EXECUTED)
            throw new IllegalStateException("Task already executed or cancelled");

        if (task.state == TimerTask.VIRGIN) {
            long time = task.trigger.getFirstExecutionTime();
            if (time < 0)
                throw new IllegalArgumentException("Illegal execution time.");

            task.trigger.nextExecutionTime = time;
            task.state = TimerTask.SCHEDULED;
        }
        synchronized(this) {
            queue.add(task);
            updateQueue();
        }
    }

    private void updateQueue() {
        queue.sort((t1, t2) -> {
            long diff = t1.trigger.nextExecutionTime - t2.trigger.nextExecutionTime;
            if (diff < 0)
                return -1;
            if (diff == 0)
                return 0;
            return 1;
        });
    }

    /**
     * Clear tasks, set time to 0l
     */
    public List<TimerTask> reset() {
        if(ownsExecutor) {
            executorService.shutdownNow();
            init();
        }
        List<TimerTask> tasks = getTasks();
        queue.clear();
        timeSource.setTime(0L);
        cancelled = false;
        return tasks;
    }    
    
    @Override
    public List<TimerTask> cancel() {
        cancelled = true;
        List<TimerTask> tasks = getTasks();
        queue.clear();
        return tasks;
    }

    @Override
    public int purge() {
        int result = 0;

        for (int i = queue.size()-1; i >= 0; i--) {
            if (queue.get(i).state == TimerTask.CANCELLED) {
                queue.remove(i);
                result++;
            }
        }

        return result;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public List<TimerTask> getTasks() {
        return new ArrayList<>(queue);
    }

    @Override
    public long currentTimeMillis() {
        return timeSource.currentTimeMillis();
    }

	@Override
	public void execute(Task command) {
	    if(async) {
	        this.executorService.execute(new TaskWrapper(command, this.timeSource.currentTimeMillis()));
	    }else {
	        //TODO should we run this here or a new thread?
            new Thread(() -> command.runTask(timeSource.currentTimeMillis())).start();
	    }
	}

    @Override
    public void init() {
        this.ownsExecutor = true;
        this.executorService = new OrderedThreadPoolExecutor(0, 1000, 30L, TimeUnit.SECONDS, new SynchronousQueue<>(), false, timeSource);
    }

    @Override
    public void init(ExecutorService executorService) {
        this.executorService = (OrderedThreadPoolExecutor)executorService;
    }

    @Override
    public void init(TimerThread timer) {
        
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public TimeSource getTimeSource() {
        return timeSource;
    }
    
    /**
     * Override as necessary
     */
    void taskRejected(long executionTime, TimerTask task, RejectedExecutionException e) {
        task.rejectedAsDelegate(new RejectedTaskReason(RejectedTaskReason.POOL_FULL, executionTime, task, this.executorService));
    }
}
