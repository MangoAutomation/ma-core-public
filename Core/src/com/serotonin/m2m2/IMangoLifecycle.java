package com.serotonin.m2m2;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.provider.Provider;

public interface IMangoLifecycle extends Provider {

    boolean isTerminated();
    TerminationReason getTerminationReason();

    void terminate(TerminationReason reason);

    void addStartupTask(Runnable task);
    void addShutdownTask(Runnable task);

    void addListener(Consumer<LifecycleState> listener);
    void removeListener(Consumer<LifecycleState> listener);

    /**
     * Get the state of the Lifecycle
     *
     */
    public LifecycleState getLifecycleState();

    /**
     * Get the percentage 0-100
     * 0 is Not Started
     * 100 is running
     *
     *
     */
    public int getStartupProgress();

    /**
     * Get the percentage 0-100
     * 0 is Running
     * 100 is Shutdown
     *
     */
    public int getShutdownProgress();

    /**
     * (Re)load the license
     */
    public void loadLic();

    /**
     * Get the data point limit
     */
    public Integer dataPointLimit();

    /**
     * Check that the properties file the input stream is from contains the properties described in verify
     */
    public boolean verifyProperties(InputStream in, boolean signed, Map<String, String> verify);

    /**
     * @param restart (should Mango restart?)
     */
    Thread scheduleShutdown(Long timeout, boolean restart, PermissionHolder user);

    /**
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
