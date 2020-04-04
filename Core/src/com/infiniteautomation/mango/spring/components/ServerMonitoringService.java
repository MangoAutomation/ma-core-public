/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.infiniteautomation.mango.monitor.PollableMonitor;
import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.infiniteautomation.mango.spring.service.ServerInformationService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.ServerStatus;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;

/**
 * @author Jared Wiltshire
 * @author Terry Packer
 */
@Service
public class ServerMonitoringService {

    //Jetty Server metrics
    public static final String SERVER_THREADS = "internal.monitor.SERVER_THREADS";
    public static final String SERVER_IDLE_THREADS = "internal.monitor.SERVER_IDLE_THREADS";
    public static final String SERVER_QUEUE_SIZE = "internal.monitor.SERVER_QUEUE_SIZE";

    //JVM Metrics
    public static final String MAX_STACK_HEIGHT_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.maxStackHeight";
    public static final String THREAD_COUNT_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.threadCount";

    //DB Metrics
    public static final String DB_ACTIVE_CONNECTIONS_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.dbActiveConnections";
    public static final String DB_IDLE_CONNECTIONS_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.dbIdleConnections";

    //Work Item Metrics
    public static final String HIGH_PRIORITY_ACTIVE_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.highPriorityActive";
    public static final String HIGH_PRIORITY_SCHEDULED_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.highPriorityScheduled";
    public static final String HIGH_PRIORITY_WAITING_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.highPriorityWaiting";
    public static final String MEDIUM_PRIORITY_ACTIVE_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.mediumPriorityActive";
    public static final String MEDIUM_PRIORITY_WAITING_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.mediumPriorityWaiting";
    public static final String LOW_PRIORITY_ACTIVE_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.lowPriorityActive";
    public static final String LOW_PRIORITY_WAITING_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.lowPriorityWaiting";

    //System Uptime
    public static final String SYSTEM_UPTIME_MONITOR_ID = "mango.system.uptime";

    //User Sessions
    public static final String USER_SESSION_MONITOR_ID = MangoSessionRegistry.class.getCanonicalName() + ".COUNT";

    public static final String AVAILABLE_PROCESSORS_ID = "java.lang.Runtime.availableProcessors";
    public static final String MAX_MEMORY_ID = "java.lang.Runtime.maxMemory";
    public static final String USED_MEMORY_ID = "java.lang.Runtime.usedMemory";
    public static final String FREE_MEMORY_ID = "java.lang.Runtime.freeMemory";

    public static final String LOAD_AVERAGE_MONITOR_ID = "internal.monitor.LOAD_AVERAGE";
    public static final String OS_CPU_LOAD_PROCESS_ID = "os.cpu_load.process";
    public static final String OS_CPU_LOAD_SYSTEM_ID = "os.cpu_load.system";
    public static final String RUNTIME_UPTIME_ID = "runtime.uptime";
    public static final String CLASS_LOADING_LOADED_ID = "class_loading.loaded";
    public static final String CLASS_LOADING_TOTAL_LOADED_ID = "class_loading.total_loaded";
    public static final String CLASS_LOADING_UNLOADED_ID = "class_loading.unloaded";
    public static final String THREAD_COUNT_ID = "thread.count";
    public static final String THREAD_DAEMON_COUNT_ID = "thread.daemon_count";
    public static final String THREAD_PEAK_COUNT_ID = "thread.peak_count";
    public static final String THREAD_TOTAL_STARTED_ID = "thread.total_started";

    private final ValueMonitor<Integer> threads;
    private final ValueMonitor<Integer> idleThreads;
    private final ValueMonitor<Integer> queueSize;
    private final ValueMonitor<Integer> maxStackHeight;
    private final ValueMonitor<Integer> threadCount;
    private final ValueMonitor<Integer> javaMaxMemory;
    private final ValueMonitor<Integer> javaUsedMemory;
    private final ValueMonitor<Integer> javaFreeMemory;
    private final ValueMonitor<Integer> dbActiveConnections;
    private final ValueMonitor<Integer> dbIdleConnections;
    private final ValueMonitor<Integer> highPriorityActive;
    private final ValueMonitor<Integer> highPriorityScheduled;
    private final ValueMonitor<Integer> highPriorityWaiting;
    private final ValueMonitor<Integer> mediumPriorityActive;
    private final ValueMonitor<Integer> mediumPriorityWaiting;
    private final ValueMonitor<Integer> lowPriorityActive;
    private final ValueMonitor<Integer> lowPriorityWaiting;
    private final ValueMonitor<Double> uptime;
    private final ValueMonitor<Integer> userSessions;

    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final Runtime runtime = Runtime.getRuntime();

