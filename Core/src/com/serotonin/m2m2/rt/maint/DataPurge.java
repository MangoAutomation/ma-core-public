/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.maint;

import java.text.ParseException;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PurgeDefinition;
import com.serotonin.m2m2.module.PurgeFilterDefinition;
import com.serotonin.m2m2.module.definitions.actions.PurgeFilter;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.DataPointVO.LoggingTypes;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycleState;

public class DataPurge {

    private static final Logger log = LoggerFactory.getLogger(DataPurge.class);
    private final SystemSettingsDao systemSettingDao;
    private long runtime;
    private final DataPointDao dataPointDao;
    private final PointValueDao pointValueDao;
    private long deletedSamples;
    private boolean numberDeletedSamplesKnown;
    private long deletedEvents;

    public DataPurge() {
        this(Common.getBean(SystemSettingsDao.class), Common.getBean(DataPointDao.class), Common.getBean(PointValueDao.class));
    }

    public DataPurge(SystemSettingsDao systemSettingDao, DataPointDao dataPointDao, PointValueDao pointValueDao) {
        this.systemSettingDao = systemSettingDao;
        this.dataPointDao = dataPointDao;
        this.pointValueDao = pointValueDao;
    }

    public static void schedule() {
        try {
            Common.backgroundProcessing.schedule(new DataPurgeTask());
        }
        catch (ParseException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    synchronized public void execute(long runtime) {
        this.runtime = runtime;
        executeImpl();
    }

    private void addDeletedSamples(long count) {
        this.numberDeletedSamplesKnown = true;
        deletedSamples += count;
    }

    private void executeImpl() {
        log.info("Data purge started");

        int purgePeriodType = systemSettingDao.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE);
        int purgePeriods = systemSettingDao.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIODS);

        Period period = null;
        try {
            period = TimePeriods.toPeriod(purgePeriodType, purgePeriods);
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported purge period type, should be days, weeks, months or years.");
        }

        log.info("Removing orphaned time-series");
        int deletedTimeSeries = dataPointDao.deleteOrphanedTimeSeries();
        if (deletedTimeSeries > 0) {
            log.info("Removed {} orphaned time-series", deletedTimeSeries);
        }

        if (systemSettingDao.getBooleanValue(SystemSettingsDao.ENABLE_POINT_DATA_PURGE) && pointValueDao.enablePerPointPurge()) {
            log.info("Purging point values on a point-by-point basis. Using data source and data point purge overrides.");

            // Get any filters for the data purge from the modules
            List<PurgeFilter> purgeFilters = new ArrayList<>();
            for(PurgeFilterDefinition pfd : ModuleRegistry.getDefinitions(PurgeFilterDefinition.class))
                purgeFilters.add(pfd.getPurgeFilter());

            // Get the data point information.
            List<DataPointVO> dataPoints = dataPointDao.getAll();
            for (DataPointVO dataPoint : dataPoints)
                purgePoint(dataPoint, purgeFilters, purgePeriodType, purgePeriods);

            pointValueDao.deleteOrphanedPointValues().ifPresent(this::addDeletedSamples);

            if (numberDeletedSamplesKnown) {
                log.info("Point value purge complete, " + deletedSamples + " point values were deleted");
            } else {
                log.info("Point value purge complete, unknown number of point values were deleted");
            }
        } else if (systemSettingDao.getBooleanValue(SystemSettingsDao.ENABLE_POINT_DATA_PURGE) && period != null) {
            log.info("Purging all point values older than {}, Data source and data point purge overrides have been ignored.", period);

            ZonedDateTime before = ZonedDateTime.ofInstant(Instant.ofEpochMilli(runtime), ZoneId.systemDefault())
                    .minus(period);
            try {
                pointValueDao.deletePointValuesBefore(before.toInstant().toEpochMilli())
                        .ifPresent(this::addDeletedSamples);
            } catch (UnsupportedOperationException e) {
                log.debug("purgePointValuesBefore operation is not supported by {}.", pointValueDao.getClass().getSimpleName());
            }
            log.info("Point value purge is complete");
        } else {
            log.info("Point value purge is not enabled, skipping.");
        }

        // Event purge
        eventPurge();

        // Definitions
        for (PurgeDefinition def : ModuleRegistry.getDefinitions(PurgeDefinition.class))
            def.execute(runtime);
    }

