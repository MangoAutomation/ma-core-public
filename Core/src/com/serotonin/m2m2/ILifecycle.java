package com.serotonin.m2m2;

import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.provider.Provider;

public interface ILifecycle extends Provider {
    boolean isTerminated();

    void terminate();

    void addStartupTask(Runnable task);

    void addShutdownTask(Runnable task);

    /**
     * Get the state of the Lifecycle
     * 
     * @return
     */
    public int getLifecycleState();

    /**
     * Get the percentage 0-100
     * 0 is Not Started
     * 100 is running
     * 
     * 
     * @return
     */
    public float getStartupProgress();

    /**
     * Get the percentage 0-100
     * 0 is Running
     * 100 is Shutdown
     * 
     * @return
     */
    public float getShutdownProgress();

    /**
     * (Re)load the license
     */
    public void loadLic();

    /**
     * @param timeout
     * @param b
     * @return
     */
    TimeoutTask scheduleShutdown(long timeout, boolean b);

    /**
     * @return
     */
    boolean isRestarting();
}