    private final int mb = 1024*1024;

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final long period;
    private volatile ScheduledFuture<?> scheduledFuture;
    private final IMangoLifecycle lifecycle;
    private final ServerInformationService serverInfoService;
    private final Set<ValueMonitor<?>> monitors = new HashSet<>();
    private final Logger log = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private ServerMonitoringService(ExecutorService executor,
            ScheduledExecutorService scheduledExecutor,
            @Value("${internal.monitor.pollPeriod:10000}") long period,
            IMangoLifecycle lifecycle,
            ServerInformationService serverInfoService,
            MonitoredValues mv) {

        this.executor = executor;
        this.scheduledExecutor = scheduledExecutor;
        this.period = period;
        this.lifecycle = lifecycle;
        this.serverInfoService = serverInfoService;

        threads = mv.<Integer>create(SERVER_THREADS).name(new TranslatableMessage(SERVER_THREADS)).build();
        idleThreads = mv.<Integer>create(SERVER_IDLE_THREADS).name(new TranslatableMessage(SERVER_IDLE_THREADS)).build();
        queueSize = mv.<Integer>create(SERVER_QUEUE_SIZE).name(new TranslatableMessage(SERVER_QUEUE_SIZE)).build();

        maxStackHeight = mv.<Integer>create(MAX_STACK_HEIGHT_MONITOR_ID).name( new TranslatableMessage("internal.monitor.MONITOR_STACK_HEIGHT")).build();
        threadCount = mv.<Integer>create(THREAD_COUNT_MONITOR_ID).name( new TranslatableMessage("internal.monitor.MONITOR_THREAD_COUNT")).build();

        mv.<Integer>create(AVAILABLE_PROCESSORS_ID)
        .name(new TranslatableMessage("java.monitor.JAVA_PROCESSORS"))
        .supplier(() -> this.serverInfoService.availableProcessors().length)
        .addTo(monitors)
        .uploadToStore(true)
        .buildPollable();

        javaMaxMemory = mv.<Integer>create(MAX_MEMORY_ID)
                .name(new TranslatableMessage("java.monitor.JAVA_MAX_MEMORY"))
                .uploadToStore(true)
                .build();
        javaUsedMemory = mv.<Integer>create(USED_MEMORY_ID)
                .name( new TranslatableMessage("java.monitor.JAVA_USED_MEMORY"))
                .build();
        javaFreeMemory = mv.<Integer>create(FREE_MEMORY_ID)
                .name( new TranslatableMessage("java.monitor.JAVA_FREE_MEMORY"))
                .build();

        dbActiveConnections = mv.<Integer>create(DB_ACTIVE_CONNECTIONS_MONITOR_ID).name(new TranslatableMessage("internal.monitor.DB_ACTIVE_CONNECTIONS")).build();
        dbIdleConnections = mv.<Integer>create(DB_IDLE_CONNECTIONS_MONITOR_ID).name(new TranslatableMessage("internal.monitor.DB_IDLE_CONNECTIONS")).build();

        highPriorityActive = mv.<Integer>create(HIGH_PRIORITY_ACTIVE_MONITOR_ID).name(new TranslatableMessage("internal.monitor.MONITOR_HIGH_ACTIVE")).build();
        highPriorityScheduled = mv.<Integer>create(HIGH_PRIORITY_SCHEDULED_MONITOR_ID).name(new TranslatableMessage("internal.monitor.MONITOR_HIGH_SCHEDULED")).build();
        highPriorityWaiting = mv.<Integer>create(HIGH_PRIORITY_WAITING_MONITOR_ID).name(new TranslatableMessage("internal.monitor.MONITOR_HIGH_WAITING")).build();
        mediumPriorityActive = mv.<Integer>create(MEDIUM_PRIORITY_ACTIVE_MONITOR_ID).name(new TranslatableMessage("internal.monitor.MONITOR_MEDIUM_ACTIVE")).build();
        mediumPriorityWaiting = mv.<Integer>create(MEDIUM_PRIORITY_WAITING_MONITOR_ID).name(new TranslatableMessage("internal.monitor.MONITOR_MEDIUM_WAITING")).build();
        lowPriorityActive = mv.<Integer>create(LOW_PRIORITY_ACTIVE_MONITOR_ID).name(new TranslatableMessage("internal.monitor.MONITOR_LOW_ACTIVE")).build();
        lowPriorityWaiting = mv.<Integer>create(LOW_PRIORITY_WAITING_MONITOR_ID).name(new TranslatableMessage("internal.monitor.MONITOR_LOW_WAITING")).build();

        uptime = mv.<Double>create(SYSTEM_UPTIME_MONITOR_ID).name(new TranslatableMessage("internal.monitor.SYSTEM_UPTIME")).build();
        userSessions = mv.<Integer>create(USER_SESSION_MONITOR_ID).name(new TranslatableMessage("internal.monitor.USER_SESSION_COUNT")).build();

        mv.<Double>create(LOAD_AVERAGE_MONITOR_ID).supplier(() -> this.serverInfoService.systemLoadAverage(1)).addTo(monitors).buildPollable();

        mv.<Double>create(OS_CPU_LOAD_PROCESS_ID).supplier(this.serverInfoService::processCpuLoadPercent).addTo(monitors).buildPollable();
        mv.<Double>create(OS_CPU_LOAD_SYSTEM_ID).supplier(this.serverInfoService::systemCpuLoadPercent).addTo(monitors).buildPollable();

        mv.<Long>create(RUNTIME_UPTIME_ID).supplier(() -> {
            return runtimeBean.getUptime() / 1000;
        }).addTo(monitors).buildPollable();
        mv.<Integer>create(CLASS_LOADING_LOADED_ID).supplier(classLoadingBean::getLoadedClassCount).addTo(monitors).buildPollable();
        mv.<Long>create(CLASS_LOADING_TOTAL_LOADED_ID).supplier(classLoadingBean::getTotalLoadedClassCount).addTo(monitors).buildPollable();
        mv.<Long>create(CLASS_LOADING_UNLOADED_ID).supplier(classLoadingBean::getUnloadedClassCount).addTo(monitors).buildPollable();
        mv.<Integer>create(THREAD_COUNT_ID).supplier(threadBean::getThreadCount).addTo(monitors).buildPollable();
        mv.<Integer>create(THREAD_DAEMON_COUNT_ID).supplier(threadBean::getDaemonThreadCount).addTo(monitors).buildPollable();
        mv.<Integer>create(THREAD_PEAK_COUNT_ID).supplier(threadBean::getPeakThreadCount).addTo(monitors).buildPollable();
        mv.<Long>create(THREAD_TOTAL_STARTED_ID).supplier(threadBean::getTotalStartedThreadCount).addTo(monitors).buildPollable();
    }

