/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.quantize;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.view.stats.IValueTime;

/**
 * @deprecated use quantize2 classes instead.
 */
@Deprecated
public class NumericDataQuantizer extends AbstractDataQuantizer {
    private IValueTime startValue;
    private IValueTime minValue;
    private IValueTime maxValue;
    private IValueTime endValue;

    public NumericDataQuantizer(long start, long end, int buckets, DataQuantizerCallback callback) {
        super(start, end, buckets, callback);
    }

    @Override
    protected void periodData(IValueTime vt) {
        double d = vt.getValue().getDoubleValue();
        if (startValue == null)
            startValue = vt;
        if (minValue == null || minValue.getValue().getDoubleValue() > d)
            minValue = vt;
        if (maxValue == null || maxValue.getValue().getDoubleValue() < d)
            maxValue = vt;
        endValue = vt;
    }

    @Override
    protected List<IValueTime> donePeriod(int valueCounter) {
        List<IValueTime> result = new ArrayList<IValueTime>();
        result.add(startValue);
        if (minValue != null)
            result.add(minValue);
        if (minValue != maxValue)
            result.add(maxValue);
        result.add(endValue);

        startValue = null;
        minValue = null;
        maxValue = null;
        endValue = null;

        return result;
    }

    //    
    // private static List<ReportDataValue> quantize(List<ReportDataValue> data, long start, long end, int buckets) {
    // final List<ReportDataValue> result = new ArrayList<ReportDataValue>();
    //        
    // NumericDataQuantizer dq = new NumericDataQuantizer(start, end, buckets, new DataQuantizerCallback() {
    // public void quantizedData(Number value, long time) {
    // result.add(new ReportDataValue(value, time));
    // }
    // });
    // for (ReportDataValue value : data)
    // dq.data((Double)value.getValue(), value.getTime());
    // dq.done();
    //        
    // return result;
    // }
    //    
    // public static void main(String[] args) {
    // List<ReportDataValue> data = new ArrayList<ReportDataValue>();
    // data.add(new ReportDataValue(14d, 1));
    // data.add(new ReportDataValue(13d, 2));
    // data.add(new ReportDataValue(10d, 3));
    // data.add(new ReportDataValue(17d, 4));
    // data.add(new ReportDataValue(18d, 10));
    // data.add(new ReportDataValue(11d, 11));
    // data.add(new ReportDataValue(12d, 15));
    // data.add(new ReportDataValue(13d, 16));
    // data.add(new ReportDataValue(14d, 17));
    // data.add(new ReportDataValue(20d, 20));
    // data.add(new ReportDataValue(25d, 21));
    // data.add(new ReportDataValue(27d, 30));
    // data.add(new ReportDataValue(29d, 50));
    // data.add(new ReportDataValue(30d, 51));
    // data.add(new ReportDataValue(28d, 52));
    // data.add(new ReportDataValue(27d, 53));
    // data.add(new ReportDataValue(26d, 54));
    // data.add(new ReportDataValue(25d, 55));
    // data.add(new ReportDataValue(24d, 56));
    // data.add(new ReportDataValue(23d, 57));
    // data.add(new ReportDataValue(22d, 58));
    // data.add(new ReportDataValue(20d, 90));
    //        
    // System.out.println(quantize(data, 0, 100, 800));
    // System.out.println(quantize(data, 0, 100, 100));
    // System.out.println(quantize(data, 0, 100, 90));
    // System.out.println(quantize(data, 0, 100, 50));
    // System.out.println(quantize(data, 0, 100, 7));
    // System.out.println(quantize(data, 0, 100, 6));
    // System.out.println(quantize(data, 0, 100, 5));
    // System.out.println(quantize(data, 0, 100, 4));
    // System.out.println(quantize(data, 0, 100, 1));
    // System.out.println(quantize(data, 1, 90, 13));
    // }
}
