/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import static com.serotonin.m2m2.db.dao.DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Condition;
import org.jooq.Cursor;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.exception.NoDataFoundException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.db.query.RQLToConditionWithTagKeys;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.DataSources;
import com.infiniteautomation.mango.db.tables.EventDetectors;
import com.infiniteautomation.mango.db.tables.EventHandlersMapping;
import com.infiniteautomation.mango.db.tables.MintermsRoles;
import com.infiniteautomation.mango.db.tables.PermissionsMinterms;
import com.infiniteautomation.mango.db.tables.TimeSeries;
import com.infiniteautomation.mango.db.tables.UserComments;
import com.infiniteautomation.mango.db.tables.records.DataPointsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.events.DataPointTagsUpdatedEvent;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.usage.DataPointUsageStatistics;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.dataPoint.DataPointChangeDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.provider.Providers;
import com.serotonin.util.SerializationHelper;

/**
 *
 * @author Terry Packer
 *
 */
@Repository()
public class DataPointDao extends AbstractVoDao<DataPointVO, DataPointsRecord, DataPoints> {
    static final Log LOG = LogFactory.getLog(DataPointDao.class);


    private final PermissionService permissionService;
    private final DataPointTagsDao dataPointTagsDao;
    private final EventDetectorDao eventDetectorDao;
    private final List<DataPointChangeDefinition> changeDefinitions;

