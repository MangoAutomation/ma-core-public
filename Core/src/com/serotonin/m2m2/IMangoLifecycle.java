package com.serotonin.m2m2;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.provider.Provider;

public interface IMangoLifecycle extends Provider {

    boolean isTerminated();

    void terminate();

    void addStartupTask(Runnable task);
    void addShutdownTask(Runnable task);

    void addListener(Consumer<LifecycleState> listener);
    void removeListener(Consumer<LifecycleState> listener);

    /**
     * Get the state of the Lifecycle
     *
     * @return
     */
    public LifecycleState getLifecycleState();

    /**
     * Get the percentage 0-100
     * 0 is Not Started
     * 100 is running
     *
     *
     * @return
     */
    public int getStartupProgress();

    /**
     * Get the percentage 0-100
     * 0 is Running
     * 100 is Shutdown
     *
     * @return
     */
    public int getShutdownProgress();

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
     * Check that the properties file the input stream is from contains the properties described in verify
     * @return
     */
    public boolean verifyProperties(InputStream in, boolean signed, Map<String, String> verify);

    /**
     * @param timeout
     * @param restart (should Mango restart?)
     * @return
     */
    Thread scheduleShutdown(Long timeout, boolean restart, PermissionHolder user);

    /**
     * @return
     */
    boolean isRestarting();

    /**
     * (Re)load ssl certificates and keys
     */
    public void reloadSslContext();

    public default ServerStatus getServerStatus() {
        return ServerStatus.NOT_RUNNING;
    }

    public default boolean isSafeMode() {
        return false;
    }
}
