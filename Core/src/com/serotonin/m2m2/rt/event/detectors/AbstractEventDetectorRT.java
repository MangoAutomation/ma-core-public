/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.timer.Task;

/**
 * 
 * Abstraction for Event Detector Runtimes
 * @author Terry Packer
 *
 */
public abstract class AbstractEventDetectorRT<T extends AbstractEventDetectorVO<?>> {

	protected T vo;
	private TimeoutClient quiescentClient;
	private Task quiescentTask;
	private final long quiescentMillis;
	private boolean quiescentState;
	private Map<String, Object> quiescentContext;
	
	public AbstractEventDetectorRT(T vo){
		this.vo = vo;
		quiescentMillis = Common.getMillis(vo.getQuiescentPeriodType(), vo.getQuiescentPeriods());
		quiescentClient = new TimeoutClient() {

            @Override
            public void scheduleTimeout(long fireTime) {
                wakeup();
            }
            
            @Override
            public String getTaskId() {
                //XIDs unique as of 3.3
                return "EDQ-" + vo.getXid();
            }

            @Override
            public String getThreadName() {
                return vo.getXid() + " Quiescence";
            }
		};
	}
	
	public T getVO(){
		return vo;
	}
	
	protected void raiseEvent(long time, Map<String, Object> context) {
	    if(quiescentMillis != 0 && quiescentTask != null) {
	        synchronized(this) { //get the lock and check
	            if(quiescentTask != null) {
	                quiescentState = true;
	                quiescentContext = context;
	                return;
	            }
	        }
	    }
	    
        TranslatableMessage msg;
        if (!StringUtils.isBlank(vo.getName()))
            msg = new TranslatableMessage("common.default", vo.getName());
        else
            msg = getMessage();

        Common.eventManager.raiseEvent(getEventType(), time, vo.isRtnApplicable(), vo.getAlarmLevel(), msg, context);
    }

    protected void returnToNormal(long time) {
        quiescentState = false;
        Common.eventManager.returnToNormal(getEventType(), time, vo.getAlarmLevel());
        if(quiescentMillis != 0) {
            synchronized(this) {
                if(quiescentTask != null)
                    quiescentTask.cancel();
                quiescentTask = new TimeoutTask(new OneTimeTrigger(quiescentMillis), quiescentClient);
            }
        }
    }
    
    private void wakeup() {
        boolean state;
        Map<String, Object> context;
        synchronized(this) {
            state = quiescentState;
            context = quiescentContext;
            quiescentState = false;
            quiescentContext = null;
            quiescentTask = null;
        }
        
        if(state) {
            raiseEvent(Common.timer.currentTimeMillis(), context);
        }
    }
    
    protected abstract EventType getEventType();
    
    abstract protected TranslatableMessage getMessage();
}
