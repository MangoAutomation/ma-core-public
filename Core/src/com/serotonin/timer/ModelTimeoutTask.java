package com.serotonin.timer;

import java.util.Date;
import java.util.concurrent.RejectedExecutionException;

/**
 * A parameterizable one-time task. Allows the pass-through of a model to the target.
 * 
 * @author Matthew
 * 
 * @param <T>
 */
public class ModelTimeoutTask<T> extends TimerTask {
    private final ModelTimeoutClient<T> client;
    private final T model;

    public ModelTimeoutTask(long delay, ModelTimeoutClient<T> client, T model) {
        this(new OneTimeTrigger(delay), client, model);
    }

    public ModelTimeoutTask(Date date, ModelTimeoutClient<T> client, T model) {
        this(new OneTimeTrigger(date), client, model);
    }

    public ModelTimeoutTask(TimerTrigger trigger, ModelTimeoutClient<T> client, T model) {
        super(trigger, null, null, 0, false);
        this.client = client;
        this.model = model;
    }

    @Override
    public void run(long runtime) {
        client.scheduleTimeout(model, runtime);
    }
    
    @Override
    public void rejected(RejectedTaskReason reason){
    	throw new RejectedExecutionException(reason.getDescription());
    }
}
