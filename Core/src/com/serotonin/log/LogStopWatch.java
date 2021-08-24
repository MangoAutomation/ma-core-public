/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.log;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Helper class to generate timing log output
 *
 * @author Terry Packer
 */
public class LogStopWatch {
    static final String start = "start[";
    static final String time = "time[";
    static final String closeDuration = " ms] message[";
    static final String close = "] ";

    private final transient Logger logger;
    private long startTime;

    public LogStopWatch() {
        logger = LoggerFactory.getLogger(LogStopWatch.class);
        reset();
    }

    /**
     * Logs an info line to the log/console with the message and the elapsed time, can be called
     * multiple times
     *
     * @param message
     */
    public void stop(Supplier<String> message) {
        logInfo(message, 0);
    }

    /**
     * Logs a line to the log/console with the message and the elapsed time, can be called
     * multiple times.
     *
     * If threshold is greater than 0 then the message will be logged at warning level,
     * otherwise it will be logged at info level.
     *
     * @param message
     * @param threshold message will only be logged if elapsed time is longer than the threshold
     */
    public void stop(Supplier<String> message, long threshold) {
        if (threshold > 0) {
            logWarn(message, threshold);
        } else {
            logInfo(message, 0);
        }
    }

    public void logWarn(Supplier<String> message, long threshold) {
        long duration = getElapsedTime();
        if (logger.isWarnEnabled() && duration >= threshold) {
            logger.warn(createMessage(duration, message));
        }
    }

    public void logInfo(Supplier<String> message, long threshold) {
        long duration = getElapsedTime();
        if (logger.isInfoEnabled() && duration >= threshold) {
            logger.info(createMessage(duration, message));
        }
    }

    public void logDebug(Supplier<String> message, long threshold) {
        long duration = getElapsedTime();
        if (logger.isDebugEnabled() && duration >= threshold) {
            logger.debug(createMessage(duration, message));
        }
    }

    public void logTrace(Supplier<String> message, long threshold) {
        long duration = getElapsedTime();
        if (logger.isTraceEnabled() && duration >= threshold) {
            logger.trace(createMessage(duration, message));
        }
    }

    /**
     * @return elapsed time in ms
     */
    public long getElapsedTime() {
        return (System.nanoTime() - startTime) / 1000000;
    }

    /**
     * Reset the start time to now
     */
    public void reset() {startTime = System.nanoTime();}

    private String createMessage(long duration, Supplier<String> message) {
        StringBuilder builder = new StringBuilder();
        builder.append(time);
        builder.append(duration);
        builder.append(closeDuration);
        builder.append(message.get());
        builder.append(close);
        return builder.toString();
    }
}
