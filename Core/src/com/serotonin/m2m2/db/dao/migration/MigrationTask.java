/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class MigrationTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(MigrationTask.class);
    private final CompletableFuture<Boolean> finished = new CompletableFuture<>();
    private final String name;
    private final Queue<MigrationSeries> seriesQueue;

    private volatile boolean stopFlag = false;

    public MigrationTask(Queue<MigrationSeries> seriesQueue, int threadId) {
        super();
        this.name = String.format("pv-migration-%03d", threadId);
        this.seriesQueue = seriesQueue;
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName(name);
            if (log.isInfoEnabled()) {
                log.info("Migration task '{}' starting.", name);
            }

            boolean stopped;
            MigrationSeries series;
            while (!(stopped = stopFlag) && (series = seriesQueue.poll()) != null) {
                MigrationStatus status = series.run();
                if (status == MigrationStatus.RUNNING) {
                    seriesQueue.add(series);
                }
            }

            if (stopped) {
                if (log.isWarnEnabled()) {
                    log.warn("Migration task '{}' was stopped.", name);
                }
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Migration task '{}' complete, no more work.", name);
                }
            }

            finished.complete(stopped);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error in migration task '{}', task terminated", name, e);
            }
            finished.completeExceptionally(e);
        } catch (Throwable t) {
            finished.completeExceptionally(t);
            throw t;
        }
    }

    /**
     * Stop this migration task. Can be called multiple times.
     */
    public void stopTask() {
        this.stopFlag = true;
    }

    /**
     * @return future indicating when task actually finishes, result is true if task was manually stopped
     */
    public CompletableFuture<Boolean> getFinished() {
        return finished.copy();
    }

    /**
     * @return task name
     */
    public String getName() {
        return name;
    }
}
