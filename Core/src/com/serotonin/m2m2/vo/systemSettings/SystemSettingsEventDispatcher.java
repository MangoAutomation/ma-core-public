/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.systemSettings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;

/**
 * @author Terry Packer
 *
 */
public class SystemSettingsEventDispatcher {

	private final static int SETTING_SAVED = 1;
	private final static int SETTING_REMOVED = 2;
	
    private final static List<SystemSettingsListener> LISTENERS = new CopyOnWriteArrayList<SystemSettingsListener>();

	
    public static void addListener(SystemSettingsListener l) {
        LISTENERS.add(l);
    }

    public static void removeListener(SystemSettingsListener l) {
        LISTENERS.remove(l);
    }

    public static void fireSystemSettingSaved(String key, Object oldValue, Object newValue) {
        for (SystemSettingsListener l : LISTENERS)
            Common.backgroundProcessing.execute(new DispatcherExecution(l, key, oldValue, newValue, SETTING_SAVED));
    }

    public static void fireSystemSettingRemoved(String key, Object lastValue) {
        for (SystemSettingsListener l : LISTENERS)
            Common.backgroundProcessing.execute(new DispatcherExecution(l, key, lastValue, null, SETTING_REMOVED));
    }

    static class DispatcherExecution extends HighPriorityTask {
        private final SystemSettingsListener l;
        private final String key;
        private final Object oldValue;
        private final Object newValue;
        private final int operation;

        public DispatcherExecution(SystemSettingsListener l,String key,  Object oldValue, Object newValue, int operation) {
        	super("System settings event dispatcher", null, 0);
        	this.l = l;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.operation = operation;
        }

        @Override
        public void run(long runtime) {
            switch(operation){
            case SETTING_SAVED:
            	l.SystemSettingsSaved(key, oldValue, newValue);
            	break;
            case SETTING_REMOVED:
            	l.SystemSettingsRemoved(key, oldValue);
            	break;
            }
        }
    }
}
