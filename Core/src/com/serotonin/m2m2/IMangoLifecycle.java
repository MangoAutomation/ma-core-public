package com.serotonin.m2m2;

import com.serotonin.m2m2.vo.User;
import com.serotonin.provider.Provider;

public interface IMangoLifecycle extends Provider {
	
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
     * Get the data point limit
     * @return
     */
    public Integer dataPointLimit();

    /**
     * @param timeout
     * @param b
     * @return
     */
    Thread scheduleShutdown(long timeout, boolean b, User user);

    /**
     * @return
     */
    boolean isRestarting();
    
    /**
     * (Re)load ssl certificates and keys
     */
    public void reloadSslContext();
    
    //The Various States
    //States of the Lifecycle
    public static final int NOT_STARTED = 0;
    public static final int WEB_SERVER_INITIALIZE = 10;
    public static final int PRE_INITIALIZE = 20;
    public static final int TIMER_INITIALIZE = 30;
    public static final int JSON_INITIALIZE = 40;
    public static final int EPOLL_INITIALIZE = 50;
    public static final int LICENSE_CHECK = 60;
    public static final int FREEMARKER_INITIALIZE = 70;
    public static final int DATABASE_INITIALIZE = 80;
    public static final int POST_DATABASE_INITIALIZE = 90;
    public static final int UTILITIES_INITIALIZE = 100;
    public static final int EVENT_MANAGER_INITIALIZE = 110;
    public static final int RUNTIME_MANAGER_INITIALIZE = 150;
    public static final int MAINTENANCE_INITIALIZE = 160;
    public static final int IMAGE_SET_INITIALIZE = 170;
    public static final int WEB_SERVER_FINALIZE = 175;
    public static final int POST_INITIALIZE = 180;
    public static final int STARTUP_TASKS_RUNNING = 190;

    public static final int RUNNING = 200;
    //Shutdown sequence
    public static final int PRE_TERMINATE = 210;
    public static final int SHUTDOWN_TASKS_RUNNING = 220;
    public static final int WEB_SERVER_TERMINATE = 230;
    public static final int RUNTIME_MANAGER_TERMINATE = 240;
    public static final int EPOLL_TERMINATE = 255;
    public static final int UTILITIES_TERMINATE = 260;
    public static final int TIMER_TERMINATE = 265;
    public static final int EVENT_MANAGER_TERMINATE = 270;
    public static final int DATABASE_TERMINATE = 280;

    public static final int POST_TERMINATE = 310;

    public static final int TERMINATED = 400;
    
}
