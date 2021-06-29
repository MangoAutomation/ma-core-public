/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.timeout;

import java.util.Date;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.timer.TimerTask;
import com.serotonin.timer.TimerTrigger;

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
        super(trigger, client.getThreadName(), client.getTaskId(), client.getQueueSize());
        this.client = client;
        this.model = model;
        Common.backgroundProcessing.schedule(this);
    }
    
    @Override
    public void run(long runtime) {
        client.scheduleTimeout(model, runtime);
    }
}
