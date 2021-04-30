/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint.work;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;
import com.serotonin.util.ILifecycleState;

/**
 * @author Matthew Lohbihler
 */
public class SetPointWorkItem implements WorkItem {
    private static final ThreadLocal<List<String>> threadLocal = new ThreadLocal<List<String>>();
    private static final int MAX_RECURSION = 10;
    private static final String prefix = "SETPNT-";
    private final int targetPointId;
    private final PointValueTime pvt;
    private final SetPointSource source;
    private final List<String> sourceIds;

    public SetPointWorkItem(int targetPointId, PointValueTime pvt, SetPointSource source) {
        this.targetPointId = targetPointId;
        this.pvt = pvt;
        this.source = source;

        if (threadLocal.get() == null)
            sourceIds = new ArrayList<String>();
        else
            sourceIds = threadLocal.get();
    }

    @Override
    public void execute() {
        String sourceId = source.getSetPointSourceType() + "-" + Integer.toString(source.getSetPointSourceId());

        // Check if we've reached the maximum number of hits for this point
        int count = 0;
        for (String id : sourceIds) {
            if (id.equals(sourceId))
                count++;
        }

        if (count > MAX_RECURSION) {
            source.raiseRecursionFailureEvent();
            return;
        }

        sourceIds.add(sourceId);
        threadLocal.set(sourceIds);
        try {
        	if(Common.runtimeManager.getLifecycleState() == ILifecycleState.RUNNING)
        		Common.runtimeManager.setDataPointValue(targetPointId, pvt, source);
        }
        finally {
            threadLocal.remove();
        }
    }

    @Override
    public int getPriority() {
        return WorkItem.PRIORITY_HIGH;
    }
    
    @Override
    public String getDescription(){
    	return "Setting point with ID: " + Integer.toString(this.targetPointId) + " via " +  source.getSetPointSourceType() + "-" + Integer.toString(source.getSetPointSourceId());
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getTaskId()
	 */
	@Override
	public String getTaskId() {
		return prefix + targetPointId;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getQueueSize()
	 */
	@Override
	public int getQueueSize() {
		return Task.UNLIMITED_QUEUE_SIZE;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) { }


}
