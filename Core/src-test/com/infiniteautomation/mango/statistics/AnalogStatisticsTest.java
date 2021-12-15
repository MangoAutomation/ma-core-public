/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.statistics;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.stats.IValueTime;

public class AnalogStatisticsTest {

    /**
     * Evenly spaced value's of 100 every 100 counts
     *
     */
    protected List<PointValueTime> getEvenlySpacedData() {
        List<PointValueTime> values = new ArrayList<PointValueTime>();
        values.add(new PointValueTime(100.0, 100));
        values.add(new PointValueTime(100.0, 200));
        values.add(new PointValueTime(100.0, 300));
        values.add(new PointValueTime(100.0, 400));
        values.add(new PointValueTime(100.0, 500));
        values.add(new PointValueTime(100.0, 600));
        values.add(new PointValueTime(100.0, 700));
        values.add(new PointValueTime(100.0, 800));
        values.add(new PointValueTime(100.0, 900));
        return values;
    }

    /**
     * One value set to 200 at 500ms in
     *
     */
    protected List<PointValueTime> getMidpointData() {
        List<PointValueTime> values = new ArrayList<PointValueTime>();
        values = new ArrayList<PointValueTime>();
        values.add(new PointValueTime(200.0, 500));
        return values;
    }

    @Test
    public void testDeltaMidpoint() {
        long periodStart = 0;
        long periodEnd = 1000;
        PointValueTime startValue = new PointValueTime(0D, periodStart);

        List<PointValueTime> values = getMidpointData();
        AnalogStatistics s;

        // Test no values at all
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getDelta(), 0.001D);

        // Test only start value
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(0D, s.getDelta(), 0.001D);

