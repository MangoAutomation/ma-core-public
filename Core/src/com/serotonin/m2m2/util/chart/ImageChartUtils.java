/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util.chart;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.TextAnchor;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.util.mindprod.StripEntities;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Matthew Lohbihler
 */
public class ImageChartUtils {
    public static void writeChart(PointTimeSeriesCollection pointTimeSeriesCollection, OutputStream out, int width,
            int height, long from, long to) throws IOException {
        writeChart(pointTimeSeriesCollection, pointTimeSeriesCollection.hasMultiplePoints(), out, width, height, from,
                to);
    }

    public static byte[] getChartData(PointTimeSeriesCollection pointTimeSeriesCollection, int width, int height,
            long from, long to) {
        return getChartData(pointTimeSeriesCollection, pointTimeSeriesCollection.hasMultiplePoints(), width, height,
                from, to);
    }

    public static byte[] getChartData(PointTimeSeriesCollection pointTimeSeriesCollection, boolean showLegend,
            int width, int height, long from, long to) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeChart(pointTimeSeriesCollection, showLegend, out, width, height, from, to);
            return out.toByteArray();
        }
        catch (IOException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public static void writeChart(PointTimeSeriesCollection pointTimeSeriesCollection, boolean showLegend,
            OutputStream out, int width, int height, long from, long to) throws IOException {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, null, showLegend, false, false);
        chart.setBackgroundPaint(SystemSettingsDao.getColour(SystemSettingsDao.CHART_BACKGROUND_COLOUR));

        XYPlot plot = chart.getXYPlot();
        ((DateAxis) plot.getDomainAxis()).setTimeZone(pointTimeSeriesCollection.getTimeZone());
        plot.setBackgroundPaint(SystemSettingsDao.getColour(SystemSettingsDao.PLOT_BACKGROUND_COLOUR));
        Color gridlines = SystemSettingsDao.getColour(SystemSettingsDao.PLOT_GRIDLINE_COLOUR);
        plot.setDomainGridlinePaint(gridlines);
        plot.setRangeGridlinePaint(gridlines);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeStickyZero(false);

        double numericMin = 0;
        double numericMax = 1;

        int numericSeriesCount = pointTimeSeriesCollection.getNumericSeriesCount();
        if (pointTimeSeriesCollection.hasNumericData()) {
            for (int i = 0; i < numericSeriesCount; i++) {
                NumericTimeSeries nts = pointTimeSeriesCollection.getNumericTimeSeries(i);
                AbstractXYItemRenderer renderer;
                if (nts.getPlotType() == DataPointVO.PlotTypes.STEP)
                    renderer = new XYStepRenderer();
                else if (nts.getPlotType() == DataPointVO.PlotTypes.LINE)
                    renderer = new XYLineAndShapeRenderer(true, false);
                else {
                    XYSplineRenderer spline = new XYSplineRenderer();
                    spline.setBaseShapesVisible(false);
                    renderer = spline;
                }

                if (nts.getPaint() != null)
                    renderer.setSeriesPaint(0, nts.getPaint(), false);
                if (nts.getStroke() != null)
                    renderer.setSeriesStroke(0, nts.getStroke(), false);

                plot.setDataset(i, new TimeSeriesCollection(nts.getTimeSeries()));
                plot.setRenderer(i, renderer);
            }

            numericMin = plot.getRangeAxis().getLowerBound();
            numericMax = plot.getRangeAxis().getUpperBound();

            if (!pointTimeSeriesCollection.hasMultiplePoints()) {
                // If this chart displays a single point, check if there should be a range description.
                TimeSeries timeSeries = pointTimeSeriesCollection.getNumericTimeSeries(0).getTimeSeries();
                String desc = timeSeries.getRangeDescription();
                if (!StringUtils.isBlank(desc)) {
                    // Replace any HTML entities with Java equivalents
                    desc = StripEntities.stripHTMLEntities(desc, ' ');
                    plot.getRangeAxis().setLabel(desc);
                }
            }
        }
        else
            plot.getRangeAxis().setVisible(false);

        if (pointTimeSeriesCollection.getRangeMarkers() != null) {
            boolean rangeAdjusted = false;
            for (Marker marker : pointTimeSeriesCollection.getRangeMarkers()) {
                plot.addRangeMarker(marker);
                if (marker instanceof ValueMarker) {
                    ValueMarker vm = (ValueMarker) marker;
                    if (numericMin > vm.getValue()) {
                        numericMin = vm.getValue();
                        rangeAdjusted = true;
                    }
                    if (numericMax < vm.getValue()) {
                        numericMax = vm.getValue();
                        rangeAdjusted = true;
                    }
                }
            }

            if (rangeAdjusted) {
                double adj = (numericMax - numericMin);
                plot.getRangeAxis().setLowerBound(numericMin - adj * plot.getRangeAxis().getLowerMargin());
                plot.getRangeAxis().setUpperBound(numericMax + adj * plot.getRangeAxis().getUpperMargin());
            }
        }

        int discreteValueCount = pointTimeSeriesCollection.getDiscreteValueCount();
        double interval = (numericMax - numericMin) / (discreteValueCount + 1);
        int intervalIndex = 1;

        if (pointTimeSeriesCollection.hasDiscreteData()) {
            for (int i = 0; i < pointTimeSeriesCollection.getDiscreteSeriesCount(); i++) {
                DiscreteTimeSeries dts = pointTimeSeriesCollection.getDiscreteTimeSeries(i);
                XYStepRenderer renderer = new XYStepRenderer();

                TimeSeries ts = new TimeSeries(dts.getName(), null, null);
                for (IValueTime vt : dts.getValueTimes())
                    addMillisecond(ts, vt.getTime(), numericMin
                            + (interval * (dts.getValueIndex(vt.getValue()) + intervalIndex)));

                if (dts.getPaint() != null)
                    renderer.setSeriesPaint(0, dts.getPaint(), false);
                if (dts.getStroke() != null)
                    renderer.setSeriesStroke(0, dts.getStroke(), false);

                plot.setDataset(numericSeriesCount + i,
                        new TimeSeriesCollection(ts, pointTimeSeriesCollection.getTimeZone()));
                plot.setRenderer(numericSeriesCount + i, renderer);

                intervalIndex += dts.getDiscreteValueCount();
            }
        }

        if (from > 0)
            plot.getDomainAxis().setLowerBound(from);
        if (to > 0)
            plot.getDomainAxis().setUpperBound(to);

        if (pointTimeSeriesCollection.hasDiscreteData()) {
            // Add the value annotations.
            double annoX = plot.getDomainAxis().getLowerBound();
            intervalIndex = 1;
            for (int i = 0; i < pointTimeSeriesCollection.getDiscreteSeriesCount(); i++) {
                DiscreteTimeSeries dts = pointTimeSeriesCollection.getDiscreteTimeSeries(i);

                for (int j = 0; j < dts.getDiscreteValueCount(); j++) {
                    XYTextAnnotation anno = new XYTextAnnotation(" " + dts.getValueText(j), annoX, numericMin
                            + (interval * (j + intervalIndex)));
                    if (!pointTimeSeriesCollection.hasNumericData() && intervalIndex + j == discreteValueCount)
                        // This prevents the top label from getting cut off
                        anno.setTextAnchor(TextAnchor.TOP_LEFT);
                    else
                        anno.setTextAnchor(TextAnchor.BOTTOM_LEFT);
                    anno.setPaint(((AbstractRenderer) plot.getRenderer(numericSeriesCount + i)).lookupSeriesPaint(0));
                    plot.addAnnotation(anno);
                }

                intervalIndex += dts.getDiscreteValueCount();
            }
        }

        // Return the image.
        ChartUtilities.writeChartAsPNG(out, chart, width, height);
    }

    // public static void writeChart(TimeSeries timeSeries, OutputStream out, int width, int height) throws IOException
    // {
    // writeChart(new TimeSeriesCollection(timeSeries), false, out, width, height);
    // }
    //    
    // public static void writeChart(TimeSeriesCollection timeSeriesCollection, boolean showLegend, OutputStream out,
    // int width, int height) throws IOException {
    // JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, timeSeriesCollection, showLegend,
    // false, false);
    // chart.setBackgroundPaint(Color.white);
    //        
    // // Change the plot renderer
    // // XYPlot plot = chart.getXYPlot();
    // // XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    // // plot.setRenderer(renderer);
    //        
    // // Return the image.
    // ChartUtilities.writeChartAsPNG(out, chart, width, height);
    // }

    public static void writeChart(HttpServletResponse response, byte[] chartData) throws IOException {
        response.setContentType(getContentType());
        StreamUtils.transfer(new ByteArrayInputStream(chartData), response.getOutputStream());
    }

    public static String getContentType() {
        return "image/x-png";
    }

    public static void addMillisecond(TimeSeries timeSeries, long time, Number value) {
        try {
            timeSeries.add(new Millisecond(new Date(time)), value);
        }
        catch (SeriesException e) { /* duplicate Second. Ignore. */
        }
    }
}
