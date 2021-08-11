/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PollingService {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final ReentrantLock pollLock = new ReentrantLock();
    private long pollCount = 0;

    protected abstract void doPoll();

    protected final void tryPoll() {
        boolean locked = pollLock.tryLock();
        try {
            if (locked) {
                pollCount++;
                Long start = null;
                if (log.isDebugEnabled()) {
                    log.debug("Beginning poll {}", pollCount);
                    start = System.nanoTime();
                }
                doPoll();
                if (start != null) {
                    log.debug("Finished poll {}, duration {} ms", pollCount, (System.nanoTime() - start) / 1_000_000);
                }
            } else {
                log.error("Aborted poll. Previous poll {} was still running", pollCount);
            }
        } finally {
            if (locked) {
                pollLock.unlock();
            }
        }
    }

}