        // Test only end value
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getDelta(), 0.001D);

        // Test no end
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(200D, s.getDelta(), 0.001D);

        // Test no start.
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(0D, s.getDelta(), 0.001D);

        // Test Have all 0 for 500 counts, 200 for 500 counts
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(200D, s.getDelta(), 0.001D);

        // Run Tests again with different start/ends
        startValue = new PointValueTime(50D, periodStart);


        // Test no values at all
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getDelta(), 0.001D);

        // Test only start value
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(0D, s.getDelta(), 0.001D);

        // Test only end value
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getDelta(), 0.001D);

        // Test no start/end
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(0D, s.getDelta(), 0.001D);

        // Test no end
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(150D, s.getDelta(), 0.001D);

        // Test no start
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(0D, s.getDelta(), 0.001D);

        // Test Have all
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(150D, s.getDelta(), 0.001D);
    }

    /**
     * Test the average statistic using the Midpoint data set
     */
    @Test
    public void testAverageMidpoint() {

        long periodStart = 0;
        long periodEnd = 1000;
        PointValueTime startValue = new PointValueTime(0D, periodStart);

        List<PointValueTime> values = getMidpointData();
        AnalogStatistics s;

        // Test no values at all
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getAverage(), 0.001);

        // Test only start value
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(0D, s.getAverage(), 0.001D);

        // Test only end value
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getAverage(), 0.001);

        // Test no start/end
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(200D, s.getAverage(), 0.001D);

        // Test no end
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(100D, s.getAverage(), 0.001D);

        // Test no start
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(200D, s.getAverage(), 0.001D);

        // Test Have all
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(100D, s.getAverage(), 0.001D);

        // Run Tests again with different start/ends
        startValue = new PointValueTime(50D, periodStart);

        // Test no values at all
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getAverage(), 0.001);

        // Test only start value
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(50D, s.getAverage(), 0.001D);

        // Test only end value
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getAverage(), 0.001);

        // Test no start/end
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(200D, s.getAverage(), 0.001D);

        // Test no end
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(125D, s.getAverage(), 0.001D);

        // Test no start
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(200, s.getAverage(), 0.001D);

        // Test Have all
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(125D, s.getAverage(), 0.001D);
    }

    /**
     * Test the average statistic using the Even Result set
     */
    @Test
    public void testAverageEven() {

        long periodStart = 0;
        long periodEnd = 1000;
        PointValueTime startValue = new PointValueTime(100D, periodStart);

        List<PointValueTime> values = getEvenlySpacedData();
        AnalogStatistics s;

        // Test no values at all
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getAverage(), 0.001);

        // Test only start value
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(100D, s.getAverage(), 0.001D);

        // Test only end value
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getAverage(), 0.001);

        // Test no start/end
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(100D, s.getAverage(), 0.001D);


        // Test no end
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(100D, s.getAverage(), 0.001D);

        // Test no start
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(100D, s.getAverage(), 0.001D);

        // Test Have all
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(100D, s.getAverage(), 0.001D);

    }

    /**
     * Test the integral statistic using the Midpoint data set
     */
    @Test
    public void testIntegralMidpoint() {

        long periodStart = 0;
        long periodEnd = 1000;
        PointValueTime startValue = new PointValueTime(100D, periodStart);

        List<PointValueTime> values = getMidpointData();
        AnalogStatistics s;

        // Note results are scaled in the integral by 1000 (to return integral in seconds)

        // Test no values at all
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getIntegral(), 0.001D);

        // Test only start value
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(100D, s.getIntegral(), 0.001D);

        // Test only end value
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getIntegral(), 0.001D);

        // Test no start/end nothing for 500 counts, 200 for 500 counts
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(100D, s.getIntegral(), 0.001D);

        // Test with values, no end (100 for 500 counts, 200 for 500 counts)
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(150D, s.getIntegral(), 0.001D);

        // Test with values, no start
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(100D, s.getIntegral(), 0.001D);

        // Test Have all
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(150D, s.getIntegral(), 0.001D);


        // Re-run tests with different start/end
        // Run Tests again with different start/ends
        startValue = new PointValueTime(50D, periodStart);

        // Test no values at all
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getIntegral(), 0.001D);

        // Test only start value (50 for 1000 counts)
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(50D, s.getIntegral(), 0.001D);

        // Test only end value
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getIntegral(), 0.001D);

        // Test no start/end nothing for 500 counts, 200 for 500 counts
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(100D, s.getIntegral(), 0.001D);

        // Test with values, no end (50 for 500 counts, 200 for 500 counts)
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(125D, s.getIntegral(), 0.001D);

        // Test with values, no start (200 for 500 counts)
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(100D, s.getIntegral(), 0.001D);

        // Test Have all (50 for 500 Counts and 200 for 500 Counts)
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(125D, s.getIntegral(), 0.001D);

    }

    /**
     * Get the integral statistic using the Even data set
     */
    @Test
    public void testIntegralEven() {

        long periodStart = 0;
        long periodEnd = 1000;
        PointValueTime startValue = new PointValueTime(100D, periodStart);

        List<PointValueTime> values = getEvenlySpacedData();
        AnalogStatistics s;

        // Note results are scaled in the integral / 1000 (to return integral in seconds)

        // Test no values at all
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getIntegral(), 0.001D);

        // Test only start value
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(100D, s.getIntegral(), 0.001D);

        // Test only end value
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getIntegral(), 0.001D);

        // Test no start/end nothing for 100 counts then 100 for 900 counts
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(90D, s.getIntegral(), 0.001D);

        // Test with values, no end. 100 for all 1000 counts
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(100D, s.getIntegral(), 0.001D);

        // Test with values, no start
        s = new AnalogStatistics(periodStart, periodEnd, null, values);
        assertEquals(90D, s.getIntegral(), 0.001D);

        // Test Have all
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);
        assertEquals(100D, s.getIntegral(), 0.001D);

    }


    // Random tests

    @Test
    public void testIntegralNoEndValue() {

        long periodStart = 0;
        long periodEnd = 30l * 24l * 60l * 60l * 1000l; // 30 Days in ms
        AnalogStatistics s;
        List<PointValueTime> values;
        long pointChangeTime;
        double pointChangeValue;
        Double pointStartValue = 0D;

        // Expected Values
        Double expectedIntegral;

        // Test a series of values
        values = new ArrayList<PointValueTime>();
        pointStartValue = 100d;
        pointChangeTime = 25l * 24l * 60l * 60l * 1000l;
        pointChangeValue = 200d;

        long dayInMs = 24l * 60l * 60l * 1000l;
        long time = dayInMs;
        while (time < pointChangeTime) {
            values.add(new PointValueTime(pointStartValue, time));
            time += dayInMs;
        }

        PointValueTime startValue = new PointValueTime(pointStartValue, periodStart);
        values.add(new PointValueTime(pointChangeValue, pointChangeTime));
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);


        expectedIntegral = ((new Double(pointChangeTime) - new Double(periodStart))
                * new Double(pointStartValue)) / 1000D;
        expectedIntegral +=
                (pointChangeValue * (new Double(periodEnd) - new Double(pointChangeTime))) / 1000D;
        assertEquals(expectedIntegral, s.getIntegral(), 0.001D);

    }

    @Test
    public void testIntegral() {

        long periodStart = 0;
        long periodEnd = 30l * 24l * 60l * 60l * 1000l; // 30 Days in ms
        AnalogStatistics s;
        List<PointValueTime> values;
        Double pointStartValue;
        long pointChangeTime;
        double pointChangeValue;
        Double expectedIntegral;

        // No values no start and end values either
        s = new AnalogStatistics(periodStart, periodEnd, null,
                new ArrayList<IValueTime>());
        assertEquals(Double.NaN, s.getIntegral(), 0.001D);

        PointValueTime startValue = new PointValueTime(0d, periodStart);
        // Start with 0 but no other values
        s = new AnalogStatistics(periodStart, periodEnd, startValue, new ArrayList<IValueTime>());
        assertEquals(0D, s.getIntegral(), 0.001D);

        // Start with 0, one change on day 25 at midnight and no end value
        values = new ArrayList<PointValueTime>();
        pointChangeTime = 25l * 24l * 60l * 60l * 1000l;
        pointChangeValue = 200d;
        values.add(new PointValueTime(pointChangeValue, pointChangeTime));

        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);

        expectedIntegral =
                (pointChangeValue * (new Double(periodEnd) - new Double(pointChangeTime))) / 1000D;
        assertEquals(expectedIntegral, s.getIntegral(), 0.001D);

        // Start with 0, one change on day 25 at midnight and have end value of 0
        values = new ArrayList<PointValueTime>();
        pointChangeTime = 25l * 24l * 60l * 60l * 1000l;
        pointChangeValue = 200d;
        pointStartValue = 100d;
        values.add(new PointValueTime(pointChangeValue, pointChangeTime));

        startValue = new PointValueTime(pointStartValue, periodStart);
        s = new AnalogStatistics(periodStart, periodEnd, startValue, values);


        expectedIntegral = ((new Double(pointChangeTime) - new Double(periodStart))
                * new Double(pointStartValue)) / 1000D;
        expectedIntegral +=
                (pointChangeValue * (new Double(periodEnd) - new Double(pointChangeTime))) / 1000D;

        assertEquals(expectedIntegral, s.getIntegral(), 0.001D);

    }

    /**
     * Using a period of 1000 ms test delta boundaries
     */
    @Test
    public void testDeltaEdge() {
        PointValueTime previousValue = new PointValueTime(1d, 99);
        PointValueTime valueInPeriod = new PointValueTime(5d, 500);

        //Test previous value and no values in period
        AnalogStatistics stats = new AnalogStatistics(100, 1100, previousValue, new ArrayList<>());
        assertEquals(0.0d, stats.getDelta(), 0.001);

        //Test previous value and value in period
        ArrayList<PointValueTime> values = new ArrayList<>();
        values.add(valueInPeriod);
        stats = new AnalogStatistics(100, 1100, previousValue, values);
        assertEquals(4d, stats.getDelta(), 0.001);

        //Test value at start and no values
        stats = new AnalogStatistics(100, 1100, new PointValueTime(1d, 100), new ArrayList<>());
        assertEquals(0d, stats.getDelta(), 0.001);

        //Test value at start and value in period
        stats = new AnalogStatistics(100, 1100, new PointValueTime(1d, 100), values);
        assertEquals(4d, stats.getDelta(), 0.001);
    }

}
