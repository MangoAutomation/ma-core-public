package com.serotonin.m2m2;

import com.serotonin.provider.Provider;

public interface ILifecycle extends Provider {
    boolean isTerminated();

    void terminate();

    void addStartupTask(Runnable task);

    void addShutdownTask(Runnable task);
}
