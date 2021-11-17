/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.util.properties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Watches the Mango configuration file for changes (mango.properties, formerly env.properties)
 * @author Jared Wiltshire
 */
@Component
public class MangoConfigurationWatcher {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final DefaultMangoProperties mangoProperties;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService executorService;
    private final ApplicationEventPublisher eventPublisher;

    private WatchService watchService;
    private ScheduledFuture<?> scheduledTask;

    @Autowired
    public MangoConfigurationWatcher(MangoProperties mangoProperties, ScheduledExecutorService scheduledExecutorService,
                                     ExecutorService executorService, ApplicationEventPublisher eventPublisher) {

        this.mangoProperties = mangoProperties instanceof DefaultMangoProperties ? (DefaultMangoProperties) mangoProperties : null;
        this.scheduledExecutorService = scheduledExecutorService;
        this.executorService = executorService;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    protected synchronized void init() {
        if (mangoProperties != null && mangoProperties.getBoolean("properties.reloading", true)) {
            Path path = mangoProperties.getEnvPropertiesPath();
            Path parent = path.getParent();
            try {
                if (Files.isDirectory(parent)) {
                    this.watchService = parent.getFileSystem().newWatchService();
                    parent.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
                    this.scheduledTask = scheduledExecutorService.scheduleWithFixedDelay(this::doCheck, 10, 10, TimeUnit.SECONDS);
                }
            } catch (IOException e) {
                log.error("Can't watch config file for changes", e);
            }
        }
    }

    @PreDestroy
    protected synchronized void destroy() throws IOException {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            this.scheduledTask = null;
        }
        if (watchService != null) {
            watchService.close();
            this.watchService = null;
        }
    }

    private synchronized void doCheck() {
        try {
            WatchKey key = watchService.poll();
            if (key != null) {
                Path path = mangoProperties.getEnvPropertiesPath();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path eventPath = (Path) event.context();
                    if (eventPath.equals(path.getFileName())) {
                        executorService.execute(this::reloadAndFireEvent);
                        break;
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            log.error("Error checking config file for changes", e);
        }
    }

    private void reloadAndFireEvent() {
        try {
            mangoProperties.reload();
            if (log.isInfoEnabled()) {
                log.info("Mango properties file {} reloaded", mangoProperties.getEnvPropertiesPath());
            }
            eventPublisher.publishEvent(new MangoConfigurationReloadedEvent());
        } catch (Exception e) {
            log.error("Error reloading config file", e);
        }
    }

    public class MangoConfigurationReloadedEvent extends ApplicationEvent {
        private MangoConfigurationReloadedEvent() {
            super(MangoConfigurationWatcher.this);
        }
    }
}
