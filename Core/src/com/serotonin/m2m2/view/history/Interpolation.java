package com.serotonin.m2m2.view.history;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.MutableDateTime;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

public class Interpolation {
    public static List<PointValueTime> interpolate(long startTime, long endTime, long interval,
            PointValueTime preValue, List<PointValueTime> pvts, PointValueTime postValue) {
        List<PointValueTime> result = new ArrayList<PointValueTime>();

        int index = 0;
        long currentTime = startTime;
        PointValueTime prevPvt = preValue;
        while (currentTime < endTime) {
            // Fast forward in the value list until we catch up to current time.
            while (index < pvts.size() && pvts.get(index).getTime() < currentTime) {
                prevPvt = pvts.get(index);
                index++;
            }

            if (index >= pvts.size())
                // Ran out of values
                result.add(interpolate(currentTime, prevPvt, postValue));
            else {
                if (pvts.get(index).getTime() == currentTime)
                    // No interpolation necessary
                    result.add(pvts.get(index));
                else
                    // Next value is in the future.
                    result.add(interpolate(currentTime, prevPvt, pvts.get(index)));
            }

            currentTime += interval;
        }

        return result;
    }

    public static List<PointValueTime> updateYear(List<PointValueTime> pvts, int year) {
        List<PointValueTime> result = new ArrayList<PointValueTime>(pvts.size());
        MutableDateTime mdt = new MutableDateTime();
        for (PointValueTime pvt : pvts) {
            mdt.setMillis(pvt.getTime());
            mdt.setYear(year);
            result.add(new PointValueTime(pvt.getValue(), mdt.getMillis()));
        }
        return result;
    }

    private static PointValueTime interpolate(long time, PointValueTime from, PointValueTime to) {
        if (from != null && time == from.getTime())
            return from;
        if (to != null && time == to.getTime())
            return to;

        if (from == null && to == null)
            return new PointValueTime(0, time);

        if (from == null)
            return new PointValueTime(to.getDoubleValue(), time);

        if (to == null)
            return new PointValueTime(from.getDoubleValue(), time);

        double valueDiff = to.getDoubleValue() - from.getDoubleValue();
        long timeDiff = to.getTime() - from.getTime();
        double value = ((double) time - from.getTime()) / timeDiff * valueDiff + from.getDoubleValue();
        return new PointValueTime(value, time);
    }

    public static void main(String[] args) {
        test1();
        test2();
        test3();
    }

    private static void test1() {
        List<PointValueTime> pvts = new ArrayList<PointValueTime>();
        pvts.add(new PointValueTime(2, 1000000));
        pvts.add(new PointValueTime(4, 1001000));
        pvts.add(new PointValueTime(5, 1002000));
        pvts.add(new PointValueTime(6, 1004000));
        pvts.add(new PointValueTime(3, 1005000));
        pvts.add(new PointValueTime(2, 1006000));
        pvts.add(new PointValueTime(3, 1010000));
        pvts.add(new PointValueTime(3, 1011000));
        pvts.add(new PointValueTime(8, 1011500));
        pvts.add(new PointValueTime(9, 1011600));
        pvts.add(new PointValueTime(7, 1015500));
        pvts.add(new PointValueTime(4, 1018000));

        List<PointValueTime> filled = interpolate(1000000, 1020000, 1000, new PointValueTime(3, 999000), pvts,
                new PointValueTime(3, 1025000));

        ensure(filled, 0, 2, 1000000);
        ensure(filled, 1, 4, 1001000);
        ensure(filled, 2, 5, 1002000);
        ensure(filled, 3, 5.5, 1003000);
        ensure(filled, 4, 6, 1004000);
        ensure(filled, 5, 3, 1005000);
        ensure(filled, 6, 2, 1006000);
        ensure(filled, 7, 2.25, 1007000);
        ensure(filled, 8, 2.5, 1008000);
        ensure(filled, 9, 2.75, 1009000);
        ensure(filled, 10, 3, 1010000);
        ensure(filled, 11, 3, 1011000);
        ensure(filled, 12, 8.794871794871796, 1012000);
        ensure(filled, 13, 8.282051282051283, 1013000);
        ensure(filled, 14, 7.769230769230769, 1014000);
        ensure(filled, 15, 7.256410256410256, 1015000);
        ensure(filled, 16, 6.4, 1016000);
        ensure(filled, 17, 5.2, 1017000);
        ensure(filled, 18, 4, 1018000);
        ensure(filled, 19, 3.857142857142857, 1019000);
    }

    private static void test2() {
        List<PointValueTime> pvts = new ArrayList<PointValueTime>();
        List<PointValueTime> filled = interpolate(1000000, 1010000, 1000, null, pvts, null);

        ensure(filled, 0, 0, 1000000);
        ensure(filled, 1, 0, 1001000);
        ensure(filled, 2, 0, 1002000);
        ensure(filled, 3, 0, 1003000);
        ensure(filled, 4, 0, 1004000);
        ensure(filled, 5, 0, 1005000);
        ensure(filled, 6, 0, 1006000);
        ensure(filled, 7, 0, 1007000);
        ensure(filled, 8, 0, 1008000);
        ensure(filled, 9, 0, 1009000);
    }

    private static void test3() {
        List<PointValueTime> pvts = new ArrayList<PointValueTime>();
        pvts.add(new PointValueTime(3, 1003000));
        List<PointValueTime> filled = interpolate(1000000, 1005000, 1000, null, pvts, null);

        ensure(filled, 0, 3, 1000000);
        ensure(filled, 1, 3, 1001000);
        ensure(filled, 2, 3, 1002000);
        ensure(filled, 3, 3, 1003000);
        ensure(filled, 4, 3, 1004000);
    }

    private static void ensure(List<PointValueTime> pvts, int index, double value, long time) {
        if (index >= pvts.size())
            throw new RuntimeException("Index " + index + " is out of range");
        PointValueTime pvt = pvts.get(index);
        if (pvt.getDoubleValue() != value)
            throw new RuntimeException("Index " + index + " value is incorrect");
        if (pvt.getTime() != time)
            throw new RuntimeException("Index " + index + " time is incorrect");
    }
}
