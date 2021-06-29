/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.util.DateUtils;

/**
 * @author Matthew Lohbihler
 */
public class WrapperContext {
    
    private final long runtime; //Wall clock time of execution
    private final long timestamp; //Scheduled time of execution (Point event time)
    private long compute;
    
    public WrapperContext(long runtime) {
        this.runtime = this.timestamp = this.compute = runtime;
    }

    public WrapperContext(long runtime, long timestamp) {
        this.runtime = runtime;
        this.timestamp = timestamp;
        this.compute = timestamp;
    }

    public long getRuntime() {
        return runtime;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public long getComputeTime() {
        return compute;
    }

    public long millisInPrev(int periodType) {
        return millisInPrevious(periodType, 1);
    }

    public long millisInPrevious(int periodType) {
        return millisInPrevious(periodType, 1);
    }

    public long millisInPrev(int periodType, int count) {
        return millisInPrevious(periodType, count);
    }

    public long millisInPrevious(int periodType, int count) {
        long to = DateUtils.truncate(compute, periodType);
        long from = DateUtils.minus(to, periodType, count);
        return to - from;
    }

    public long millisInPast(int periodType) {
        return millisInPast(periodType, 1);
    }

    public long millisInPast(int periodType, int count) {
        long from = DateUtils.minus(compute, periodType, count);
        return compute - from;
    }

    @Override
    public String toString() {
        return "{\nmillisInPast(periodType, count): long,\nmillisInPrev(periodType, count): long,\n"
                + "millisInPrevious(periodType, count): long,\ngetRuntime(): long,\nuseTimestamp(): void,\n"
                + "useRuntime(): void,\n setComputeTime(long): void\n}";
    }

    public String getHelp() {
        return toString();
    }
    
    public void useTimestamp() {
        compute = timestamp;
    }
    
    public void useRuntime() {
        compute = runtime;
    }
    
    public void setComputeTime(long time) {
        compute = time;
    }
}
