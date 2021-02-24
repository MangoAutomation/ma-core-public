/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.systemSettings;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.timer.RejectedTaskReason;

/**
 * Dispatch events for System settings changes
 *   This class is deprecated, use the Spring event SystemSettingChangeAuditEvent
 *
 * @author Terry Packer
 *
 */
@Deprecated
public class SystemSettingsEventDispatcher {

    private enum SystemSettingEventType {
        SETTING_SAVED, SETTING_REMOVED
    }

    /**
     * Try not to use this static field, inject the dispatcher using spring wherever you can
     * (or simply declare your SystemSettingsListener as a Spring bean, it will be automatically added to the dispatcher).
     * We need this static instance for now as SystemSettingsEventDispatcher is used before the Spring runtime context is initialized.
     */
    public static final SystemSettingsEventDispatcher INSTANCE = new SystemSettingsEventDispatcher();

    private SystemSettingsEventDispatcher() {
    }

    private final Map<String, Set<SystemSettingsListener>> listenerMap = new ConcurrentHashMap<String, Set<SystemSettingsListener>>();

    /**
     * Add the listener, subscribing to the keys returned from {@link com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#getKeys() getKeys()}
     * Note: you can also simply declare your SystemSettingsListener as a Spring bean, it will be automatically added to the dispatcher (and removed when destroyed).
     * @param l
     */
    public void addListener(SystemSettingsListener l) {
        //Add the listener for all supported keys
        for (String key : l.getKeys()) {
            listenerMap.compute(key, (k, v) -> {
                if (v == null) {
                    // although the set will never be concurrently modified, it is possible that another thread will be iterating
                    // over the set when a modification occurs
                    v = ConcurrentHashMap.newKeySet();
                }
                v.add(l);
                return v;
            });
        }
    }

    /**
     * Remove the listener
     * @param l
     */
    public void removeListener(SystemSettingsListener l) {
        //Remove the listener for any keys
        for(String key : l.getKeys()){
            listenerMap.computeIfPresent(key, (k, v) -> {
                v.remove(l);
                if (v.isEmpty()) {
                    return null;
                }
                return v;
            });
        }
    }

    /**
     * Setting was saved
     * @param key
     * @param oldValue
     * @param newValue
     */
    public void fireSystemSettingSaved(String key, String oldValue, String newValue) {
        Set<SystemSettingsListener> listeners = listenerMap.getOrDefault(key, null);
        if(listeners != null) {
            Common.backgroundProcessing.addWorkItem(new DispatcherExecution(listeners, key, oldValue, newValue, SystemSettingEventType.SETTING_SAVED));
        }
    }

    /**
     * Setting was removed
     * @param key
     * @param lastValue
     * @param defaultValue
     */
    public void fireSystemSettingRemoved(String key, String lastValue, String defaultValue) {
        Set<SystemSettingsListener> listeners = listenerMap.getOrDefault(key, null);
        if(listeners != null) {
            Common.backgroundProcessing.addWorkItem(new DispatcherExecution(listeners, key, lastValue, defaultValue, SystemSettingEventType.SETTING_REMOVED));
        }
    }

    static class DispatcherExecution implements WorkItem {
        private final String description = "System settings event dispatcher for: ";
        private final Set<SystemSettingsListener> listeners;
        private final String key;
        private final String oldValue;
        private final String newValue;
        private final SystemSettingEventType operation;

        public DispatcherExecution(Set<SystemSettingsListener> listeners, String key,  String oldValue, String newValue, SystemSettingEventType operation) {
            this.listeners = listeners;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.operation = operation;
        }

        @Override
        public void execute() {
            switch (operation) {
                case SETTING_SAVED:
                    for (SystemSettingsListener l : listeners) {
                        l.systemSettingsSaved(key, oldValue, newValue);
                    }
                    break;
                case SETTING_REMOVED:
                    for (SystemSettingsListener l : listeners) {
                        l.systemSettingsRemoved(key, oldValue, newValue);
                    }
                    break;
            }
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_MEDIUM;
        }

        @Override
        public String getDescription() {
            return description + key;
        }

        private final String TASK_ID = "SYSTEM_SETTINGS_LISTENER_";

        @Override
        public String getTaskId() {
            return TASK_ID + key ;
        }

        @Override
        public int getQueueSize() {
            return 100;
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            //No special handling, as a work item this is recorded in BackgroundProcessing
            System.out.println("OOOPS");
        }
    }
}
