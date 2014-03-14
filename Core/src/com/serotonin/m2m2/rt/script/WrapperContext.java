/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.util.DateUtils;

/**
 * @author Matthew Lohbihler
 */
public class WrapperContext {
    private final long runtime;

    public WrapperContext(long runtime) {
        this.runtime = runtime;
    }

    public long getRuntime() {
        return runtime;
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
        long to = DateUtils.truncate(runtime, periodType);
        long from = DateUtils.minus(to, periodType, count);
        return to - from;
    }

    public long millisInPast(int periodType) {
        return millisInPast(periodType, 1);
    }

    public long millisInPast(int periodType, int count) {
        long from = DateUtils.minus(runtime, periodType, count);
        return runtime - from;
    }

    @Override
    public String toString() {
        return "{millisInPast(periodType, count), millisInPrev(periodType, count), "
                + "millisInPrevious(periodType, count)}";
    }

    public String getHelp() {
        return toString();
    }
}
