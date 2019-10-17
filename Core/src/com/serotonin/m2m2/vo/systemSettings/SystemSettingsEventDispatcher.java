/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.systemSettings;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.timer.RejectedTaskReason;

/**
 * Dispatch events for System settings changes
 * @author Terry Packer
 *
 */
public class SystemSettingsEventDispatcher {

    private enum SystemSettingEventType {
        SETTING_SAVED, SETTING_REMOVED
    }

    private final static Map<String, Set<SystemSettingsListener>> LISTENER_MAP = new ConcurrentHashMap<String, Set<SystemSettingsListener>>();

    /**
     * Add the listener to whatever keys it should listen to
     * @param l
     */
    public static void addListener(SystemSettingsListener l) {
        //Add the listener for all supported keys
        for (String key : l.getKeys()) {
            LISTENER_MAP.compute(key, (k, v) -> {
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
     * Remove the listener from any keys it is listening to
     * @param l
     */
    public static void removeListener(SystemSettingsListener l) {
        //Remove the listener for any keys
        for(String key : l.getKeys()){
            LISTENER_MAP.computeIfPresent(key, (k, v) -> {
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
    public static void fireSystemSettingSaved(String key, String oldValue, String newValue) {
        Set<SystemSettingsListener> listeners = LISTENER_MAP.getOrDefault(key, Collections.emptySet());
        for (SystemSettingsListener l : listeners) {
            Common.backgroundProcessing.addWorkItem(
                    new DispatcherExecution(l, key, oldValue, newValue, SystemSettingEventType.SETTING_SAVED));
        }
    }

    /**
     * Setting was removed
     * @param key
     * @param lastValue
     */
    public static void fireSystemSettingRemoved(String key, String lastValue, String defaultValue) {
        Set<SystemSettingsListener> listeners = LISTENER_MAP.getOrDefault(key, Collections.emptySet());
        for (SystemSettingsListener l : listeners) {
            Common.backgroundProcessing.addWorkItem(
                    new DispatcherExecution(l, key, lastValue, defaultValue, SystemSettingEventType.SETTING_REMOVED));
        }
    }

    static class DispatcherExecution implements WorkItem {
        private final String description = "System settings event dispatcher for: ";
        private final SystemSettingsListener l;
        private final String key;
        private final String oldValue;
        private final String newValue;
        private final SystemSettingEventType operation;

        public DispatcherExecution(SystemSettingsListener l, String key,  String oldValue, String newValue, SystemSettingEventType operation) {
            this.l = l;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.operation = operation;
        }

        @Override
        public void execute() {
            switch (operation) {
                case SETTING_SAVED:
                    l.systemSettingsSaved(key, oldValue, newValue);
                    break;
                case SETTING_REMOVED:
                    l.systemSettingsRemoved(key, oldValue, newValue);
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
            //No special handling
        }
    }
}
