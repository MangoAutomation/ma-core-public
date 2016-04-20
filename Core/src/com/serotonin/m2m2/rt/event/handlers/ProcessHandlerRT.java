/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.maint.work.ProcessWorkItem;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;

/**
 * @author Matthew Lohbihler
 */
public class ProcessHandlerRT extends EventHandlerRT<ProcessEventHandlerVO> {
    public ProcessHandlerRT(ProcessEventHandlerVO vo) {
        super(vo);
    }

    @Override
    public void eventRaised(EventInstance evt) {
        executeCommand(vo.getActiveProcessCommand(), vo.getActiveProcessTimeout());
    }

    @Override
    public void eventInactive(EventInstance evt) {
        executeCommand(vo.getInactiveProcessCommand(), vo.getInactiveProcessTimeout());
    }

    private void executeCommand(String command, int timeout) {
        if (StringUtils.isBlank(command))
            return;
        ProcessWorkItem.queueProcess(command, timeout);
    }
}
