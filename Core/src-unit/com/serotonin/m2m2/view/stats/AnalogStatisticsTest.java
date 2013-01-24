package com.serotonin.m2m2.view.stats;

import java.util.ArrayList;

public class AnalogStatisticsTest {
    // TODO

    //    public static void main(String[] args) {
    //        Double startValue = 10d;
    //        List<PointValueTime> values = new ArrayList<PointValueTime>();
    //        values.add(new PointValueTime(11d, 2000));
    //        values.add(new PointValueTime(12d, 3000));
    //        values.add(new PointValueTime(7d, 4000));
    //        values.add(new PointValueTime(13d, 5000));
    //        values.add(new PointValueTime(18d, 6000));
    //        values.add(new PointValueTime(14d, 8000));
    //
    //        System.out.println(new AnalogStatistics(startValue, values, 1000, 10000));
    //        System.out.println(new AnalogStatistics(startValue, values, 1500, 15000));
    //        System.out.println(new AnalogStatistics((Double) null, values, 1000, 10000));
    //        System.out.println(new AnalogStatistics((Double) null, values, 1500, 15000));
    //        System.out.println(new AnalogStatistics((Double) null, new ArrayList<PointValueTime>(), 1500, 15000));
    //        System.out.println(new AnalogStatistics(startValue, new ArrayList<PointValueTime>(), 1500, 15000));
    //    }

    public static void main(String[] args) {
        AnalogStatistics s = new AnalogStatistics(10000, 20000, (Double) null, new ArrayList<IValueTime>(), null);
        System.out.println(s.getAverage()); // null

        s = new AnalogStatistics(10000, 20000, (Double) null, new ArrayList<IValueTime>(), 20D);
        System.out.println(s.getAverage()); // null

        s = new AnalogStatistics(10000, 20000, 10D, new ArrayList<IValueTime>(), 20D);
        System.out.println(s.getAverage()); // 10.0
    }
}