    @PostConstruct
    private void postConstruct() {
        this.scheduledFuture = scheduledExecutor.scheduleAtFixedRate(() -> {
            executor.execute(this::doPoll);
        }, 0, this.period, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    private void preDestroy() {
        ScheduledFuture<?> scheduledFuture = this.scheduledFuture;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    private void doPoll() {
        ServerStatus status = lifecycle.getServerStatus();
        threads.setValue(status.getThreads());
        idleThreads.setValue(status.getIdleThreads());
        queueSize.setValue(status.getQueueSize());

        if(Common.backgroundProcessing != null){
            highPriorityActive.setValue(Common.backgroundProcessing.getHighPriorityServiceActiveCount());
            highPriorityScheduled.setValue(Common.backgroundProcessing.getHighPriorityServiceScheduledTaskCount());
            highPriorityWaiting.setValue(Common.backgroundProcessing.getHighPriorityServiceQueueSize());

            mediumPriorityActive.setValue(Common.backgroundProcessing.getMediumPriorityServiceActiveCount());
            mediumPriorityWaiting.setValue(Common.backgroundProcessing.getMediumPriorityServiceQueueSize());

            lowPriorityActive.setValue(Common.backgroundProcessing.getLowPriorityServiceActiveCount());
            lowPriorityWaiting.setValue(Common.backgroundProcessing.getLowPriorityServiceQueueSize());
        }

        // Check the stack heights
        int max = 0;
        Collection<StackTraceElement[]> stacks = Thread.getAllStackTraces().values();
        int count = stacks.size();
        for (StackTraceElement[] stack : stacks) {
            if (max < stack.length)
                max = stack.length;
            if (stack.length == 0)
                // Don't include inactive threads
                count--;
        }
        threadCount.setValue(count);
        maxStackHeight.setValue(max);

        if(Common.databaseProxy != null){
            dbActiveConnections.setValue(Common.databaseProxy.getActiveConnections());
            dbIdleConnections.setValue(Common.databaseProxy.getIdleConnections());
        }

        //In MB
        javaMaxMemory.setValue((int)(runtime.maxMemory()/mb));
        javaUsedMemory.setValue((int)(runtime.totalMemory()/mb) -(int)(runtime.freeMemory()/mb));
        javaFreeMemory.setValue(javaMaxMemory.getValue() - javaUsedMemory.getValue());

        //Uptime in HRS
        long uptimeMs = runtimeBean.getUptime();
        Double uptimeHrs = uptimeMs/3600000.0D;
        BigDecimal bd = new BigDecimal(uptimeHrs);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        uptime.setValue(bd.doubleValue());

        //Collect Active User Sessions
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) Common.getRootWebContext();
        if (context != null && context.isActive()) {
            MangoSessionRegistry sessionRegistry = context.getBean(MangoSessionRegistry.class);
            userSessions.setValue(sessionRegistry.getActiveSessionCount());
        } else {
            userSessions.setValue(0);
        }

        long ts = System.currentTimeMillis();
        for (ValueMonitor<?> monitor : monitors) {
            try {
                if (monitor instanceof PollableMonitor) {
                    ((PollableMonitor<?>) monitor).poll(ts);
                }
            } catch (Exception e) {
                log.warn("Failed to poll monitor {}", monitor.getId(), e);
            }
        }
    }

}
