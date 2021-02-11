/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

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
        executeCommand(evt, vo.getActiveProcessCommand(), vo.getActiveProcessTimeout());
    }

    @Override
    public void eventInactive(EventInstance evt) {
        executeCommand(evt, vo.getInactiveProcessCommand(), vo.getInactiveProcessTimeout());
    }

    private void executeCommand(EventInstance event, String command, int timeout) {
        if (StringUtils.isBlank(command))
            return;
        if (vo.isInterpolateCommands()) {
            command = getSubstitutor(event).replace(command);
        }
        ProcessWorkItem.queueProcess(command, timeout);
    }

    private StringSubstitutor getSubstitutor(EventInstance event) {
        return new StringSubstitutor(name -> {
            try {
                SubstituteContext context = new SubstituteContext(event);
                return BeanUtils.getProperty(context, name);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to access property '%s'", name), e);
            }
        });
    }

    public static class SubstituteContext {
        private final EventInstance event;

        private SubstituteContext(EventInstance event) {
            this.event = event;
        }

        public EventInstance getEvent() {
            return event;
        }
    }
}
