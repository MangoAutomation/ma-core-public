/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.servlet;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jfree.data.time.TimeSeries;

import com.infiniteautomation.mango.spring.dao.DataPointDao;
import com.serotonin.InvalidArgumentException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.chart.DiscreteTimeSeries;
import com.serotonin.m2m2.util.chart.ImageChartUtils;
import com.serotonin.m2m2.util.chart.NumericTimeSeries;
import com.serotonin.m2m2.util.chart.PointTimeSeriesCollection;
import com.serotonin.m2m2.view.quantize.AbstractDataQuantizer;
import com.serotonin.m2m2.view.quantize.BinaryDataQuantizer;
import com.serotonin.m2m2.view.quantize.DiscreteTimeSeriesQuantizerCallback;
import com.serotonin.m2m2.view.quantize.MultistateDataQuantizer;
import com.serotonin.m2m2.view.quantize.NumericDataQuantizer;
import com.serotonin.m2m2.view.quantize.TimeSeriesQuantizerCallback;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.pair.LongPair;
import com.serotonin.provider.Providers;
import com.serotonin.provider.TimerProvider;
import com.serotonin.timer.sync.Synchronizer;
import com.serotonin.m2m2.util.ColorUtils;

/**
 * This class is probably buggy
 * @author Matthew Lohbihler
 *
 */
public class AsyncImageChartServlet extends BaseInfoServlet {
    private static final long serialVersionUID = -1;

    final DataPointDao dataPointDao = DataPointDao.instance;
    final PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();

