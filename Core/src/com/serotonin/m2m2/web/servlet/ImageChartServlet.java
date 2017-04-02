/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.servlet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.converter.UnitConverter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.TimeSeries;

import com.serotonin.InvalidArgumentException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogHighLimitEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogLowLimitEventDetectorDefinition;
import com.serotonin.m2m2.rt.dataImage.PointValueFacade;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.chart.DiscreteTimeSeries;
import com.serotonin.m2m2.util.chart.ImageChartUtils;
import com.serotonin.m2m2.util.chart.NumericTimeSeries;
import com.serotonin.m2m2.util.chart.PointTimeSeriesCollection;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogHighLimitDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogLowLimitDetectorVO;
import com.serotonin.m2m2.util.ColorUtils;

public class ImageChartServlet extends BaseInfoServlet {
    private static final long serialVersionUID = -1;
    private static final long CACHE_PURGE_INTERVAL = 1000 * 60 * 10; // 10 minutes

    private long lastCachePurgeTime = 0;
    private final Map<String, CacheElement> cachedImages = new ConcurrentHashMap<String, CacheElement>();

    /**
     * @TODO(security): Validate the point access against the user. If anonymous, make sure the view allows public
     *                  access to the point. (Need to add view id.)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
    	//Check out Public Graphic Views
    	User user = Common.getUser(request);
        if (user == null){
        	boolean allowView = SystemSettingsDao.getBooleanValue(SystemSettingsDao.ALLOW_ANONYMOUS_CHART_VIEW, false);
        	if(!allowView)
        		return;
        }
    	
        String imageInfo = request.getPathInfo();

        CacheElement ce = cachedImages.get(imageInfo);
        byte[] data;
        if (ce == null) {
            data = getImageData(imageInfo, request);
            if (data == null)
                return;
            cachedImages.put(imageInfo, new CacheElement(data));
        }
        else
            data = ce.getData();

        ImageChartUtils.writeChart(response, data);

        tryCachePurge();
    }

    private final Stroke limitStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0,
            new float[] { 2, 4 }, 0);
    private final Paint lowLimitPaint = new Color(0, 0, 0.7f);
    private final Paint highLimitPaint = new Color(0.7f, 0, 0);

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
                from = System.currentTimeMillis() - Long.parseLong(imageBits[1]);
                to = -1;
                pointIdStart = 2;
            }

            int width = getIntRequestParameter(request, "w", 200);
            int height = getIntRequestParameter(request, "h", 100);

            TimeZone timeZone = Common.getUserTimeZone(Common.getUser(request));

            // Create the datasets
            DataPointVO markerPoint = null;
            int pointCount = 0;
            PointTimeSeriesCollection ptsc = new PointTimeSeriesCollection(timeZone);
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

                    // Get the data.
                    DataPointVO dp = DataPointDao.instance.getDataPoint(dataPointId);
                    if (dp != null && dp.getName() != null) {
                        pointCount++;
                        markerPoint = dp;

                        //Get the Color if there wasn't one provided
                        if(colour == null){
                        	try{
                        		if(dp.getChartColour() != null)
                        			colour = ColorUtils.toColor(dp.getChartColour());
                        	}catch(InvalidArgumentException e){
                        		//Munch it
                        	}
                        }
                        
                        PointValueFacade pointValueFacade = new PointValueFacade(dataPointId);
                        List<PointValueTime> data;
                        if (from == -1 && to == -1)
                            data = pointValueFacade.getPointValues(0);
                        else if (from == -1)
                            data = pointValueFacade.getPointValuesBetween(0, to);
                        else if (to == -1)
                            data = pointValueFacade.getPointValues(from);
                        else
                            data = pointValueFacade.getPointValuesBetween(from, to);

                        if (dp.getPointLocator().getDataTypeId() == DataTypes.NUMERIC) {
                        	TimeSeries ts;
                        	if(dp.isUseRenderedUnit()){
                        		//This works because we enforce that all Units default to the ONE Unit if not used
                        		UnitConverter converter = null;
                        		if(dp.getRenderedUnit() != dp.getUnit())
                        			converter = dp.getUnit().getConverterTo(dp.getRenderedUnit());
	                            ts = new TimeSeries(dp.getExtendedName(), null, dp.getTextRenderer().getMetaText());
	                            double value;
	                            for (PointValueTime pv : data){
	                            	if(converter != null)
	                            		value = converter.convert(pv.getDoubleValue());
	                            	else
	                            		value = pv.getDoubleValue();
	                                ImageChartUtils.addMillisecond(ts, pv.getTime(), value); //pv.getValue().numberValue());
	                            }
                        	}else{
                        		//No renderer, don't need it
	                            ts = new TimeSeries(dp.getExtendedName(), null, dp.getTextRenderer().getMetaText());
	                            for (PointValueTime pv : data){
	                                ImageChartUtils.addMillisecond(ts, pv.getTime(), pv.getValue().numberValue()); //pv.getValue().numberValue());
	                            }                        		
                        	}
                            ptsc.addNumericTimeSeries(new NumericTimeSeries(dp.getPlotType(), ts, colour, null));
                        }
                        else {
                            DiscreteTimeSeries ts = new DiscreteTimeSeries(dp.getExtendedName(), dp.getTextRenderer(), colour,
                                    null);
                            for (PointValueTime pv : data)
                                ts.addValueTime(pv);
                            ptsc.addDiscreteTimeSeries(ts);
                        }
                    }
                }
            }

            if (pointCount == 1) {
                // Only one point. Check for limits to draw as markers.
                for (AbstractPointEventDetectorVO<?> ped : markerPoint.getEventDetectors()) {
                    if (ped.getDefinition().getEventDetectorTypeName().equals(AnalogLowLimitEventDetectorDefinition.TYPE_NAME))
                        ptsc.addRangeMarker(new ValueMarker(((AnalogLowLimitDetectorVO)ped).getLimit(), lowLimitPaint, limitStroke));
                    else if (ped.getDefinition().getEventDetectorTypeName().equals(AnalogHighLimitEventDetectorDefinition.TYPE_NAME))
                        ptsc.addRangeMarker(new ValueMarker(((AnalogHighLimitDetectorVO)ped).getLimit(), highLimitPaint, limitStroke));
                }
            }

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

    private void tryCachePurge() {
        long now = System.currentTimeMillis();
        if (lastCachePurgeTime + CACHE_PURGE_INTERVAL < now) {
            List<String> toBePurged = new ArrayList<String>();
            for (String key : cachedImages.keySet()) {
                CacheElement ce = cachedImages.get(key);
                if (ce.getLastAccessTime() + CACHE_PURGE_INTERVAL < now)
                    toBePurged.add(key);
            }

            for (String key : toBePurged)
                cachedImages.remove(key);

            lastCachePurgeTime = System.currentTimeMillis();
        }
    }

    class CacheElement {
        private long lastAccessTime;
        private final byte[] data;

        CacheElement(byte[] data) {
            this.data = data;
            lastAccessTime = System.currentTimeMillis();
        }

        byte[] getData() {
            lastAccessTime = System.currentTimeMillis();
            return data;
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}
