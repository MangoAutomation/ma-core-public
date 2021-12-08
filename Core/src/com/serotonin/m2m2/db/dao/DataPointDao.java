/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Condition;
import org.jooq.Cursor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.SortField;
import org.jooq.exception.NoDataFoundException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.db.query.RQLToConditionWithTagKeys;
import com.infiniteautomation.mango.db.tables.DataPointTags;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.DataSources;
import com.infiniteautomation.mango.db.tables.EventDetectors;
import com.infiniteautomation.mango.db.tables.EventHandlersMapping;
import com.infiniteautomation.mango.db.tables.PointValues;
import com.infiniteautomation.mango.db.tables.TimeSeries;
import com.infiniteautomation.mango.db.tables.UserComments;
import com.infiniteautomation.mango.db.tables.records.DataPointsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.events.DataPointTagsUpdatedEvent;
import com.infiniteautomation.mango.spring.events.StateChangeEvent;
import com.infiniteautomation.mango.spring.events.audit.DeleteAuditEvent;
import com.infiniteautomation.mango.spring.events.audit.ToggleAuditEvent;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.usage.DataPointUsageStatistics;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.dataPoint.DataPointChangeDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DataPointPermissionDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.provider.Providers;
import com.serotonin.util.ILifecycleState;
import com.serotonin.util.SerializationHelper;

/**
 *
 * @author Terry Packer
 *
 */
@Repository()
public class DataPointDao extends AbstractVoDao<DataPointVO, DataPointsRecord, DataPoints> {
    static final Logger LOG = LoggerFactory.getLogger(DataPointDao.class);

    private final DataPointTagsDao dataPointTagsDao;
    private final EventDetectorDao eventDetectorDao;
    private final List<DataPointChangeDefinition> changeDefinitions;

