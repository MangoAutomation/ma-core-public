/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import com.serotonin.m2m2.vo.User;

/**
 * Dummy implementation for Mango Lifecycle for use in testing, 
 *   override as necessary.
 *
 * @author Terry Packer
 */
public class MockMangoLifecycle implements IMangoLifecycle{

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#isTerminated()
     */
    @Override
    public boolean isTerminated() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#terminate()
     */
    @Override
    public void terminate() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#addStartupTask(java.lang.Runnable)
     */
    @Override
    public void addStartupTask(Runnable task) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#addShutdownTask(java.lang.Runnable)
     */
    @Override
    public void addShutdownTask(Runnable task) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#getLifecycleState()
     */
    @Override
    public int getLifecycleState() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#getStartupProgress()
     */
    @Override
    public float getStartupProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#getShutdownProgress()
     */
    @Override
    public float getShutdownProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#loadLic()
     */
    @Override
    public void loadLic() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#dataPointLimit()
     */
    @Override
    public Integer dataPointLimit() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#scheduleShutdown(long, boolean, com.serotonin.m2m2.vo.User)
     */
    @Override
    public Thread scheduleShutdown(long timeout, boolean b, User user) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.IMangoLifecycle#isRestarting()
     */
    @Override
    public boolean isRestarting() {
        // TODO Auto-generated method stub
        return false;
    }

}
