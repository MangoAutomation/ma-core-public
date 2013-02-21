package com.serotonin.m2m2.rt.dataImage;

import java.util.Arrays;

import com.serotonin.m2m2.util.LongCompare;
import com.serotonin.web.taglib.DateFunctions;

public class MultiValueTime implements Comparable<MultiValueTime> {
    private final Object[] values;
    private final long time;

    public MultiValueTime(Object[] values, long time) {
        this.values = values;
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public Object[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "MultiValueTime(" + Arrays.toString(values) + " @ " + DateFunctions.getTime(time) + ")";
    }

    @Override
    public int compareTo(MultiValueTime that) {
        return LongCompare.compare(time, that.time);
    }
}