    private static final LazyInitSupplier<DataPointDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(DataPointDao.class);
    });

    private final EventDetectors eventDetectors;
    private final UserComments userComments;
    private final DataSources dataSources;
    private final EventHandlersMapping eventHandlersMapping;
    private final PointValues pointValues;
    private final DataPointPermissionDefinition dataPointPermissionDefinition;

    @Autowired
    private DataPointDao(DaoDependencies dependencies,
            DataPointTagsDao dataPointTagsDao,
                         EventDetectorDao eventDetectorDao,
                         @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataPointPermissionDefinition dataPointPermissionDefinition) {

        super(dependencies, AuditEventType.TYPE_DATA_POINT, DataPoints.DATA_POINTS,
                new TranslatableMessage("internal.monitor.DATA_POINT_COUNT"));

        this.dataPointTagsDao = dataPointTagsDao;
        this.eventDetectorDao = eventDetectorDao;
        this.dataPointPermissionDefinition = dataPointPermissionDefinition;
        this.changeDefinitions = ModuleRegistry.getDataPointChangeDefinitions();

        this.eventDetectors = EventDetectors.EVENT_DETECTORS;
        this.userComments = UserComments.USER_COMMENTS;
        this.dataSources = DataSources.DATA_SOURCES;
        this.eventHandlersMapping = EventHandlersMapping.EVENT_HANDLERS_MAPPING;
        this.pointValues = PointValues.POINT_VALUES;
    }

    /**
     * Get cached instance from Spring Context
     */
    public static DataPointDao getInstance() {
        return springInstance.get();
    }

    //
    //
    // Data Points
    //
    /**
     * Get data points for a data source, no permissions checks are done by this method.
     */
    public List<DataPointVO> getDataPoints(int dataSourceId) {
        return getJoinedSelectQuery()
                .where(table.dataSourceId.eq(dataSourceId))
                .fetch(this::mapRecordLoadRelationalData);
    }

    public Set<Integer> getSeriesIds() {
        return create.select(table.seriesId).from(table).fetchSet(table.seriesId);
    }

    public IntStream streamSeriesIds() {
        return create.select(table.seriesId).from(table).stream().mapToInt(Record1::value1);
    }

    public @Nullable DataPointVO getBySeriesId(int seriesId) {
        return getJoinedSelectQuery()
                .where(table.seriesId.eq(seriesId))
                .fetchOne(this::mapRecordLoadRelationalData);
    }

    /**
     * Get points for runtime in an efficient manner by joining with the event detectors and only returning
     *  data points that are enabled
     */
    public List<DataPointWithEventDetectors> getDataPointsForDataSourceStart(int dataSourceId) {
        List<Field<?>> fields = new ArrayList<>(this.getSelectFields());
        fields.addAll(Arrays.asList(eventDetectors.fields()));

        Select<Record> select = this.joinTables(this.getSelectQuery(fields), null).leftOuterJoin(eventDetectors)
                .on(table.id.eq(eventDetectors.dataPointId))
                .where(table.dataSourceId.eq(dataSourceId).and(table.enabled.eq(boolToChar(true))));

        Map<Integer, DataPointWithEventDetectors> result = new HashMap<>();
        try (Cursor<Record> cursor = select.fetchLazy()) {
            for (Record record : cursor) {
                int id = record.get(table.id);

                DataPointWithEventDetectors dp = result.computeIfAbsent(id, (i) -> {
                    DataPointVO dpvo = this.mapRecord(record);
                    loadRelationalData(dpvo);
                    return new DataPointWithEventDetectors(dpvo, new ArrayList<>());
                });

                AbstractPointEventDetectorVO detector = eventDetectorDao.mapPointEventDetector(record, dp.getDataPoint());
                if (detector != null) {
                    dp.getEventDetectors().add(detector);
                }
            }
        }

        return new ArrayList<>(result.values());
    }

    /**
     * Check licensing before adding a point
     */
    private void checkAddPoint() {
        IMangoLifecycle lifecycle = Providers.get(IMangoLifecycle.class);
        Integer limit = lifecycle.dataPointLimit();
        if(limit != null && this.countMonitor.getValue() >= limit) {
            String licenseType;
            if(Common.license() != null)
                licenseType = Common.license().getLicenseType();
            else
                licenseType = "Free";
            throw new LicenseViolatedException(new TranslatableMessage("license.dataPointLimit", licenseType, limit));
        }
    }

    @Override
    public void insert(DataPointVO vo) {
        checkAddPoint();

        // Create a default text renderer
        if (vo.getTextRenderer() == null)
            vo.defaultTextRenderer();

        super.insert(vo);
    }

    /**
     * Update the enabled column
     *
     * @param dp point for which to update the enabled column
     */
    public void saveEnabledColumn(DataPointVO dp) {
        DataPointVO existing = get(dp.getId());
        saveEnabledColumn(existing, dp.isEnabled());
    }

    /**
     * Update the enabled column
     *
     * @param existing point for which to update the enabled column
     * @param enabled if the point should be enabled or disabled
     * @return updated data point
     */
    public DataPointVO saveEnabledColumn(DataPointVO existing, boolean enabled) {
        create.update(table)
                .set(table.enabled, boolToChar(enabled))
                .where(table.id.eq(existing.getId()))
                .execute();

        DataPointVO result = existing.copy();
        result.setEnabled(enabled);
        this.publishEvent(new DaoEvent<>(this, DaoEventType.UPDATE, result, existing));
        this.publishAuditEvent(new ToggleAuditEvent<>(this.auditEventType, Common.getUser(), result));
        return result;
    }

    /**
     * Is a data point enabled, returns false if point is disabled or DNE.
     */
    public boolean isEnabled(int id) {
        String enabled = create.select(table.enabled)
                .from(table)
                .where(table.id.eq(id))
                .fetchSingle()
                .value1();
        return charToBool(enabled);
    }

    /**
     * Delete the data point for a data source, this must be called within a transaction
     *  and will does not specifically load relational data.  This will clean permissions
     *
     */
    void deleteDataPoints(final int dataSourceId) {
        List<DataPointVO> batch = new ArrayList<>();
        List<Integer> pointIds = new ArrayList<>();
        Set<Integer> permissionIds = new HashSet<>();
        Set<Integer> seriesIds = new HashSet<>();
        int batchSize = databaseProxy.batchDeleteSize();

        //We will not load any relational data from this and rely on the permissionIds being set
        getJoinedSelectQuery().where(table.dataSourceId.eq(dataSourceId)).forEach(record -> {
            try {
                DataPointVO row = mapRecord(record);

                //Don't trigger a lazy load (load relational after this)
                permissionIds.add(row.getReadPermission().getId());
                permissionIds.add(row.getEditPermission().getId());
                permissionIds.add(row.getSetPermission().getId());

                loadRelationalData(row);
                batch.add(row);
                pointIds.add(row.getId());
                seriesIds.add(row.getSeriesId());
            } catch (Exception e) {
                if (e.getCause() instanceof ModuleNotLoadedException) {
                    LOG.error("Data point with xid '" + record.get(table.xid)
                            + "' could not be loaded. Is its module missing?", e.getCause());
                } else {
                    LOG.error(e.getMessage(), e);
                }
            }

            //Check if time to batch
            if (batch.size() == batchSize) {
                deleteBatch(batch, pointIds, permissionIds, seriesIds);
                batch.clear();
                pointIds.clear();
                permissionIds.clear();
                seriesIds.clear();
            }
        });

        if (batch.size() > 0) {
            deleteBatch(batch, pointIds, permissionIds, seriesIds);
        }
    }

    /**
     * Delete a batch of data points in bulk (for performance)
     *
     */
    protected void deleteBatch(List<DataPointVO> batch, List<Integer> ids,
            Set<Integer> permissionIds, Set<Integer> seriesIds) {
        //delete event handler mappings

        create.deleteFrom(eventHandlersMapping)
        .where(eventHandlersMapping.eventTypeName.eq(EventTypeNames.DATA_POINT),
                eventHandlersMapping.eventTypeRef1.in(ids)).execute();

        //delete user comments
        create.deleteFrom(userComments).where(userComments.commentType.eq(2),
                userComments.typeKey.in(ids)).execute();

        //delete event detectors
        create.deleteFrom(eventDetectors).where(eventDetectors.dataPointId.in(ids)).execute();

        for(DataPointVO vo : batch) {
            DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
            if(def != null) {
                def.deleteRelationalData(vo);
            }
            //Run Pre Delete Change definitions
            for (DataPointChangeDefinition changeDefinition : changeDefinitions) {
                changeDefinition.preDelete(vo);
            }
        }

        //delete the points in bulk
        int deleted = create.deleteFrom(table).where(table.id.in(ids)).execute();

        if(this.countMonitor != null) {
            this.countMonitor.addValue(-deleted);
        }

        for(Integer id : permissionIds) {
            permissionService.deletePermissionId(id);
        }

        //Try to delete the timeSeries.id row
        for(Integer seriesId : seriesIds) {
            try {
                this.create.deleteFrom(TimeSeries.TIME_SERIES)
                .where(TimeSeries.TIME_SERIES.id.eq(seriesId))
                .execute();
            }catch(Exception e) {
                //Probably in use by another point (2 points on the same series)
            }
        }

        //Run Post Delete Change definitions
        for(DataPointVO vo : batch) {
            for (DataPointChangeDefinition changeDefinition : changeDefinitions) {
                changeDefinition.postDelete(vo);
            }
        }

        //Audit Events/Dao events
        for(DataPointVO vo : batch) {
            publishEvent(createDaoEvent(DaoEventType.DELETE, vo, null));
            publishAuditEvent(new DeleteAuditEvent<DataPointVO>(this.auditEventType, Common.getUser(), vo));
        }

    }

    /**
     * Count the data points on a data source, used for licensing
     */
    public int countPointsForDataSourceType(String dataSourceType) {
        return create.select(DSL.countDistinct(table.id))
                .from(table)
                .leftJoin(dataSources)
                .on(table.dataSourceId.equal(dataSources.id))
                .where(dataSources.dataSourceType.eq(dataSourceType))
                .fetchSingle()
                .value1();
    }

    /**
     * Get a summary of a data point
     */
    public DataPointSummary getSummary(String xid) {
        return create.select(
                table.id,
                table.xid,
                table.name,
                table.dataSourceId,
                table.deviceName,
                table.readPermissionId,
                table.editPermissionId,
                table.setPermissionId,
                table.seriesId)
                .from(table)
                .where(getXidField().eq(xid))
                .limit(1)
                .fetchOne(this::mapDataPointSummary);
    }

    public DataPointSummary mapDataPointSummary(Record record) {
        DataPointSummary summary = new DataPointSummary();
        summary.setId(record.get(table.id));
        summary.setXid(record.get(table.xid));
        summary.setName(record.get(table.name));
        summary.setDataSourceId(record.get(table.dataSourceId));
        summary.setDeviceName(record.get(table.deviceName));
        summary.setReadPermission(permissionService.get(record.get(table.readPermissionId)));
        summary.setEditPermission(permissionService.get(record.get(table.editPermissionId)));
        summary.setSetPermission(permissionService.get(record.get(table.setPermissionId)));
        summary.setSeriesId(record.get(table.seriesId));
        return summary;
    }

    //
    //
    // Event detectors
    //

    /**
     *
     * Loads the event detectors from the database and sets them on the data point.
     *
     */
    public void loadEventDetectors(DataPointWithEventDetectors dp) {
        dp.setEventDetectors(EventDetectorDao.getInstance().getWithSource(dp.getDataPoint().getId(), dp.getDataPoint()));
    }

    /**
     * Get the count of data points per type of data source
     */
    public List<DataPointUsageStatistics> getUsage() {
        List<DataPointUsageStatistics> result = new ArrayList<>();
        create.select(dataSources.dataSourceType, DSL.count(dataSources.dataSourceType))
                .from(table)
                .leftJoin(dataSources)
                .on(table.dataSourceId.equal(dataSources.id))
                .groupBy(dataSources.dataSourceType)
                .fetch()
                .forEach(record -> {
                    DataPointUsageStatistics usage = new DataPointUsageStatistics();
                    usage.setDataSourceType(record.get(dataSources.dataSourceType));
                    usage.setCount(record.get(DSL.count(dataSources.dataSourceType)));
                    result.add(usage);
                });
        return result;
    }

    @Override
    protected String getXidPrefix() {
        return DataPointVO.XID_PREFIX;
    }

    @Override
    protected Record toRecord(DataPointVO vo) {
        DataPointsRecord record = table.newRecord();

        record.set(table.xid, vo.getXid());
        record.set(table.name, vo.getName());
        record.set(table.data, SerializationHelper.writeObjectToArray(vo));
        record.set(table.dataSourceId, vo.getDataSourceId());
        record.set(table.deviceName, vo.getDeviceName());
        record.set(table.enabled, boolToChar(vo.isEnabled()));
        record.set(table.loggingType, vo.getLoggingType());
        record.set(table.intervalLoggingPeriodType, vo.getIntervalLoggingPeriodType());
        record.set(table.intervalLoggingPeriod, vo.getIntervalLoggingPeriod());
        record.set(table.intervalLoggingType, vo.getIntervalLoggingType());
        record.set(table.tolerance, vo.getTolerance());
        record.set(table.purgeOverride, boolToChar(vo.isPurgeOverride()));
        record.set(table.purgeType, vo.getPurgeType());
        record.set(table.purgePeriod, vo.getPurgePeriod());
        record.set(table.defaultCacheSize, vo.getDefaultCacheSize());
        record.set(table.discardExtremeValues, boolToChar(vo.isDiscardExtremeValues()));
        record.set(table.engineeringUnits, vo.getEngineeringUnits());
        record.set(table.rollup, vo.getRollup());
        record.set(table.dataTypeId, vo.getPointLocator().getDataType().getId());
        record.set(table.settable, boolToChar(vo.getPointLocator().isSettable()));
        record.set(table.jsonData, convertData(vo.getData()));
        record.set(table.seriesId, vo.getSeriesId());
        record.set(table.readPermissionId, vo.getReadPermission().getId());
        record.set(table.editPermissionId, vo.getEditPermission().getId());
        record.set(table.setPermissionId, vo.getSetPermission().getId());

        return record;
    }

    @Override
    public DataPointVO mapRecord(Record record) {
        int id = record.get(table.id);
        String xid = record.get(table.xid);
        String name = record.get(table.name);

        DataPointVO dp = (DataPointVO) SerializationHelper.readObjectInContextFromArray(record.get(table.data));

        dp.setId(id);
        dp.setXid(xid);
        dp.setName(name);
        dp.setDataSourceId(record.get(table.dataSourceId));
        dp.setDeviceName(record.get(table.deviceName));
        dp.setEnabled(charToBool(record.get(table.enabled)));
        dp.setLoggingType(record.get(table.loggingType));
        dp.setIntervalLoggingPeriodType(record.get(table.intervalLoggingPeriodType));
        dp.setIntervalLoggingPeriod(record.get(table.intervalLoggingPeriod));
        dp.setIntervalLoggingType(record.get(table.intervalLoggingType));
        dp.setTolerance(record.get(table.tolerance));
        dp.setPurgeOverride(charToBool(record.get(table.purgeOverride)));
        dp.setPurgeType(record.get(table.purgeType));
        dp.setPurgePeriod(record.get(table.purgePeriod));
        dp.setDefaultCacheSize(record.get(table.defaultCacheSize));
        dp.setDiscardExtremeValues(charToBool(record.get(table.discardExtremeValues)));
        dp.setEngineeringUnits(record.get(table.engineeringUnits));
        dp.setRollup(record.get(table.rollup));

//        // read and discard dataTypeId
//        record.get(table.xyz);
//        // read and discard settable boolean
//        record.get(table.xyz);

        dp.setData(extractDataFromObject(record.get(table.jsonData)));
        dp.setSeriesId(record.get(table.seriesId));

        MangoPermission read = new MangoPermission(record.get(table.readPermissionId));
        dp.supplyReadPermission(() -> read);

        MangoPermission edit = new MangoPermission(record.get(table.editPermissionId));
        dp.supplyEditPermission(() -> edit);

        MangoPermission set = new MangoPermission(record.get(table.setPermissionId));
        dp.supplySetPermission(() -> set);

        // Data source information from join
        dp.setDataSourceName(record.get(dataSources.name));
        dp.setDataSourceXid(record.get(dataSources.xid));
        dp.setDataSourceTypeName(record.get(dataSources.dataSourceType));
        dp.ensureUnitsCorrect();

        return dp;
    }

    /**
     * Inserts a new time series.
     * @return series id for the new time series
     */
    public int insertNewTimeSeries() {
        return this.create.insertInto(TimeSeries.TIME_SERIES)
                .defaultValues()
                .returningResult(TimeSeries.TIME_SERIES.id)
                .fetchOptional()
                .orElseThrow(NoDataFoundException::new)
                .value1();
    }

    /**
     * Does this series id exist in the database
     */
    public boolean seriesIdExists(int id) {
        return this.create.select(DSL.count(TimeSeries.TIME_SERIES.id))
                .from(TimeSeries.TIME_SERIES)
                .where(TimeSeries.TIME_SERIES.id.eq(id))
                .fetchSingle()
                .value1() == 1;
    }

    /**
     * Checks if a Time series is not linked to a datapoint. If true, deletes that time series
     * @return amount of time series deleted
     */
    public int deleteOrphanedTimeSeries() {
        return this.create.deleteFrom(TimeSeries.TIME_SERIES)
                .whereNotExists(DSL.select().from(DataPoints.DATA_POINTS)
                        .where(DataPoints.DATA_POINTS.seriesId.eq(TimeSeries.TIME_SERIES.id)))
                .execute();
    }

    @Override
    public void savePreRelationalData(DataPointVO existing, DataPointVO vo) {
        //Shall we generate a new series ID?
        if (vo.getSeriesId() <= 0) {
            if (existing == null) {
                int seriesId = insertNewTimeSeries();
                vo.setSeriesId(seriesId);
            } else {
                vo.setSeriesId(existing.getSeriesId());
            }
        }

        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission());
        vo.setEditPermission(editPermission);

        MangoPermission setPermission = permissionService.findOrCreate(vo.getSetPermission());
        vo.setSetPermission(setPermission);
    }

    @Override
    public void saveRelationalData(DataPointVO existing, DataPointVO vo) {
        if (existing == null) {
            dataPointTagsDao.insertTagsForDataPoint(vo);
        } else {
            dataPointTagsDao.updateTags(vo);
        }

        DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
        if(def != null) {
            def.saveRelationalData(existing, vo);
        }

        if(existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(vo.getEditPermission())) {
                permissionService.deletePermissions(existing.getEditPermission());
            }
            if(!existing.getSetPermission().equals(vo.getSetPermission())) {
                permissionService.deletePermissions(existing.getSetPermission());
            }
        }
    }

    /**
     * Loads the event detectors, point comments, tags data source and template name
     * Used by getFull()
     */
    @Override
    public void loadRelationalData(DataPointVO vo) {
        vo.supplyTags(() -> dataPointTagsDao.getTagsForDataPointId(vo.getId()));

        //Populate permissions
        MangoPermission read = vo.getReadPermission();
        vo.supplyReadPermission(() -> permissionService.get(read.getId()));
        MangoPermission edit = vo.getEditPermission();
        vo.supplyEditPermission(() -> permissionService.get(edit.getId()));
        MangoPermission set = vo.getSetPermission();
        vo.supplySetPermission(() -> permissionService.get(set.getId()));

        DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
        if(def != null) {
            def.loadRelationalData(vo);
        }
    }

    @Override
    public void deleteRelationalData(DataPointVO vo) {
        //delete event handler mappings
        create.deleteFrom(eventHandlersMapping)
        .where(eventHandlersMapping.eventTypeName.eq(EventTypeNames.DATA_POINT),
                eventHandlersMapping.eventTypeRef1.eq(vo.getId())).execute();

        //delete user comments
        create.deleteFrom(userComments).where(userComments.commentType.eq(2),
                userComments.typeKey.eq(vo.getId())).execute();

        //delete event detectors
        create.deleteFrom(eventDetectors).where(eventDetectors.dataPointId.eq(vo.getId())).execute();

        DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
        if(def != null) {
            def.deleteRelationalData(vo);
        }
    }

    @Override
    public void deletePostRelationalData(DataPointVO vo) {
        //Clean permissions, be aware of the lazy loading problem that deleting a permission
        // here before it is lazily accessed will throw a NotFoundException
        MangoPermission readPermission = vo.getReadPermission();
        MangoPermission editPermission = vo.getEditPermission();
        MangoPermission setPermission = vo.getSetPermission();
        permissionService.deletePermissions(readPermission, editPermission, setPermission);

        //Try to delete the timeSeries.id row
        try {
            this.create.deleteFrom(TimeSeries.TIME_SERIES)
            .where(TimeSeries.TIME_SERIES.id.eq(vo.getSeriesId()))
            .execute();
        }catch(Exception e) {
            //Probably in use by another point (2 points on the same series)
        }
    }

    @Override
    protected <R extends Record> SelectJoinStep<R> joinPermissionsOnField(SelectJoinStep<R> select, PermissionHolder user, Field<Integer> permissionIdField){
        if (this.permissionService.hasPermission(user, dataPointPermissionDefinition.getPermission())) {
            return select;
        } else {
            return super.joinPermissionsOnField(select ,user ,permissionIdField);
        }
    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(dataSources.name);
        fields.add(dataSources.xid);
        fields.add(dataSources.dataSourceType);
        return fields;
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        select = select.leftJoin(dataSources).on(dataSources.id.eq(table.dataSourceId));
        if (conditions instanceof ConditionSortLimitWithTagKeys) {
            Map<String, Field<String>> tagFields = ((ConditionSortLimitWithTagKeys) conditions).getTagFields();
            select = dataPointTagsDao.joinTags(select, table.id, tagFields);
        }
        return select;
    }

    @Override
    protected RQLToCondition createRqlToCondition(Map<String, RQLSubSelectCondition> subSelectMap, Map<String, Field<?>> fieldMap,
            Map<String, Function<Object, Object>> converterMap) {
        return new RQLToConditionWithTagKeys(fieldMap, converterMap);
    }

    /**
     * Query points that the user has edit permission for
     */
    public void customizedEditQuery(ConditionSortLimit conditions, PermissionHolder user, Consumer<DataPointVO> callback) {
        SelectJoinStep<Record> select = getSelectQuery(getSelectFields());
        select = joinTables(select, conditions);
        select = joinPermissionsOnField(select, user, table.editPermissionId);
        customizedQuery(select, conditions.getCondition(), conditions.getSort(), conditions.getLimit(), conditions.getOffset(), callback);
    }

    protected void notifyTagsUpdated(DataPointVO dataPoint) {
        this.eventPublisher.publishEvent(new DataPointTagsUpdatedEvent(this, dataPoint));
    }

    public void notifyStateChanged(DataPointVO vo, ILifecycleState state) {
        eventPublisher.publishEvent(new StateChangeEvent<>(this, state, vo));
    }

    @Override
    protected Map<String, Field<?>> createFieldMap() {
        Map<String, Field<?>> fields = super.createFieldMap();
        fields.put("dataType", table.dataTypeId);

        DataSources dataSources = DataSources.DATA_SOURCES;
        fields.put("dataSourceName", dataSources.name);
        fields.put("dataSourceXid", dataSources.xid);
        fields.put("dataSourceType", dataSources.dataSourceType);
        return fields;
    }

    @Override
    protected Map<String, Function<Object, Object>> createValueConverterMap() {
        Map<String, Function<Object, Object>> converters = super.createValueConverterMap();
        Map<String, Function<Object, Object>> myConverters = new HashMap<>();
        myConverters.put("dataTypeId", value -> {
            if (value instanceof String) {
                DataType type = DataType.fromName((String) value);
                return type == null ? null : type.getId();
            }
            return value;
        });
        return combine(converters, myConverters);
    }

    @Override
    protected void handleMappingException(Exception e, Record record) {
        if (e.getCause() instanceof ModuleNotLoadedException) {
            LOG.error("Data point with xid '" + record.get(table.xid) +
                    "' could not be loaded. Is its module missing?", e.getCause());
        } else {
            LOG.error("Error mapping data point with xid '" + record.get(table.xid) +
                    "' from SQL record", e.getCause());
        }
    }

    @Override
    public QueryBuilder<DataPointVO> buildQuery(PermissionHolder user) {
        return new DataPointQueryBuilder(fieldMap, valueConverterMap,
                csl -> customizedCount(csl, user),
                (csl, consumer) -> customizedQuery(csl, user, consumer));
    }

    /**
     * Get the read permission ID for in memory checks
     * @return permission id or null
     */
    public Integer getReadPermissionId(int dataPointId) {
        return this.create.select(table.readPermissionId).from(table).where(table.id.eq(dataPointId)).fetchOneInto(Integer.class);
    }

    private static class DataPointQueryBuilder extends QueryBuilder<DataPointVO> {

        int tagIndex = 0;
        final Map<String, Field<String>> tagFields = new HashMap<>();

        protected DataPointQueryBuilder(Map<String, Field<?>> fields,
                Map<String, Function<Object, Object>> valueConverter,
                Function<ConditionSortLimit, Integer> countFn,
                BiConsumer<ConditionSortLimit, Consumer<DataPointVO>> queryFn) {
            super(fields, valueConverter, countFn, queryFn);
        }

        @Override
        protected Field<Object> getField(String fieldName) {
            String tagKey;

            if (fieldName.startsWith("tags.")) {
                tagKey = fieldName.substring("tags.".length());
            } else {
                return super.getField(fieldName);
            }

            return getTagField(tagKey).coerce(Object.class);
        }

        private Field<String> getTagField(String tagKey) {
            return tagFields.computeIfAbsent(tagKey, k -> DataPointTags.DATA_POINT_TAGS.as("key" + tagIndex++).tagValue);
        }

        @Override
        protected ConditionSortLimit createConditionSortLimit(Condition condition, List<SortField<?>> sort, Integer limit, Integer offset) {
            return new ConditionSortLimitWithTagKeys(condition, sort, limit, offset, tagFields);
        }
    }
}
