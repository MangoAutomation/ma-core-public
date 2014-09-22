/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.List;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.view.chart.ChartRenderer;
import com.serotonin.m2m2.view.chart.ImageChartRenderer;
import com.serotonin.m2m2.view.chart.ImageFlipbookRenderer;
import com.serotonin.m2m2.view.chart.StatisticsChartRenderer;
import com.serotonin.m2m2.view.chart.TableChartRenderer;
import com.serotonin.m2m2.view.text.AnalogRenderer;
import com.serotonin.m2m2.view.text.BinaryTextRenderer;
import com.serotonin.m2m2.view.text.MultistateRenderer;
import com.serotonin.m2m2.view.text.MultistateValue;
import com.serotonin.m2m2.view.text.NoneRenderer;
import com.serotonin.m2m2.view.text.PlainRenderer;
import com.serotonin.m2m2.view.text.RangeRenderer;
import com.serotonin.m2m2.view.text.RangeValue;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.view.text.TimeRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

public class DataPointEditDwr extends BaseDwr {
    private DataPointVO getDataPoint() {
        // The user can also end up with this point in their session in the point details page, which only requires
        // read access. So, ensure that any access here is allowed with edit permission.
        User user = Common.getUser();
        DataPointVO dataPoint = user.getEditPoint();
        Permissions.ensureDataSourcePermission(user, dataPoint.getDataSourceId());
        return dataPoint;
    }
    
    @DwrPermission(user = true)
    public ProcessResult ensureEditingPointMatch(int uiPointId){
    	ProcessResult result = new ProcessResult();
    	User user = Common.getUser();
    	DataPointVO dataPoint = user.getEditPoint();
    	if(dataPoint.getId() == uiPointId){
    		result.addData("match",true);
    	}else{
    		
    		result.addData("message", Common.translate("common.dataPointSaveFailed"));
    		result.addData("match", false);
    	}
    	
    	return result;
    }
    
    //
    // Set text renderer
    //
    @DwrPermission(user = true)
    public void setAnalogTextRenderer(String format, String suffix,boolean useUnitAsSuffix) {
        setTextRenderer(new AnalogRenderer(format, suffix,useUnitAsSuffix));
    }

    @DwrPermission(user = true)
    public void setBinaryTextRenderer(String zeroLabel, String zeroColour, String oneLabel, String oneColour) {
        setTextRenderer(new BinaryTextRenderer(zeroLabel, zeroColour, oneLabel, oneColour));
    }

    @DwrPermission(user = true)
    public void setMultistateRenderer(List<MultistateValue> values) {
        MultistateRenderer r = new MultistateRenderer();
        for (MultistateValue v : values)
            r.addMultistateValue(v.getKey(), v.getText(), v.getColour());
        setTextRenderer(r);
    }

    @DwrPermission(user = true)
    public void setNoneRenderer() {
        setTextRenderer(new NoneRenderer());
    }

    @DwrPermission(user = true)
    public void setPlainRenderer(String suffix,boolean useUnitAsSuffix) {
        setTextRenderer(new PlainRenderer(suffix,useUnitAsSuffix));
    }

    @DwrPermission(user = true)
    public void setRangeRenderer(String format, List<RangeValue> values) {
        RangeRenderer r = new RangeRenderer(format);
        for (RangeValue v : values)
            r.addRangeValues(v.getFrom(), v.getTo(), v.getText(), v.getColour());
        setTextRenderer(r);
    }

    @DwrPermission(user = true)
    public void setTimeTextRenderer(String format, int conversionExponent) {
        setTextRenderer(new TimeRenderer(format, conversionExponent));
    }

    private void setTextRenderer(TextRenderer renderer) {
        getDataPoint().setTextRenderer(renderer);
    }

    //
    // Set chart renderer
    //
    @DwrPermission(user = true)
    public void setNoneChartRenderer() {
        setChartRenderer(null);
    }

    @DwrPermission(user = true)
    public void setTableChartRenderer(int limit) {
        setChartRenderer(new TableChartRenderer(limit));
    }

    @DwrPermission(user = true)
    public void setImageChartRenderer(int timePeriod, int numberOfPeriods) {
        setChartRenderer(new ImageChartRenderer(timePeriod, numberOfPeriods));
    }

    @DwrPermission(user = true)
    public void setStatisticsChartRenderer(int timePeriod, int numberOfPeriods, boolean includeSum) {
        setChartRenderer(new StatisticsChartRenderer(timePeriod, numberOfPeriods, includeSum));
    }

    @DwrPermission(user = true)
    public void setImageFlipbookRenderer(int limit) {
        setChartRenderer(new ImageFlipbookRenderer(limit));
    }

    private void setChartRenderer(ChartRenderer renderer) {
        getDataPoint().setChartRenderer(renderer);
    }

