/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers;

import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;

/**
 * 
 * @author Terry Packer
 */
public class ProcessEventHandlerModel extends AbstractEventHandlerModel<ProcessEventHandlerVO>{

	public ProcessEventHandlerModel(){
		super(new ProcessEventHandlerVO());
	}
	
	public ProcessEventHandlerModel(ProcessEventHandlerVO data) {
		super(data);
	}

	public String getActiveProcessCommand() {
        return this.data.getActiveProcessCommand();
    }

    public void setActiveProcessCommand(String activeProcessCommand) {
        this.data.setActiveProcessCommand(activeProcessCommand);
    }

    public int getActiveProcessTimeout() {
        return this.data.getActiveProcessTimeout();
    }

    public void setActiveProcessTimeout(int activeProcessTimeout) {
        this.data.setActiveProcessTimeout(activeProcessTimeout);
    }

    public String getInactiveProcessCommand() {
        return this.data.getInactiveProcessCommand();
    }

    public void setInactiveProcessCommand(String inactiveProcessCommand) {
        this.data.setInactiveProcessCommand(inactiveProcessCommand);
    }

    public int getInactiveProcessTimeout() {
        return this.data.getInactiveProcessTimeout();
    }

    public void setInactiveProcessTimeout(int inactiveProcessTimeout) {
        this.data.setInactiveProcessTimeout(inactiveProcessTimeout);
    }
	
}
