/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.util.properties.MangoConfigurationWatcher.MangoConfigurationReloadedEvent;

/**
 * @author Jared Wiltshire
 */
@Component
@ConditionalOnProperty("${ssl.on:false}")
public class KeyStoreWatcher {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService executor;
    private final Environment env;
    private final IMangoLifecycle lifecycle;
    private WatchService watchService;
    private ScheduledFuture<?> scheduledTask;
    private KeyStoreParameters params;

    @Autowired
    public KeyStoreWatcher(ScheduledExecutorService scheduledExecutorService, ExecutorService executor,
                           Environment env, IMangoLifecycle lifecycle) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.executor = executor;
        this.env = env;
        this.lifecycle = lifecycle;
    }

    @EventListener
    protected synchronized void envPropertiesReloaded(MangoConfigurationReloadedEvent event) {
        KeyStoreParameters newParams = new KeyStoreParameters();
        boolean paramsChanged = !params.equals(newParams);
        this.params = newParams;

        if (paramsChanged) {
            executor.execute(lifecycle::reloadSslContext);
            if (params.watchFile) {
                this.restartWatchService();
            } else {
                this.stopWatchService();
            }
        }
    }

    @PostConstruct
    protected void init() {
        this.params = new KeyStoreParameters();
        if (params.watchFile) {
            this.restartWatchService();
        }
    }

    @PreDestroy
    protected void destroy() {
        this.stopWatchService();
    }

    private synchronized void restartWatchService() {
        try {
            stopWatchService();
            if (params.keystorePath == null || !Files.isRegularFile(params.keystorePath)) {
                throw new Exception("Key store file does not exist or is not accessible: " + params.keystorePath);
            }
            Path keyStoreDirectory = params.keystorePath.getParent();
            this.watchService = keyStoreDirectory.getFileSystem().newWatchService();
            keyStoreDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            this.scheduledTask = scheduledExecutorService.scheduleWithFixedDelay(this::doCheck, 10, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error initializing the SSL key store file watcher: " + e.getMessage(), e);
        }
    }

    private synchronized void stopWatchService() {
        try {
            if (this.scheduledTask != null) {
                this.scheduledTask.cancel(false);
                this.scheduledTask = null;
            }

            if (this.watchService != null) {
                try {
                    this.watchService.close();
                    this.watchService = null;
                } catch (ClosedWatchServiceException e1) {
                    // no problem, already closed
                }
            }
        } catch (Exception e) {
            log.error("Error terminating the SSL key store file watcher: " + e.getMessage(), e);
        }
    }

    private synchronized void doCheck() {
        try {
            WatchKey key = watchService.poll();
            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path path = (Path) event.context();
                    if (path.equals(params.keystorePath.getFileName())) {
                        executor.execute(lifecycle::reloadSslContext);
                        break;
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            log.error("Error watching SSL key store file", e);
        }
    }

    private class KeyStoreParameters {
        final Path keystorePath;
        final String keystorePassword;
        final String keyPassword;
        final boolean watchFile;

        private KeyStoreParameters() {
            String keyStore = env.getProperty("ssl.keystore.location");
            this.keystorePath = keyStore != null ? Common.MA_DATA_PATH.resolve(keyStore).normalize() : null;
            this.keystorePassword = env.getProperty("ssl.keystore.password");
            this.keyPassword = env.getProperty("ssl.key.password");
            this.watchFile = env.getProperty("ssl.keystore.watchFile", Boolean.class, false);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyStoreParameters that = (KeyStoreParameters) o;
            return watchFile == that.watchFile &&
                    Objects.equals(keystorePath, that.keystorePath) &&
                    Objects.equals(keystorePassword, that.keystorePassword) &&
                    Objects.equals(keyPassword, that.keyPassword);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keystorePath, keystorePassword, keyPassword, watchFile);
        }
    }
}