    //
    // Data purge
    //
    @DwrPermission(user = true)
    public long purgeNow(int purgeType, int purgePeriod, boolean allData) {
        DataPointVO point = getDataPoint();
        Long count;
        if (allData)
            count = Common.runtimeManager.purgeDataPointValues(point.getId());
        else
            count = Common.runtimeManager.purgeDataPointValues(point.getId(), purgeType, purgePeriod);
        return count;
    }

    //
    // Clear point cache
    //
    @DwrPermission(user = true)
    public void clearPointCache() {
        DataPointVO point = getDataPoint();
        DataPointRT rt = Common.runtimeManager.getDataPoint(point.getId());
        if (rt != null)
            rt.resetValues();
    }

    //
    // Event detectors TODO: This section can be cleaned up since PointEventDetectorVO is now a single class.
    //
    @DwrPermission(user = true)
    public List<PointEventDetectorVO> getEventDetectors() {
        return getDataPoint().getEventDetectors();
    }

    @DwrPermission(user = true)
    public PointEventDetectorVO addEventDetector(int typeId) {
        DataPointVO dp = getDataPoint();
        PointEventDetectorVO ped = new PointEventDetectorVO();
        ped.setXid(new DataPointDao().generateEventDetectorUniqueXid(dp.getId()));
        ped.setAlias("");
        ped.setDetectorType(typeId);

        if (typeId == PointEventDetectorVO.TYPE_STATE_CHANGE_COUNT) {
            ped.setChangeCount(2);
            ped.setDuration(1);
        }
        else if (typeId == PointEventDetectorVO.TYPE_NO_CHANGE)
            ped.setDuration(1);
        else if (typeId == PointEventDetectorVO.TYPE_NO_UPDATE)
            ped.setDuration(1);

        int id = -1;
        synchronized (dp) {
            // Get a unique negative id as an indicator that this is new.
            for (PointEventDetectorVO d : dp.getEventDetectors()) {
                if (d.getId() <= id)
                    id = d.getId() - 1;
            }
            ped.setId(id);
            ped.njbSetDataPoint(dp);
            dp.getEventDetectors().add(ped);
        }
        return ped;
    }

    @DwrPermission(user = true)
    public void deleteEventDetector(int pedId) {
        DataPointVO dp = getDataPoint();
        synchronized (dp) {
            dp.getEventDetectors().remove(getEventDetector(pedId));
        }
    }

    @DwrPermission(user = true)
    public void updateHighLimitDetector(int pedId, String xid, String alias, double limit, boolean notHigher,
    		int useResetLimit, double resetLimit, int duration, int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setLimit(limit);
        ped.setBinaryState(notHigher);
        ped.setMultistateState(useResetLimit);
        ped.setWeight(resetLimit);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateLowLimitDetector(int pedId, String xid, String alias, double limit, boolean notLower,
    		int useResetLimit, double resetLimit, int duration, int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setLimit(limit);
        ped.setBinaryState(notLower);
        ped.setMultistateState(useResetLimit);
        ped.setWeight(resetLimit);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateBinaryStateDetector(int pedId, String xid, String alias, boolean state, int duration,
            int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setBinaryState(state);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateMultistateStateDetector(int pedId, String xid, String alias, int state, int duration,
            int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setMultistateState(state);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updatePointChangeDetector(int pedId, String xid, String alias, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateStateChangeCountDetector(int pedId, String xid, String alias, int count, int duration,
            int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setChangeCount(count);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateNoChangeDetector(int pedId, String xid, String alias, int duration, int durationType,
            int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateNoUpdateDetector(int pedId, String xid, String alias, int duration, int durationType,
            int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateAlphanumericStateDetector(int pedId, String xid, String alias, String state, int duration,
            int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setAlphanumericState(state);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }
    
    @DwrPermission(user = true)
    public void updateAlphanumericRegexStateDetector(int pedId, String xid, String alias, String state, int duration,
            int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setAlphanumericState(state);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updatePositiveCusumDetector(int pedId, String xid, String alias, double limit, double weight,
            int duration, int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setLimit(limit);
        ped.setWeight(weight);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateNegativeCusumDetector(int pedId, String xid, String alias, double limit, double weight,
            int duration, int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setLimit(limit);
        ped.setWeight(weight);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }

    @DwrPermission(user = true)
    public void updateAnalogRangeDetector(int pedId, String xid, String alias, double limit, double weight,
            boolean withinRange, int duration, int durationType, int alarmLevel) {
        PointEventDetectorVO ped = getEventDetector(pedId);
        ped.setXid(xid);
        ped.setAlias(alias);
        ped.setLimit(limit);
        ped.setWeight(weight);
        ped.setBinaryState(withinRange);
        ped.setDuration(duration);
        ped.setDurationType(durationType);
        ped.setAlarmLevel(alarmLevel);
    }
    
    private PointEventDetectorVO getEventDetector(int pedId) {
        DataPointVO dp = getDataPoint();
        for (PointEventDetectorVO ped : dp.getEventDetectors()) {
            if (ped.getId() == pedId) {
                return ped;
            }
        }
        return null;
    }
}