    private void purgePoint(DataPointVO dataPoint, List<PurgeFilter> purgeFilters, int purgeType, int purgePeriod) {
        if (dataPoint.getLoggingType() == LoggingTypes.NONE){
            // If there is no logging, then there should be no data, unless logging was just changed to none. In either
            // case, it's ok to delete everything.
            if (Common.runtimeManager.getLifecycleState() == ILifecycleState.RUNNING) {
                Optional<Long> result = Common.runtimeManager.purgeDataPointValues(dataPoint);
                result.ifPresent(this::addDeletedSamples);
                if (result.isPresent() && result.get() > 0) {
                    log.info("Purged all data for data point with id " + dataPoint.getId() + " because it is set to logging type NONE.");
                }
            }
        }
        else {
            // Determine the purging properties to use.
            if (dataPoint.isPurgeOverride()) {
                purgeType = dataPoint.getPurgeType();
                purgePeriod = dataPoint.getPurgePeriod();
            } else {
                // Check the data source level.
                DataSourceVO ds = DataSourceDao.getInstance().get(dataPoint.getDataSourceId());
                if (ds.isPurgeOverride()) {
                    purgeType = ds.getPurgeType();
                    purgePeriod = ds.getPurgePeriod();
                }
            }

            // No matter when this purge actually runs, we want it to act like it's midnight.
            DateTime cutoff = new DateTime(runtime);
            cutoff = DateUtils.truncateDateTime(cutoff, TimePeriods.DAYS);
            cutoff = DateUtils.minus(cutoff, purgeType, purgePeriod);
            if (Common.runtimeManager.getLifecycleState() == ILifecycleState.RUNNING) {
                long millis = cutoff.getMillis();
                for(PurgeFilter pf : purgeFilters)
                    millis = pf.adjustPurgeTime(dataPoint, millis);

                Common.runtimeManager.purgeDataPointValues(dataPoint, cutoff.getMillis())
                        .ifPresent(this::addDeletedSamples);
            }
        }
    }

    /**
     * Purge Events corresponding to the system settings
     */
    private void eventPurge() {
        DateTime cutoffTruncated = DateUtils.truncateDateTime(new DateTime(runtime), TimePeriods.DAYS);

        //Purge All Events at this rate
        DateTime cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.EVENT_PURGE_PERIODS));
        this.deletedEvents = Common.eventManager.purgeEventsBefore(cutoff.getMillis());

        //Purge Data Point Events
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(), EventTypeNames.DATA_POINT);

        //Purge the Data Source Events
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(), EventTypeNames.DATA_SOURCE);

        //Purge the Data Source Events
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(), EventTypeNames.SYSTEM);

        //Purge the Data Source Events
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(), EventTypeNames.PUBLISHER);

        //Purge the Data Source Events
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(), EventTypeNames.AUDIT);

        //Purge Alarm Level NONE
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.NONE_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.NONE);

        //Purge Alarm Level INFORMATION
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.INFORMATION);

        //Purge Alarm Level IMPORTANT
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.IMPORTANT);

        //Purge Alarm Level WARNING
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.WARNING);

        //Purge Alarm Level URGENT
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.URGENT);

        //Purge Alarm Level CRITICAL
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.CRITICAL);

        //Purge Alarm Level LIFE_SAFETY
        cutoff = DateUtils.minus(cutoffTruncated, systemSettingDao.getIntValue(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE),
                systemSettingDao.getIntValue(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.LIFE_SAFETY);

        if (this.deletedEvents > 0)
            log.info("Event purge ended, " + this.deletedEvents + " events deleted");

    }

    public long getDeletedSamples() {
        return deletedSamples;
    }

    public long getDeletedEvents() {
        return deletedEvents;
    }

    public boolean isNumberDeletedSamplesKnown(){
        return numberDeletedSamplesKnown;
    }

    static class DataPurgeTask extends TimerTask {
        DataPurgeTask() throws ParseException {
            // Test trigger for running every 5 minutes.
            //super(new CronTimerTrigger("0 0/5 * * * ?"));
            // Trigger to run at 3:05am every day
            super(new CronTimerTrigger("0 5 3 * * ?"), "Data purge task", "DataPurge", 0);
        }

        @Override
        public void run(long runtime) {
            try {
                new DataPurge().execute(runtime);
            }catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