    /**
     * @TODO(security): Validate the point access against the user. If anonymous, make sure the view allows public
     *                  access to the point. (Need to add view id.)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String imageInfo = request.getPathInfo();
        byte[] data = getImageData(imageInfo, request);
        if (data != null)
            ImageChartUtils.writeChart(response, data);
    }

    private byte[] getImageData(String imageInfo, HttpServletRequest request) throws IOException {
        // The imageInfo contains the timestamp of the last point value, the data point id, and the duration of the
        // chart. The intention is to create a name for the virtual image such that the browser will cache the data
        // and only come here when the data has change. The format of the name is:
        // /{last timestamp}_{duration}_{data point id[|colour]}[_{data point id[|colour]} ...].png
        //
        // From-to charts can also be requested. They are distinguishable by their starting "ft_". Complete format is:
        // /ft_{last timestamp}_{from millis}_{to millis}_{data point id[|colour]}[_{data point id[|colour]} ...].png
        //
        // Width and height can also be added to the image name in case they change dynamically. Add them to the data
        // point id list but prepend them with w and h: "_w500_h250.png"
        //
        // Hex colour definitions need to be prefixed with '0x' instead of '#'.
        try {
            // Remove the / and the .png
            imageInfo = imageInfo.substring(1, imageInfo.length() - 4);

            // Split by underscore.
            String[] imageBits = imageInfo.split("_");

            // Get the data.
            long from, to;
            int pointIdStart;
            if (imageBits[0].equals("ft")) {
                from = Long.parseLong(imageBits[2]);
                to = Long.parseLong(imageBits[3]);
                pointIdStart = 4;
            }
            else {
                from = Common.timer.currentTimeMillis() - Long.parseLong(imageBits[1]);
                to = -1;
                pointIdStart = 2;
            }

            int width = getIntRequestParameter(request, "w", 200);
            int height = getIntRequestParameter(request, "h", 100);

            TimeZone timeZone = Common.getUserTimeZone(Common.getUser(request));

            // Create the datasets
            Synchronizer<PointDataRetriever> tasks = new Synchronizer<PointDataRetriever>();
            List<Integer> dataPointIds = new ArrayList<Integer>();
            for (int i = pointIdStart; i < imageBits.length; i++) {
                if (imageBits[i].startsWith("w"))
                    width = NumberUtils.toInt(imageBits[i].substring(1), width);
                else if (imageBits[i].startsWith("h"))
                    height = NumberUtils.toInt(imageBits[i].substring(1), height);
                else {
                    String dataPointStr = imageBits[i];
                    Color colour = null;
                    int dataPointId;

                    int pipe = dataPointStr.indexOf('|');
                    if (pipe == -1)
                        dataPointId = Integer.parseInt(dataPointStr);
                    else {
                        try {
                            String colourStr = dataPointStr.substring(pipe + 1);
                            if (colourStr.startsWith("0x"))
                                colourStr = "#" + colourStr.substring(2);
                            colour = ColorUtils.toColor(colourStr);
                        }
                        catch (InvalidArgumentException e) {
                            throw new IOException(e);
                        }
                        dataPointId = Integer.parseInt(dataPointStr.substring(0, pipe));
                    }

                    dataPointIds.add(dataPointId);
                    PointDataRetriever pdr = new PointDataRetriever(dataPointId, colour, width * 3, timeZone);
                    tasks.addTask(pdr);
                }
            }

            if (tasks.getSize() == 0)
                return null;

            long start = from;
            long end = to;
            if (from == -1 && to == -1) {
                LongPair sae = pointValueDao.getStartAndEndTime(dataPointIds);
                start = sae.getL1();
                end = sae.getL2();
            }
            else if (from == -1)
                start = pointValueDao.getStartTime(dataPointIds);
            else if (to == -1)
                end = pointValueDao.getEndTime(dataPointIds);

            for (PointDataRetriever pdr : tasks.getTasks())
                pdr.setRange(start, end);

            //Get the timer
            tasks.executeAndWait(Providers.get(TimerProvider.class).getTimer());

            PointTimeSeriesCollection ptsc = new PointTimeSeriesCollection(timeZone);
            for (PointDataRetriever pdr : tasks.getTasks())
                pdr.addToCollection(ptsc);

            return ImageChartUtils.getChartData(ptsc, width, height, from, to);
        }
        catch (StringIndexOutOfBoundsException e) {
            // no op
        }
        catch (NumberFormatException e) {
            // no op
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // no op
        }

        return null;
    }

    class PointDataRetriever implements Runnable, MappedRowCallback<PointValueTime> {
        private final int dataPointId;
        private Color colour;
        private final int imageWidth;
        private final TimeZone timeZone;
        private long from;
        private long to;
        private NumericTimeSeries nts;
        private DiscreteTimeSeries dts;
        private AbstractDataQuantizer quantizer;

        public PointDataRetriever(int dataPointId, Color colour, int imageWidth, TimeZone timeZone) {
            this.dataPointId = dataPointId;
            this.colour = colour;
            this.imageWidth = imageWidth;
            this.timeZone = timeZone;
        }

        public void setRange(long from, long to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public void run() {
            DataPointVO dp = dataPointDao.getDataPoint(dataPointId, false);
            try {
                if (colour == null && !StringUtils.isBlank(dp.getChartColour()))
                    colour = ColorUtils.toColor(dp.getChartColour());
            }
            catch (InvalidArgumentException e) {
                // no op
            }

            int dataType = dp.getPointLocator().getDataTypeId();

            if (dataType == DataTypes.NUMERIC) {
                TimeSeries ts = new TimeSeries(dp.getExtendedName(), null, null);
                nts = new NumericTimeSeries(dp.getPlotType(), ts, colour, null);
                quantizer = new NumericDataQuantizer(from, to, imageWidth, new TimeSeriesQuantizerCallback(ts));
            }
            else if (dataType == DataTypes.MULTISTATE) {
                dts = new DiscreteTimeSeries(dp.getExtendedName(), dp.getTextRenderer(), colour, null);
                quantizer = new MultistateDataQuantizer(from, to, imageWidth, new DiscreteTimeSeriesQuantizerCallback(
                        dts));
            }
            else if (dataType == DataTypes.BINARY) {
                dts = new DiscreteTimeSeries(dp.getExtendedName(), dp.getTextRenderer(), colour, null);
                quantizer = new BinaryDataQuantizer(from, to, imageWidth, new DiscreteTimeSeriesQuantizerCallback(dts));
            }

            // Get the data.
            pointValueDao.getPointValuesBetween(dataPointId, from, to, this);

            // Tell the quantizer we're finished sending data.
            if (quantizer != null)
                quantizer.done();
        }

        @Override
        public void row(PointValueTime pvt, int rowNum) {
            if (quantizer != null)
                quantizer.data(pvt);
        }

        public void addToCollection(PointTimeSeriesCollection ptsc) {
            if (nts != null)
                ptsc.addNumericTimeSeries(nts);
            else
                ptsc.addDiscreteTimeSeries(dts);
        }
    }
}
