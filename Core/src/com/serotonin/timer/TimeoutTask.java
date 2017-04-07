package com.serotonin.timer;

import java.util.Date;
import java.util.concurrent.RejectedExecutionException;

/**
 * A simple way of creating a timeout.
 * 
 * @author Matthew
 */
public class TimeoutTask extends TimerTask {
    private final ScheduledRunnable client;

    public TimeoutTask(long delay, ScheduledRunnable client) {
        this(new OneTimeTrigger(delay), client);
    }

    public TimeoutTask(Date date, ScheduledRunnable client) {
        this(new OneTimeTrigger(date), client);
    }

    public TimeoutTask(TimerTrigger trigger, ScheduledRunnable client) {
        super(trigger, null, null, 0, false);
        this.client = client;
    }

    @Override
    public void run(long runtime) {
        client.run(runtime);
    }
    
    @Override
    public void rejected(RejectedTaskReason reason){
    	throw new RejectedExecutionException(reason.getDescription());
    }
}
