/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.stats.ITime;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;


/**
 *
 * The AnalogChangeDetector is used to detect occurrences of point values changing by a given amount within a duration of time.
 *
 * For example a user may want to know if the water pressure drops suddenly but not be interested in a slowly dropping pressure.
 *
 * This detector works by first determining if the user is tracking upward or downward changes.
 *
 * Upward changes:
 * The detector tracks the lowest value during the period of interest.  For non-durational detectors this is the
 * life of the point.  For durational detectors this is during the period.
 * If the current value - lowest value > maxAllowedChange then the event will fire.
 *
 *
 * With duration:
 * We need to track the start and end of the periods and ensure that the 'lowest' value is in the period,
 * discarding older values.  This can be only be done by maintaining a list of previous Point Values and dropping
 * them from the list over time.
 *
 *
 * The configuration fields provided are static for the lifetime of this detector. The state fields vary based on the
 * changing conditions in the system.
 *
 * In particular, the highLimitActive field describes whether the point's value is
 * currently above the high limit or not. The eventActive field describes whether the point's value has been above the
 * high limit for longer than the tolerance duration.
 *
 * @author Terry Packer
 */
public class AnalogChangeDetectorRT extends TimeoutDetectorRT<AnalogChangeDetectorVO> {
    /**
     * State field. Whether the event is currently active or not. This field is used to prevent multiple events being
     * raised during the duration of a single high limit exceed.
     */
    private boolean eventActive;
    private PointValueTime instantValue;

    private double max = Double.NEGATIVE_INFINITY;
    private double min = Double.POSITIVE_INFINITY;
    private long maxTime = Long.MIN_VALUE;
    private long minTime = Long.MIN_VALUE;
    private long latestTime;
    private boolean dirty = false;
    private final long durationMillis;
    private final int valueEventType;

    private final List<PointValueTime> periodValues;

    public AnalogChangeDetectorRT(AnalogChangeDetectorVO vo) {
        super(vo);
        this.durationMillis = Common.getMillis(vo.getDurationType(), vo.getDuration());
        this.valueEventType = vo.getUpdateEvent();
        PointValueDao pvd = Common.getBean(PointValueDao.class);
        long now = Common.timer.currentTimeMillis();
        periodValues = new ArrayList<>();
        DataPointVO dpvo = Common.getBean(DataPointDao.class).get(vo.getSourceId());
        PointValueTime periodStartValue = pvd.getPointValueBefore(dpvo, now - durationMillis + 1).orElse(null);
        if(periodStartValue != null) {
            periodValues.add(periodStartValue);
            latestTime = periodStartValue.getTime();
        }else {
            latestTime = -1;
        }

        if(durationMillis == 0 && valueEventType == AnalogChangeDetectorVO.UpdateEventType.LOGGED_ONLY)
            instantValue = periodStartValue;

        periodValues.addAll(pvd.getPointValues(dpvo, now - durationMillis + 1));

        Iterator<PointValueTime> iter = periodValues.iterator();
        while(iter.hasNext()) {
            PointValueTime pvt = iter.next();
            if(pvt.getDoubleValue() > max) {
                max = pvt.getDoubleValue();
                maxTime = pvt.getTime();
            }
            if(pvt.getDoubleValue() < min) {
                min = pvt.getDoubleValue();
                minTime = pvt.getTime();
            }
            if(pvt.getTime() > latestTime)
                latestTime = pvt.getTime();
        }

    }

    public PointValueTime getInstantValue() {
        return instantValue;
    }

