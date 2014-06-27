package com.serotonin.m2m2.util.chart;

import java.awt.Paint;
import java.awt.Stroke;

import org.jfree.data.time.TimeSeries;

public class NumericTimeSeries {
    private final int plotType;
    private final TimeSeries timeSeries;
    private final Paint paint;
    private final Stroke stroke;

    public NumericTimeSeries(int plotType, TimeSeries timeSeries) {
        this(plotType, timeSeries, null, null);
    }

    public NumericTimeSeries(int plotType, TimeSeries timeSeries, Paint paint, Stroke stroke) {
        this.plotType = plotType;
        this.timeSeries = timeSeries;
        this.paint = paint;
        this.stroke = stroke;
    }

    public NumericTimeSeries plainCopy() {
        return new NumericTimeSeries(plotType, timeSeries);
    }

    public int getPlotType() {
        return plotType;
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    public Paint getPaint() {
        return paint;
    }

    public Stroke getStroke() {
        return stroke;
    }
}
