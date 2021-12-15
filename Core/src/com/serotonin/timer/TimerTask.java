package com.serotonin.timer;

import com.serotonin.m2m2.Common;

public abstract class TimerTask extends Task {
    
	TimerTrigger trigger;
	
	public TimerTask(TimerTrigger trigger, String name){
		super(name);
        if (trigger == null)
            throw new NullPointerException();
        this.trigger = trigger;
	}
	/**
	 * Create a Timer task with both a name and id
	 * @param name - name of thread during runtime
	 * @param id - not null will create an ordered queue of tasks if the timer supports it
	 */
    public TimerTask(TimerTrigger trigger, String name, String id, int queueSize) {
    	super(name, id, queueSize);
        if (trigger == null)
            throw new NullPointerException();
        this.trigger = trigger;
    }
    
    public long getNextExecutionTime() {
        return trigger.nextExecutionTime;
    }

    void setTimer(AbstractTimer timer) {
        trigger.setTimer(timer);
    }

    AbstractTimer getTimer() {
        return trigger.getTimer();
    }
    
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.backgroundProcessing.rejectedHighPriorityTask(reason);
	}
}
