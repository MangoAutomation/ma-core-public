/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.serotonin.m2m2;

import com.serotonin.m2m2.rt.maint.BackgroundProcessingImpl;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.timer.TimerTask;

/**
 * Simple background processing implementation to run all tasks in the calling thread.
 */
public class CallingThreadMockBackgroundProcessing extends BackgroundProcessingImpl {

    @Override
    public void addWorkItem(final WorkItem item) {
        item.execute();
    }

    @Override
    public void execute(HighPriorityTask task) {
        task.runTask(Common.timer.currentTimeMillis());
    }

    @Override
    public void executeMediumPriorityTask(TimerTask task) {
        task.runTask(Common.timer.currentTimeMillis());
    }

    @Override
    public void schedule(TimerTask task) {
        task.runTask(Common.timer.currentTimeMillis());
    }

    @Override
    public void schedule(TimeoutTask task) {
        task.runTask(Common.timer.currentTimeMillis());
    }
}
