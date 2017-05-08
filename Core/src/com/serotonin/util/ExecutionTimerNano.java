/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Note: numerical overflow in the nano time values is not considered here!
 * 
 * @author Matthew Lohbihler
 */
public class ExecutionTimerNano {
    private final Map<String, Stat> stats = new LinkedHashMap<>();
    private long startTime;
    private int index;

    public void start() {
        startTime = System.nanoTime();
        index = 0;
    }

    public void mark() {
        mark(Integer.toString(index++));
    }

    public void mark(String id) {
        Stat stat = stats.get(id);
        if (stat == null) {
            stat = new Stat();
            stats.put(id, stat);
        }
        long now = System.nanoTime();
        long diff = now - startTime;
        stat.time += diff;
        stat.count++;

        if (stat.max < diff)
            stat.max = diff;
        if (stat.min > diff)
            stat.min = diff;
        startTime = now;
    }

    public Map<String, Long> getElapsedTimes() {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, Stat> entry : stats.entrySet())
            result.put(entry.getKey(), entry.getValue().time);
        return result;
    }

    public Map<String, Long> getAverageTimes() {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, Stat> entry : stats.entrySet())
            result.put(entry.getKey(), entry.getValue().time / entry.getValue().count);
        return result;
    }

    public Map<String, Integer> getCounts() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Stat> entry : stats.entrySet())
            result.put(entry.getKey(), entry.getValue().count);
        return result;
    }

    public void add(ExecutionTimerNano that) {
        for (Map.Entry<String, Stat> entry : that.stats.entrySet()) {
            Stat stat = stats.get(entry.getKey());
            if (stat == null)
                stats.put(entry.getKey(), new Stat(entry.getValue()));
            else {
                stat.time += entry.getValue().time;
                stat.count += entry.getValue().count;
            }
        }
    }

    @Override
    public String toString() {
        return stats.toString();
    }

    class Stat {
        long time;
        int count;
        long min = Long.MAX_VALUE;
        long max;

        Stat() {
            // no op
        }

        Stat(Stat that) {
            this.time = that.time;
            this.count = that.count;
            this.min = that.min;
            this.max = that.max;
        }

        @Override
        public String toString() {
            return "[elapsed=" + time + ", count=" + count + ", avg=" + (time / count) + ", min=" + min + ", max="
                    + max + "]";
        }
    }
}
