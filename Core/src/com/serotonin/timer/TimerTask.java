package com.serotonin.timer;


public abstract class TimerTask extends Task{
    
	TimerTrigger trigger;
	
	//TODO Uncomment when done with Mango implementation
//	public TimerTask(TimerTrigger trigger) {
//        this(trigger, null, null, 0);
//    }
//
//    public TimerTask(TimerTrigger trigger, String name) {
//    	this(trigger, name, null, 0);
//    }
	
	/**
	 * Create a Timer task with both a name and id
	 * @param trigger
	 * @param name - name of thread during runtime
	 * @param id - not null will create an ordered queue of tasks if the timer supports it
	 */
    public TimerTask(TimerTrigger trigger, String name, String id, int queueSize, boolean queueable) {
    	super(name, id, queueSize, queueable);
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
}
