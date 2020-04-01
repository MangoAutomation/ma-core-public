/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public enum LifecycleState {

    NOT_STARTED(0, new TranslatableMessage("startup.state.notStarted")),
    WEB_SERVER_INITIALIZE(10, new TranslatableMessage("startup.state.webServerInitialize")),
    PRE_INITIALIZE(20, new TranslatableMessage("startup.state.preInitialize")),
    TIMER_INITIALIZE(30, new TranslatableMessage("startup.state.timerInitialize")),
    JSON_INITIALIZE(40, new TranslatableMessage("startup.state.jsonInitialize")),
    EPOLL_INITIALIZE(50, new TranslatableMessage("startup.state.epollInitialize")),
    LICENSE_CHECK(60, new TranslatableMessage("startup.state.licenseCheck")),
    FREEMARKER_INITIALIZE(70, new TranslatableMessage("startup.state.freeMarkerInitialize")),
    DATABASE_INITIALIZE(80, new TranslatableMessage("startup.state.databaseInitialize")),
    POST_DATABASE_INITIALIZE(90, new TranslatableMessage("startup.state.postDatabaseInitialize")),
    UTILITIES_INITIALIZE(100, new TranslatableMessage("startup.state.utilitesInitialize")),
    EVENT_MANAGER_INITIALIZE(110, new TranslatableMessage("startup.state.eventManagerInitialize")),
    RUNTIME_MANAGER_INITIALIZE(150, new TranslatableMessage("startup.state.runtimeManagerInitialize")),
    MAINTENANCE_INITIALIZE(160, new TranslatableMessage("startup.state.maintenanceInitialize")),
    WEB_SERVER_FINALIZE(175, new TranslatableMessage("startup.state.webServerFinalize")),
    POST_INITIALIZE(180, new TranslatableMessage("startup.state.postInitialize")),
    STARTUP_TASKS_RUNNING(190, new TranslatableMessage("startup.state.startupTasksRunning")),
    RUNNING(200, new TranslatableMessage("startup.state.running")),

    PRE_TERMINATE(210, new TranslatableMessage("shutdown.state.preTerminate")),
    SHUTDOWN_TASKS_RUNNING(220, new TranslatableMessage("shutdown.state.shutdownTasksRunning")),
    WEB_SERVER_TERMINATE(230, new TranslatableMessage("shutdown.state.webServerTerminate")),
    RUNTIME_MANAGER_TERMINATE(240, new TranslatableMessage("shutdown.state.runtimeManagerTerminate")),
    EPOLL_TERMINATE(255, new TranslatableMessage("shutdown.state.epollTerminate")),
    UTILITIES_TERMINATE(260, new TranslatableMessage("shutdown.state.utilitiesTerminate")),
    TIMER_TERMINATE(265, new TranslatableMessage("shutdown.state.timerTerminate")),
    EVENT_MANAGER_TERMINATE(270, new TranslatableMessage("shutdown.state.eventManagerTerminate")),
    DATABASE_TERMINATE(280, new TranslatableMessage("shutdown.state.databaseTerminate")),
    POST_TERMINATE(310, new TranslatableMessage("shutdown.state.databasePostTerminate")),
    TERMINATED(400, new TranslatableMessage("shutdown.state.terminated"));

    private final int value;
    private final TranslatableMessage description;

    private LifecycleState(int value, TranslatableMessage description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    /**
     * Get the percentage 0-100
     * 0 is Not Started
     * 100 is running
     * @return
     */
    public int getStartupProgress() {
        if (this.value >= RUNNING.value)
            return 100;
        return 100 * (this.value - NOT_STARTED.value) / (RUNNING.value - NOT_STARTED.value);
    }

    /**
     * Get the percentage 0-100
     * 0 is Running
     * 100 is Shutdown
     * @return
     */
    public int getShutdownProgress() {
        if (this.value <= RUNNING.value)
            return 0;
        if (this.value >= TERMINATED.value)
            return 100;
        return 100 * (this.value - RUNNING.value) / (TERMINATED.value - RUNNING.value);
    }
}
