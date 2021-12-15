/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

/**
 * @author Jared Wiltshire
 */
public enum TerminationReason {
    /**
     * Clean termination requested by user via UI/API, return 0 (success)
     */
    SHUTDOWN(0),

    /**
     * System.exit() called and JVM is shutting down. Exit status is not used.
     */
    SHUTDOWN_HOOK(-1),

    /**
     * User requested restart via UI/API. Positive exit status notifies service managers (systemd or WinSW) that the process should be restarted.
     */
    RESTART(200),

    /**
     * Mango failed to initialize
     */
    INITIALIZATION_ERROR(201),

    /**
     * License violation causes Mango to shutdown, sometimes after a delay (trial period)
     */
    LICENSE_VIOLATION(202),

    /**
     * A {@link OutOfMemoryError} was caught in a thread pool. Note that with the {@code -XX:+ExitOnOutOfMemoryError}
     * option configured, Java will exit with exit status 3.
     */
    OUT_OF_MEMORY_ERROR(203);

    private final int exitStatus;

    TerminationReason(int exitStatus) {
        this.exitStatus = exitStatus;
    }

    public int getExitStatus() {
        return exitStatus;
    }
}