    public List<PointValueTime> getPeriodValues() {
        synchronized(periodValues) {
            return new ArrayList<>(periodValues);
        }
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public long getMinTime() {
        return minTime;
    }

    public boolean isDirty() {
        return dirty;
    }

    @Override
    public TranslatableMessage getMessage() {
        String name = vo.getDataPoint().getExtendedName();
        String prettyLimit = vo.getDataPoint().getTextRenderer().getText(vo.getLimit(), TextRenderer.HINT_SPECIFIC);
        TranslatableMessage durationDescription = getDurationDescription();

        if(durationDescription != null) {
            if(vo.isCheckIncrease() && vo.isCheckDecrease())
                return new TranslatableMessage("event.detector.analogChangePeriod", name, prettyLimit, durationDescription);
            else if(vo.isCheckIncrease())
                return new TranslatableMessage("event.detector.analogIncreasePeriod", name, prettyLimit, durationDescription);
            else if(vo.isCheckDecrease())
                return new TranslatableMessage("event.detector.analogDecreasePeriod", name, prettyLimit, durationDescription);
            else
                throw new ShouldNeverHappenException("Illegal state for analog change detector" + vo.getXid());
        } else {
            if(vo.isCheckIncrease() && vo.isCheckDecrease())
                return new TranslatableMessage("event.detector.analogChange", name, prettyLimit);
            else if(vo.isCheckIncrease())
                return new TranslatableMessage("event.detector.analogIncrease", name, prettyLimit);
            else if(vo.isCheckDecrease())
                return new TranslatableMessage("event.detector.analogDecrease", name, prettyLimit);
            else
                throw new ShouldNeverHappenException("Illegal state for analog change detector" + vo.getXid());
        }
    }

    @Override
    public boolean isEventActive() {
        return eventActive;
    }

    private void pruneValueList(long time) {
        long cutoff = time - durationMillis;
        boolean recomputeMinimum = false, recomputeMaximum = false;

        if(dirty) {
            periodValues.sort(ITime.COMPARATOR);
            latestTime = periodValues.get(periodValues.size()-1).getTime();
            dirty = false;
        }

        while(periodValues.size() > 1) {
            PointValueTime pvt1 = periodValues.get(0);
            PointValueTime pvt2 = periodValues.get(1);
            if(pvt2.getTime() <= cutoff) {
                if(pvt1.getDoubleValue() >= max)
                    recomputeMaximum = true;
                if(pvt1.getDoubleValue() <= min)
                    recomputeMinimum = true;
                periodValues.remove(0);
            } else {
                break;
            }
        }

        recomputeMaximum |= periodValues.size() <= 1;
        recomputeMinimum |= periodValues.size() <= 1;

        if(recomputeMaximum || recomputeMinimum) {
            double newMax = Double.NEGATIVE_INFINITY;
            double newMin = Double.POSITIVE_INFINITY;
            long newMaxTime = Long.MIN_VALUE;
            long newMinTime = Long.MIN_VALUE;
            Iterator<PointValueTime> iter = periodValues.iterator();
            while(iter.hasNext()) {
                PointValueTime pvt = iter.next();
                if(pvt.getDoubleValue() > newMax) {
                    newMax = pvt.getDoubleValue();
                    newMaxTime = pvt.getTime();
                }
                if(pvt.getDoubleValue() < newMin) {
                    newMin = pvt.getDoubleValue();
                    newMinTime = pvt.getTime();
                }
            }
            if(recomputeMaximum) {
                max = newMax;
                maxTime = newMaxTime;
            }
            if(recomputeMinimum) {
                min = newMin;
                minTime = newMinTime;
            }
        }
    }

    private boolean checkNewValue(PointValueTime newValue) {
        boolean active = false;
        if(periodValues.size() > 0) {
            if(vo.isCheckIncrease() && newValue.getDoubleValue() > min + vo.getLimit()) {
                active = true;
            }
            if(vo.isCheckDecrease() && newValue.getDoubleValue() < max - vo.getLimit()) {
                active = true;
            }
        }

        periodValues.add(newValue);
        if(newValue.getTime() > latestTime)
            latestTime = newValue.getTime();
        else
            dirty = true;
        if(newValue.getDoubleValue() > max) {
            max = newValue.getDoubleValue();
            maxTime = newValue.getTime();
        }
        if(newValue.getDoubleValue() < min) {
            min = newValue.getDoubleValue();
            minTime = newValue.getTime();
        }

        return active || (vo.isCheckIncrease() && vo.isCheckDecrease() && max - min > vo.getLimit()) ||
                (vo.isCheckDecrease() && maxTime < minTime && max - min > vo.getLimit()) ||
                (vo.isCheckIncrease() && maxTime > minTime && max - min > vo.getLimit());
    }

    private void handleValue(PointValueTime newValue) {
        boolean raised = false;
        synchronized(periodValues) {
            unscheduleJob();
            pruneValueList(newValue.getTime());
            raised = checkNewValue(newValue);
            scheduleJob(Common.timer.currentTimeMillis() + durationMillis);
        }

        if(raised && !eventActive) {
            raiseEvent(newValue.getTime(), createEventContext());
            eventActive = true;
        } else if(!raised && eventActive) {
            returnToNormal(newValue.getTime());
            eventActive = false;
        }
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        if(valueEventType == AnalogChangeDetectorVO.UpdateEventType.CHANGES_ONLY) {
            if(durationMillis != 0)
                handleValue(newValue);
            else if((newValue.getDoubleValue() < oldValue.getDoubleValue() - vo.getLimit() ||
                    newValue.getDoubleValue() > oldValue.getDoubleValue() + vo.getLimit()))
                raiseEvent(newValue.getTime(), createEventContext());
        }
    }

    @Override
    public void pointLogged(PointValueTime value) {
        if(valueEventType == AnalogChangeDetectorVO.UpdateEventType.LOGGED_ONLY) {
            if(durationMillis != 0)
                handleValue(value);
            else {
                if(instantValue == null)
                    instantValue = value;
                else if((value.getDoubleValue() < instantValue.getDoubleValue() - vo.getLimit() ||
                        value.getDoubleValue() > instantValue.getDoubleValue() + vo.getLimit())) {
                    raiseEvent(value.getTime(), createEventContext());
                }

                if(value.getTime() > instantValue.getTime())
                    instantValue = value;
            }
        }
    }

    @Override
    protected void scheduleTimeoutImpl(long fireTime) {
        synchronized(periodValues) {
            PointValueTime lastValue = null;
            if(periodValues.size() > 0)
                lastValue = periodValues.get(periodValues.size()-1);
            periodValues.clear();
            if(lastValue != null) {
                periodValues.add(lastValue);
                min = max = lastValue.getDoubleValue();
                minTime = maxTime = lastValue.getTime();
            } else {
                max = Double.NEGATIVE_INFINITY;
                min = Double.POSITIVE_INFINITY;
                maxTime = minTime = Long.MIN_VALUE;
            }
            returnToNormal(fireTime);
            eventActive = false;
        }
    }

    @Override
    public String getThreadNameImpl() {
        return "AnalogChange Detector " + this.vo.getXid();
    }

}
