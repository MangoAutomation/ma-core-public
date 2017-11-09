/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    final transient Log logger;
    final long startTime;

    public LogStopWatch() {
        logger = LogFactory.getLog(LogStopWatch.class);
        startTime = System.nanoTime();
    }

    /**
     * Logs an info line to the log/console with the message and the elapsed time, can be called
     * multiple times
     * 
     * @param text
     */
    public void stop(String text) {
        logInfo(text, 0);
    }
    
    /**
     * Logs a line to the log/console with the message and the elapsed time, can be called
     * multiple times.
     * 
     * If threshold is greater than 0 then the message will be logged at warning level,
     * otherwise it will be logged at info level.
     * 
     * @param text
     * @param threshold message will only be logged if elapsed time is longer than the threshold
     */
    public void stop(String text, long threshold) {
        if (threshold > 0) {
            logWarn(text, threshold);
        } else {
            logInfo(text, 0);
        }
    }

    public void logWarn(String message, long threshold) {
        long duration = getElapsedTime();
        if (duration >= threshold) {
            logger.warn(createMessage(duration, message));
        }
    }

    public void logInfo(String message, long threshold) {
        long duration = getElapsedTime();
        if (duration >= threshold) {
            logger.info(createMessage(duration, message));
        }
    }

    public void logDebug(String message, long threshold) {
        long duration = getElapsedTime();
        if (duration >= threshold) {
            logger.debug(createMessage(duration, message));
        }
    }

    public void logTrace(String message, long threshold) {
        long duration = getElapsedTime();
        if (duration >= threshold) {
            logger.trace(createMessage(duration, message));
        }
    }

    /**
     * @return elapsed time in ms
     */
    public long getElapsedTime() {
        return (System.nanoTime() - startTime) / 1000000;
    }
    
    private String createMessage(long duration, String message) {
        StringBuilder builder = new StringBuilder();
        builder.append(time);
        builder.append(duration);
        builder.append(closeDuration);
        builder.append(message);
        builder.append(close);
        return builder.toString();
    }
}
