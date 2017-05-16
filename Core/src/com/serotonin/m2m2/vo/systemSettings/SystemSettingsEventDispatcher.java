/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.systemSettings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.timer.RejectedTaskReason;

/**
 * Dispatch events for System settings changes
 * @author Terry Packer
 *
 */
public class SystemSettingsEventDispatcher {

	private final static int SETTING_SAVED = 1;
	private final static int SETTING_REMOVED = 2;
	
    private final static Map<String, List<SystemSettingsListener>> LISTENER_MAP = new ConcurrentHashMap<String, List<SystemSettingsListener>>();
	
    /**
     * Add the listener to whatever keys it should listen to
     * @param l
     */
    public static void addListener(SystemSettingsListener l) {
        //Add the listener for all supported keys
    	List<SystemSettingsListener> list;
        for(String key : l.getKeys()){
        	list = LISTENER_MAP.get(key);
        	if(list == null){
        		list = new CopyOnWriteArrayList<SystemSettingsListener>();
        		LISTENER_MAP.put(key, list);
        	}
        	list.add(l);
        }
    }

    /**
     * Remove the listener from any keys it is listening to
     * @param l
     */
    public static void removeListener(SystemSettingsListener l) {
        //Remove the listener for any keys
    	List<SystemSettingsListener> list;
    	for(String key : l.getKeys()){
    		list = LISTENER_MAP.get(key);
    		if(list != null){
    			//Remove me from the list
    			list.remove(l);
    			//If the list is empty the remove it from the map
    			if(list.size() == 0)
    				LISTENER_MAP.remove(key);
    		}
    	}
    }

    /**
     * Setting was saved
     * @param key
     * @param oldValue
     * @param newValue
     */
    public static void fireSystemSettingSaved(String key, String oldValue, String newValue) {
        List<SystemSettingsListener> listeners = LISTENER_MAP.get(key);
        if(listeners != null)
	    	for (SystemSettingsListener l : listeners){
	        		Common.backgroundProcessing.addWorkItem(new DispatcherExecution(l, key, oldValue, newValue, SETTING_SAVED));
	        }
    }

    /**
     * Setting was removed
     * @param key
     * @param lastValue
     */
    public static void fireSystemSettingRemoved(String key, String lastValue) {
        List<SystemSettingsListener> listeners = LISTENER_MAP.get(key);
        if(listeners != null)
        for (SystemSettingsListener l : listeners)
            Common.backgroundProcessing.addWorkItem(new DispatcherExecution(l, key, lastValue, null, SETTING_REMOVED));
    }

    static class DispatcherExecution implements WorkItem {
    	private final String description = "System settings event dispatcher for: ";
        private final SystemSettingsListener l;
        private final String key;
        private final String oldValue;
        private final String newValue;
        private final int operation;

        public DispatcherExecution(SystemSettingsListener l, String key,  String oldValue, String newValue, int operation) {
        	this.l = l;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.operation = operation;
        }

        @Override
        public void execute() {
            switch(operation){
            case SETTING_SAVED:
            	l.SystemSettingsSaved(key, oldValue, newValue);
            	break;
            case SETTING_REMOVED:
            	l.SystemSettingsRemoved(key, oldValue);
            	break;
            }
        }

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getPriority()
		 */
		@Override
		public int getPriority() {
			return WorkItem.PRIORITY_MEDIUM;
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getDescription()
		 */
		@Override
		public String getDescription() {
			return description + key;
		}

		private final String TASK_ID = "SYSTEM_SETTINGS_LISTENER_";
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getTaskId()
		 */
		@Override
		public String getTaskId() {
			return TASK_ID + key ;
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getQueueSize()
		 */
		@Override
		public int getQueueSize() {
			return 100;
		}

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.rt.maint.work.WorkItem#rejected(com.serotonin.timer.RejectedTaskReason)
		 */
		@Override
		public void rejected(RejectedTaskReason reason) {
			//No special handling
		}
    }
}