    private static final LazyInitSupplier<DataPointDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(DataPointDao.class);
    });

    private final EventDetectors eventDetectors;
    private final UserComments userComments;
    private final DataSources dataSources;
    private final EventHandlersMapping eventHandlersMapping = EventHandlersMapping.EVENT_HANDLERS_MAPPING;

    @Autowired
    private DataPointDao(
            PermissionService permissionService,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher,
            DataPointTagsDao dataPointTagsDao,
            EventDetectorDao eventDetectorDao) {

        super(EventType.EventTypeNames.DATA_POINT, DataPoints.DATA_POINTS.as("dp"),
                new TranslatableMessage("internal.monitor.DATA_POINT_COUNT"),
                mapper, publisher);

        this.permissionService = permissionService;
        this.dataPointTagsDao = dataPointTagsDao;
        this.eventDetectorDao = eventDetectorDao;
        this.changeDefinitions = ModuleRegistry.getDataPointChangeDefinitions();
        
        this.eventDetectors = EventDetectors.EVENT_DETECTORS.as("edt");
        this.userComments = UserComments.USER_COMMENTS.as("uc");
        this.dataSources = DataSources.DATA_SOURCES.as("ds");
    }

    /**
     * Get cached instance from Spring Context
     * @return
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
     * @param dataSourceId
     * @return
     */
    public List<DataPointVO> getDataPoints(int dataSourceId) {
        return this.customizedQuery(getJoinedSelectQuery().where(table.dataSourceId.eq(dataSourceId)),
                getListResultSetExtractor());
    }

    /**
     * Get all data point Ids in the table
     * @return
     */
    public List<Integer> getDataPointIds(){
        return queryForList("SELECT id FROM dataPoints" , Integer.class);
    }

    /**
     * Get points for runtime in an efficient manner by joining with the event detectors and only returning
     *  data points that are enabled
     * @param dataSourceId
     * @return
     */
    public List<DataPointWithEventDetectors> getDataPointsForDataSourceStart(int dataSourceId) {
        // TODO Mango 4.0 verify
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
                dp.getEventDetectors().add(detector);
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

    @Override
    public void update(DataPointVO existing, DataPointVO vo) {
        //If have a new data type we will wipe our history
        if (existing.getPointLocator().getDataTypeId() != vo.getPointLocator().getDataTypeId())
            Common.databaseProxy.newPointValueDao().deletePointValues(vo);

        super.update(existing, vo);
    }

    /**
     * Update the enabled column, should only be done via the runtime manager
     * @param dp
     */
    public void saveEnabledColumn(DataPointVO dp) {
        ejt.update("UPDATE dataPoints SET enabled=? WHERE id=?", boolToChar(dp.isEnabled()), dp.getId());
        DataPointVO old = get(dp.getId());
        this.publishEvent(new DaoEvent<DataPointVO>(this, DaoEventType.UPDATE, dp, old));
        AuditEventType.raiseToggleEvent(AuditEventType.TYPE_DATA_POINT, dp);
    }

    /**
     * Is a data point enabled, returns false if point is disabled or DNE.
     * @param id
     * @return
     */
    public boolean isEnabled(int id) {
        return query("select dp.enabled from dataPoints as dp WHERE id=?", new Object[] {id}, new ResultSetExtractor<Boolean>() {

            @Override
            public Boolean extractData(ResultSet rs) throws SQLException, DataAccessException {
                if(rs.next()) {
                    return charToBool(rs.getString(1));
                }else
                    return false;
            }

        });
    }

    /**
     * Delete the data point for a data source, this must be called within a transaction
     *  and will does not specifically load relational data.  This will clean permissions
     *
     * @param dataSourceId
     */
    void deleteDataPoints(final int dataSourceId) {

        //We will not load any relational data from this and rely on the permissionIds being set
        Select<Record> query = getJoinedSelectQuery().where(table.dataSourceId.eq(dataSourceId));
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        query(sql, args.toArray(), new ResultSetExtractor<Void>() {

            @Override
            public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
                RowMapper<DataPointVO> rowMapper = getRowMapper();
                int rowNum = 0;
                List<DataPointVO> batch = new ArrayList<>();
                List<Integer> pointIds = new ArrayList<>();
                Set<Integer> permissionIds = new HashSet<>();
                Set<Integer> seriesIds = new HashSet<>();
                int batchSize = getInBatchSize();

                while (rs.next()) {
                    try {
                        DataPointVO row = rowMapper.mapRow(rs, rowNum);

                        //Don't trigger a lazy load (load relational after this)
                        permissionIds.add(row.getReadPermission().getId());
                        permissionIds.add(row.getEditPermission().getId());
                        permissionIds.add(row.getSetPermission().getId());

                        loadRelationalData(row);
                        batch.add(row);
                        pointIds.add(row.getId());
                        seriesIds.add(row.getSeriesId());

                    }catch (Exception e) {
                        if (e.getCause() instanceof ModuleNotLoadedException) {
                            try {
                                LOG.error("Data point with xid '" + rs.getString("xid")
                                + "' could not be loaded. Is its module missing?", e.getCause());
                            }catch(SQLException e1) {
                                LOG.error(e.getMessage(), e);
                            }
                        }else {
                            LOG.error(e.getMessage(), e);
                        }
                    }finally {
                        rowNum++;
                    }

                    //Check if time to batch
                    if(batch.size() == batchSize) {
                        deleteBatch(batch, pointIds, permissionIds, seriesIds);
                        batch.clear();
                        pointIds.clear();
                        permissionIds.clear();
                        seriesIds.clear();
                    }

                }
                if(batch.size() > 0) {
                    deleteBatch(batch, pointIds, permissionIds, seriesIds);
                }
                return null;
            }
        });
    }

    /**
     * Delete a batch of data points in bulk (for performance)
     *
     * @param batch
     * @param ids
     * @param permissionIds
     * @param seriesIds
     */
    protected void deleteBatch(List<DataPointVO> batch, List<Integer> ids,
            Set<Integer> permissionIds, Set<Integer> seriesIds) {
        //delete event handler mappings

        create.deleteFrom(eventHandlersMapping)
        .where(eventHandlersMapping.eventTypeName.eq(EventType.EventTypeNames.DATA_POINT),
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
            AuditEventType.raiseDeletedEvent(this.typeName, vo);
            publishEvent(createDaoEvent(DaoEventType.DELETE, vo, null));
        }

    }

    /**
     * Use to raise events after the delete transaction of a data source
     *
     * @param points - deleted points
     */
    void raiseDeletedEvents(List<DataPointVO> points) {
        for (DataPointVO dp : points) {
            this.publishEvent(new DaoEvent<DataPointVO>(this, DaoEventType.DELETE, dp));
            AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_DATA_POINT, dp);
            this.countMonitor.decrement();
        }
    }

    /**
     * Count the data points on a data source, used for licensing
     * @param dataSourceType
     * @return
     */
    public int countPointsForDataSourceType(String dataSourceType) {
        return ejt.queryForInt("SELECT count(DISTINCT dp.id) FROM dataPoints dp LEFT JOIN dataSources ds ON dp.dataSourceId=ds.id "
                + "WHERE ds.dataSourceType=?", new Object[] { dataSourceType }, 0);
    }

    /**
     * Get a summary of a data point
     * @param xid
     * @return
     */
    public DataPointSummary getSummary(String xid) {
        Select<?> query = this.joinTables(this.create.select(
                table.id,
                table.xid,
                table.name,
                table.dataSourceId,
                table.deviceName,
                table.readPermissionId,
                table.editPermissionId,
                table.setPermissionId,
                table.seriesId)
                .from(table), null).where(getXidField().eq(xid)).limit(1);

        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        return this.ejt.query(sql, args.toArray(new Object[0]),
                (rs) -> {
                    if(rs.next()) {
                        DataPointSummary summary = new DataPointSummary();
                        summary.setId(rs.getInt(1));
                        summary.setXid(rs.getString(2));
                        summary.setName(rs.getString(3));
                        summary.setDataSourceId(rs.getInt(4));
                        summary.setDeviceName(rs.getString(5));
                        summary.setReadPermission(permissionService.get(rs.getInt(6)));
                        summary.setEditPermission(permissionService.get(rs.getInt(7)));
                        summary.setSetPermission(permissionService.get(rs.getInt(8)));
                        summary.setSeriesId(rs.getInt(9));
                        return summary;
                    }else {
                        return null;
                    }
                });
    }

    //
    //
    // Event detectors
    //

    /**
     *
     * Loads the event detectors from the database and sets them on the data point.
     *
     * @param dp
     */
    public void loadEventDetectors(DataPointWithEventDetectors dp) {
        dp.setEventDetectors(EventDetectorDao.getInstance().getWithSource(dp.getDataPoint().getId(), dp.getDataPoint()));
    }

    /**
     * Get the count of all point values for all points
     *
     * @return
     */
    public List<PointHistoryCount> getTopPointHistoryCounts() {
        if (Common.databaseProxy.getNoSQLProxy() == null)
            return this.getTopPointHistoryCountsSql();
        return this.getTopPointHistoryCountsNoSql();
    }

    /**
     * NoSQL version to count point values for each point
     * @return
     */
    private List<PointHistoryCount> getTopPointHistoryCountsNoSql() {

        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        //For now we will do this the slow way
        List<DataPointVO> points = query(getJoinedSelectQuery().getSQL() + " ORDER BY dp.deviceName, dp.name", getListResultSetExtractor());
        List<PointHistoryCount> counts = new ArrayList<>();
        for (DataPointVO point : points) {
            PointHistoryCount phc = new PointHistoryCount();
            long count = dao.dateRangeCount(point, 0L, Long.MAX_VALUE);
            phc.setCount((int) count);
            phc.setPointId(point.getId());
            phc.setPointName(point.getName());
            counts.add(phc);
        }
        counts.sort((count1, count2) -> count2.getCount() - count1.getCount());

        return counts;
    }

    /**
     * SQL version to count point values for each point
     * @return
     */
    private List<PointHistoryCount> getTopPointHistoryCountsSql() {
        List<PointHistoryCount> counts = query(
                "select dataPointId, count(*) from pointValues group by dataPointId order by 2 desc",
                new RowMapper<PointHistoryCount>() {
                    @Override
                    public PointHistoryCount mapRow(ResultSet rs, int rowNum) throws SQLException {
                        PointHistoryCount c = new PointHistoryCount();
                        c.setPointId(rs.getInt(1));
                        c.setCount(rs.getInt(2));
                        return c;
                    }
                });

        List<DataPointVO> points = query(getJoinedSelectQuery().getSQL() + " ORDER BY deviceName, name", getListResultSetExtractor());

        // Collate in the point names.
        for (PointHistoryCount c : counts) {
            for (DataPointVO point : points) {
                if (point.getId() == c.getPointId()) {
                    c.setPointName(point.getExtendedName());
                    break;
                }
            }
        }

        // Remove the counts for which there are no point, i.e. deleted.
        counts.removeIf(c -> c.getPointName() == null);

        return counts;
    }

    /**
     * Get the count of data points per type of data source
     * @return
     */
    public List<DataPointUsageStatistics> getUsage() {
        return ejt.query("SELECT ds.dataSourceType, COUNT(ds.dataSourceType) FROM dataPoints as dp LEFT JOIN dataSources ds ON dp.dataSourceId=ds.id GROUP BY ds.dataSourceType",
                new RowMapper<DataPointUsageStatistics>() {
            @Override
            public DataPointUsageStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
                DataPointUsageStatistics usage = new DataPointUsageStatistics();
                usage.setDataSourceType(rs.getString(1));
                usage.setCount(rs.getInt(2));
                return usage;
            }
        });
    }

    @Override
    protected String getXidPrefix() {
        return DataPointVO.XID_PREFIX;
    }

    @Override
    protected Record voToObjectArray(DataPointVO vo) {
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
        record.set(table.dataTypeId, vo.getPointLocator().getDataTypeId());
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

        dp.setData(extractData(record.get(table.jsonData)));
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
     * @param id
     * @return
     */
    public boolean seriesIdExists(int id) {
        return this.create.select(DSL.count(TimeSeries.TIME_SERIES.id))
                .from(TimeSeries.TIME_SERIES)
                .where(TimeSeries.TIME_SERIES.id.eq(id))
                .fetchSingle()
                .value1() == 1;
    }

    @Override
    public void savePreRelationalData(DataPointVO existing, DataPointVO vo) {
        //Shall we generate a new series ID?
        if(vo.getSeriesId() == Common.NEW_ID) {
            int seriesId = insertNewTimeSeries();
            vo.setSeriesId(seriesId);
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
        Map<String, String> tags = vo.getTags();
        if (tags == null) {
            if (existing != null) {
                // only delete the name and device tags, leave existing tags intact
                dataPointTagsDao.deleteNameAndDeviceTagsForDataPointId(vo.getId());
            }
            tags = Collections.emptyMap();
        } else if (existing != null) {
            // we only need to delete tags when doing an update
            dataPointTagsDao.deleteTagsForDataPointId(vo.getId());
        }

        dataPointTagsDao.insertTagsForDataPoint(vo, tags);

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
     * @param vo
     */
    @Override
    public void loadRelationalData(DataPointVO vo) {
        //TODO Mango 4.0 loading tags always is much slower, need to performance test startup times
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
        .where(eventHandlersMapping.eventTypeName.eq(EventType.EventTypeNames.DATA_POINT),
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
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(dataSources.name);
        fields.add(dataSources.xid);
        fields.add(dataSources.dataSourceType);
        return fields;
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        select = select.join(dataSources)
                .on(DSL.field(dataSources.id)
                        .eq(table.dataSourceId));

        if (conditions instanceof ConditionSortLimitWithTagKeys) {
            Map<String, Name> tagKeyToColumn = ((ConditionSortLimitWithTagKeys) conditions).getTagKeyToColumn();
            if (!tagKeyToColumn.isEmpty()) {
                Table<Record> pivotTable = dataPointTagsDao.createTagPivotSql(tagKeyToColumn).asTable().as(DATA_POINT_TAGS_PIVOT_ALIAS);
                return select.leftJoin(pivotTable).on(DataPointTagsDao.PIVOT_ALIAS_DATA_POINT_ID.eq(table.id));
            }
        }
        return select;
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions,
            PermissionHolder user) {

        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = permissionService.getAllInheritedRoles(user).stream().map(Role::getId).collect(Collectors.toList());

            Condition roleIdsIn = MintermsRoles.MINTERMS_ROLES.roleId.in(roleIds);

            Table<?> mintermsGranted = this.create.select(MintermsRoles.MINTERMS_ROLES.mintermId)
                    .from(MintermsRoles.MINTERMS_ROLES)
                    .groupBy(MintermsRoles.MINTERMS_ROLES.mintermId)
                    .having(DSL.count().eq(DSL.count(
                            DSL.case_().when(roleIdsIn, DSL.inline(1))
                            .else_(DSL.inline((Integer)null))))).asTable("mintermsGranted");

            Table<?> permissionsGranted = this.create.selectDistinct(PermissionsMinterms.PERMISSIONS_MINTERMS.permissionId)
                    .from(PermissionsMinterms.PERMISSIONS_MINTERMS)
                    .join(mintermsGranted).on(mintermsGranted.field(MintermsRoles.MINTERMS_ROLES.mintermId).eq(PermissionsMinterms.PERMISSIONS_MINTERMS.mintermId))
                    .asTable("permissionsGranted");

            select = select.join(permissionsGranted).on(
                    permissionsGranted.field(PermissionsMinterms.PERMISSIONS_MINTERMS.permissionId).in(
                            table.readPermissionId));

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
     * @param conditions
     * @param user
     * @param callback
     */
    public void customizedEditQuery(ConditionSortLimit conditions, PermissionHolder user, MappedRowCallback<DataPointVO> callback) {
        SelectJoinStep<Record> select = getSelectQuery(getSelectFields());
        select = joinTables(select, conditions);
        select = joinEditPermissions(select, conditions, user);
        customizedQuery(select, conditions.getCondition(), conditions.getSort(), conditions.getLimit(), conditions.getOffset(), callback);
    }

    public <R extends Record> SelectJoinStep<R> joinEditPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions,
            PermissionHolder user) {
        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = permissionService.getAllInheritedRoles(user).stream().map(Role::getId).collect(Collectors.toList());

            Condition roleIdsIn = MintermsRoles.MINTERMS_ROLES.roleId.in(roleIds);

            Table<?> mintermsGranted = this.create.select(MintermsRoles.MINTERMS_ROLES.mintermId)
                    .from(MintermsRoles.MINTERMS_ROLES)
                    .groupBy(MintermsRoles.MINTERMS_ROLES.mintermId)
                    .having(DSL.count().eq(DSL.count(
                            DSL.case_().when(roleIdsIn, DSL.inline(1))
                            .else_(DSL.inline((Integer)null))))).asTable("mintermsGranted");

            Table<?> permissionsGranted = this.create.select(PermissionsMinterms.PERMISSIONS_MINTERMS.permissionId)
                    .from(PermissionsMinterms.PERMISSIONS_MINTERMS)
                    .join(mintermsGranted).on(mintermsGranted.field(MintermsRoles.MINTERMS_ROLES.mintermId).eq(PermissionsMinterms.PERMISSIONS_MINTERMS.mintermId))
                    .asTable("permissionsGranted");

            select = select.join(permissionsGranted).on(
                    permissionsGranted.field(PermissionsMinterms.PERMISSIONS_MINTERMS.permissionId).in(
                            table.editPermissionId));

        }
        return select;
    }


    protected void notifyTagsUpdated(DataPointVO dataPoint) {
        this.eventPublisher.publishEvent(new DataPointTagsUpdatedEvent(this, dataPoint));
    }

    @Override
    protected Map<String, Field<?>> createAliasMap() {
        Map<String, Field<?>> aliases = super.createAliasMap();
        Map<String, Field<?>> myAliases = new HashMap<>();
        myAliases.put("dataType", DataPoints.DATA_POINTS.dataTypeId);
        return combine(aliases, myAliases);
    }

    @Override
    protected Map<String, Function<Object, Object>> createValueConverterMap() {
        Map<String, Function<Object, Object>> converters = super.createValueConverterMap();
        Map<String, Function<Object, Object>> myConverters = new HashMap<>();
        myConverters.put("dataTypeId", value -> {
            if (value instanceof String) {
                return DataTypes.CODES.getId((String) value);
            }
            return value;
        });
        return combine(converters, myConverters);
    }

    @Override
    protected ResultSetExtractor<List<DataPointVO>> getListResultSetExtractor() {
        return getListResultSetExtractor((e, rs) -> {
            if (e.getCause() instanceof ModuleNotLoadedException) {
                try {
                    LOG.error("Data point with xid '" + rs.getString("xid")
                    + "' could not be loaded. Is its module missing?", e.getCause());
                }catch(SQLException e1) {
                    LOG.error(e.getMessage(), e);
                }
            }else {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    @Override
    protected ResultSetExtractor<Void> getCallbackResultSetExtractor(
            MappedRowCallback<DataPointVO> callback) {
        return getCallbackResultSetExtractor(callback, (e, rs) -> {
            if (e.getCause() instanceof ModuleNotLoadedException) {
                try {
                    LOG.error("Data point with xid '" + rs.getString("xid")
                    + "' could not be loaded. Is its module missing?", e.getCause());
                }catch(SQLException e1) {
                    LOG.error(e.getMessage(), e);
                }
            }else {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public QueryBuilder<DataPointVO> buildQuery(PermissionHolder user) {
        return new DataPointQueryBuilder(aliasMap, valueConverterMap,
                csl -> customizedCount(csl, user),
                (csl, consumer) -> customizedQuery(csl, user, consumer));
    }

    /**
     * Get the read permission ID for in memory checks
     * @param dataPointId
     * @return permission id or null
     */
    public Integer getReadPermissionId(int dataPointId) {
        return this.create.select(table.readPermissionId).from(table).where(table.id.eq(dataPointId)).fetchOneInto(Integer.class);
    }

    private static class DataPointQueryBuilder extends QueryBuilder<DataPointVO> {

        int tagIndex = 0;
        final Map<String, Name> tagKeyToColumn = new HashMap<>();

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

            Name columnName = columnNameForTagKey(tagKey);
            return DSL.field(DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS.append(columnName));
        }

        private Name columnNameForTagKey(String tagKey) {
            return tagKeyToColumn.computeIfAbsent(tagKey, k -> DSL.name("key" + tagIndex++));
        }

        @Override
        protected ConditionSortLimit createConditionSortLimit(Condition condition, List<SortField<?>> sort, Integer limit, Integer offset) {
            return new ConditionSortLimitWithTagKeys(condition, sort, limit, offset, tagKeyToColumn);
        }
    }
}
