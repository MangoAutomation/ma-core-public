/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.jfree.chart.plot.Marker;

/**
 * @author Matthew Lohbihler
 */
public class PointTimeSeriesCollection {
    private final TimeZone timeZone;
    private List<NumericTimeSeries> numericTimeSeriesCollection;
    private List<DiscreteTimeSeries> discreteTimeSeriesCollection;
    private List<Marker> rangeMarkers;

    public PointTimeSeriesCollection(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public PointTimeSeriesCollection() {
        this.timeZone = TimeZone.getDefault();
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void addNumericTimeSeries(NumericTimeSeries numericTimeSeries) {
        if (numericTimeSeriesCollection == null)
            numericTimeSeriesCollection = new ArrayList<NumericTimeSeries>();
        numericTimeSeriesCollection.add(numericTimeSeries);
    }

    public void addDiscreteTimeSeries(DiscreteTimeSeries discreteTimeSeries) {
        if (discreteTimeSeriesCollection == null)
            discreteTimeSeriesCollection = new ArrayList<DiscreteTimeSeries>();
        discreteTimeSeriesCollection.add(discreteTimeSeries);
    }

    public boolean hasData() {
        return hasNumericData() || hasDiscreteData();
    }

    public boolean hasNumericData() {
        return numericTimeSeriesCollection != null;
    }

    public boolean hasDiscreteData() {
        return discreteTimeSeriesCollection != null;
    }

    public boolean hasMultiplePoints() {
        int count = 0;
        if (numericTimeSeriesCollection != null)
            count += numericTimeSeriesCollection.size();
        if (discreteTimeSeriesCollection != null)
            count += discreteTimeSeriesCollection.size();
        return count > 1;
    }

    public int getNumericSeriesCount() {
        if (numericTimeSeriesCollection == null)
            return 0;
        return numericTimeSeriesCollection.size();
    }

    public NumericTimeSeries getNumericTimeSeries(int index) {
        return numericTimeSeriesCollection.get(index);
    }

    public int getDiscreteValueCount() {
        int count = 0;

        if (discreteTimeSeriesCollection != null) {
            for (DiscreteTimeSeries dts : discreteTimeSeriesCollection)
                count += dts.getDiscreteValueCount();
        }

        return count;
    }

    public int getDiscreteSeriesCount() {
        return discreteTimeSeriesCollection.size();
    }

    public DiscreteTimeSeries getDiscreteTimeSeries(int index) {
        return discreteTimeSeriesCollection.get(index);
    }

    public void addRangeMarker(Marker marker) {
        if (rangeMarkers == null)
            rangeMarkers = new ArrayList<Marker>();
        rangeMarkers.add(marker);
    }

    public List<Marker> getRangeMarkers() {
        return rangeMarkers;
    }
}
