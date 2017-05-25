package com.serotonin.util;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;

/**
 * @author Matthew Lohbihler
 */
abstract public class ProgressiveTask extends Task {
    private boolean cancelled = false;
    protected boolean completed = false;
    private ProgressiveTaskListener listener;

    public ProgressiveTask(String name, ProgressiveTaskListener l) {
    	super(name);
        listener = l;
    }
    
    /**
     * Create an ordered and limited queue task
     * @param name
     * @param id
     * @param queueSize
     * @param l
     */
    public ProgressiveTask(String name, String id, int queueSize, ProgressiveTaskListener l) {
    	super(name, id, queueSize);
        listener = l;
    }
    

    public boolean cancel() {
        cancelled = true;
        return super.cancel();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isCompleted() {
        return completed;
    }

    public final void run(long runtime) {
        while (true) {
            if (isCancelled()) {
                declareFinished(true);
                break;
            }

            runImpl();

            if (isCompleted()) {
                declareFinished(false);
                break;
            }
        }
        completed = true;
    }

    protected void declareProgress(float progress) {
        ProgressiveTaskListener l = listener;
        if (l != null)
            l.progressUpdate(progress);
    }

    private void declareFinished(boolean cancelled) {
        ProgressiveTaskListener l = listener;
        if (l != null) {
            if (cancelled)
                l.taskCancelled();
            else
                l.taskCompleted();
        }
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		 ProgressiveTaskListener l = listener;
	        if (l != null)
	        	l.taskRejected(reason);
		Common.backgroundProcessing.rejectedHighPriorityTask(reason);
	}

    /**
     * Implementers of this method MUST return from it occasionally so that the cancelled status can be checked. Each
     * return must leave the class and thread state with the expectation that runImpl will not be called again, while
     * acknowledging the possibility that it will.
     * 
     * Implementations SHOULD call the declareProgress method with each runImpl execution such that the listener can be
     * notified.
     * 
     * Implementations MUST set the completed field to true when the task is finished.
     */
    abstract protected void runImpl();
}
