/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event;

/**
 * Provides a summary for an event level of all
 *  unsilenced events for a user.
 * @author Terry Packer
 */
public class UserEventLevelSummary {
    
    private int alarmLevel;
    private int unsilencedCount;
    private EventInstance latest;
    
    public UserEventLevelSummary() {
        
    }
    
    public UserEventLevelSummary(int alarmLevel) {
        this.alarmLevel = alarmLevel;
    }
    /**
     * @return the alarmLevel
     */
    public int getAlarmLevel() {
        return alarmLevel;
    }
    /**
     * @param alarmLevel the alarmLevel to set
     */
    public void setAlarmLevel(int alarmLevel) {
        this.alarmLevel = alarmLevel;
    }
    /**
     * @return the unsilencedCount
     */
    public int getUnsilencedCount() {
        return unsilencedCount;
    }
    /**
     * @param unsilencedCount the unsilencedCount to set
     */
    public void setUnsilencedCount(int unsilencedCount) {
        this.unsilencedCount = unsilencedCount;
    }
    /**
     * @return the latest
     */
    public EventInstance getLatest() {
        return latest;
    }
    /**
     * @param latest the latest to set
     */
    public void setLatest(EventInstance latest) {
        this.latest = latest;
    }

}
