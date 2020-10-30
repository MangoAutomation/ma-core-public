/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.SortField;
import org.jooq.Table;
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
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.DataPointTableDefinition;
import com.infiniteautomation.mango.spring.db.DataSourceTableDefinition;
import com.infiniteautomation.mango.spring.db.EventDetectorTableDefinition;
import com.infiniteautomation.mango.spring.db.EventHandlerTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.db.UserCommentTableDefinition;
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
import com.serotonin.m2m2.db.dao.tables.MintermMappingTable;
import com.serotonin.m2m2.db.dao.tables.PermissionMappingTable;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.provider.Providers;
import com.serotonin.util.SerializationHelper;

import static com.serotonin.m2m2.db.dao.DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS;

/**
 *
 * @author Terry Packer
 *
 */
@Repository()
public class DataPointDao extends AbstractVoDao<DataPointVO, DataPointTableDefinition> {
    static final Log LOG = LogFactory.getLog(DataPointDao.class);

    private final DataSourceTableDefinition dataSourceTable;
    private final EventDetectorTableDefinition eventDetectorTable;
    private final UserCommentTableDefinition userCommentTable;
    private final PermissionService permissionService;
    private final DataPointTagsDao dataPointTagsDao;
    private final EventDetectorDao eventDetectorDao;

    private static final LazyInitSupplier<DataPointDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(DataPointDao.class);
    });

    @Autowired
    private DataPointDao(DataPointTableDefinition table,
            DataSourceTableDefinition dataSourceTable,
            EventDetectorTableDefinition eventDetectorTable,
            UserCommentTableDefinition userCommentTable,
            PermissionService permissionService,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher,
            DataPointTagsDao dataPointTagsDao,
            EventDetectorDao eventDetectorDao) {
        super(EventType.EventTypeNames.DATA_POINT, table,
                new TranslatableMessage("internal.monitor.DATA_POINT_COUNT"),
                mapper, publisher);
        this.dataSourceTable = dataSourceTable;
        this.eventDetectorTable = eventDetectorTable;
        this.userCommentTable = userCommentTable;
        this.permissionService = permissionService;
        this.dataPointTagsDao = dataPointTagsDao;
        this.eventDetectorDao = eventDetectorDao;
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
     * @param includeRelationalData
     * @return
     */
    public List<DataPointVO> getDataPoints(int dataSourceId) {
        return this.customizedQuery(getJoinedSelectQuery().where(this.table.getAlias("dataSourceId").eq(dataSourceId)),
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
        List<Field<?>> fields = new ArrayList<>(this.getSelectFields());
        fields.addAll(this.eventDetectorTable.getSelectFields());

        Select<Record> select = this.joinTables(this.getSelectQuery(fields), null).leftOuterJoin(this.eventDetectorTable.getTableAsAlias())
                .on(this.table.getIdAlias().eq(this.eventDetectorTable.getField("dataPointId")))
                .where(this.table.getAlias("dataSourceId").eq(dataSourceId).and(this.table.getAlias("enabled").eq(boolToChar(true))));

        return this.customizedQuery(select, new DataPointStartupResultSetExtractor());
    }

    class DataPointStartupResultSetExtractor implements ResultSetExtractor<List<DataPointWithEventDetectors>> {

        private final int firstEventDetectorColumn;

        public DataPointStartupResultSetExtractor() {
            this.firstEventDetectorColumn = getSelectFields().size() + 1;
        }

        @Override
        public List<DataPointWithEventDetectors> extractData(ResultSet rs) throws SQLException, DataAccessException {
            Map<Integer, DataPointWithEventDetectors> result = new HashMap<>();
            DataPointMapper pointRowMapper = new DataPointMapper();
            while(rs.next()) {
                int id = rs.getInt(1); //dp.id column number
                if(result.containsKey(id))
                    try{
                        addEventDetector(result.get(id), rs);
                    }catch(Exception e){
                        LOG.error("Point not fully initialized: " + e.getMessage(), e);
                    }
                else {
                    DataPointVO dpvo = pointRowMapper.mapRow(rs, rs.getRow());
                    loadRelationalData(dpvo);
                    DataPointWithEventDetectors dp = new DataPointWithEventDetectors(dpvo, new ArrayList<>());
                    result.put(id, dp);
                    try{
                        addEventDetector(dp, rs);
                    }catch(Exception e){
                        LOG.error("Point not fully initialized: " + e.getMessage(), e);
                    }
                }
            }
            return new ArrayList<DataPointWithEventDetectors>(result.values());
        }

        private void addEventDetector(DataPointWithEventDetectors dp, ResultSet rs) throws SQLException {
            if(rs.getObject(firstEventDetectorColumn) == null)
                return;
            PointEventDetectorRowMapper mapper = new PointEventDetectorRowMapper(this.firstEventDetectorColumn, (c) -> DataPointDao.this.extractData(c), dp.getDataPoint(), eventDetectorDao);
            AbstractEventDetectorVO edvo = mapper.mapRow(rs, rs.getRow());
            AbstractPointEventDetectorVO ped = (AbstractPointEventDetectorVO) edvo;
            dp.getEventDetectors().add(ped);
        }
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
        ejt.update("UPDATE dataPoints SET enabled=? WHERE id=?", new Object[]{boolToChar(dp.isEnabled()), dp.getId()});
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
        Select<Record> query = getJoinedSelectQuery().where(table.getAlias("dataSourceId").eq(dataSourceId));
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
                        deleteBatch(batch, pointIds, permissionIds);
                        batch.clear();
                        pointIds.clear();
                        permissionIds.clear();
                    }

                }
                if(batch.size() > 0) {
                    deleteBatch(batch, pointIds, permissionIds);
                }
                return null;
            }
        });
    }

    /**
     * @param batch
     * @param pointIds
     * @param permissionIds
     */
    protected void deleteBatch(List<DataPointVO> batch, List<Integer> ids,
            Set<Integer> permissionIds) {
        //delete event handler mappings
        create.deleteFrom(EventHandlerTableDefinition.EVENT_HANDLER_MAPPING_TABLE)
        .where(EventHandlerTableDefinition.EVENT_HANDLER_MAPPING_EVENT_TYPE_NAME.eq(EventType.EventTypeNames.DATA_POINT),
                EventHandlerTableDefinition.EVENT_HANDLER_MAPPING_TYPEREF1.in(ids)).execute();

        //delete user comments
        create.deleteFrom(userCommentTable.getTable()).where(userCommentTable.getField("commentType").eq(2),
                userCommentTable.getField("typeKey").in(ids)).execute();

        //delete event detectors
        create.deleteFrom(eventDetectorTable.getTable()).where(eventDetectorTable.getField("dataPointId").in(ids)).execute();

        for(DataPointVO vo : batch) {
            DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
            if(def != null) {
                def.deleteRelationalData(vo);
            }
        }

        //delete the points in bulk
        int deleted = create.deleteFrom(table.getTable()).where(table.getIdField().in(ids)).execute();

        if(this.countMonitor != null) {
            this.countMonitor.addValue(-deleted);
        }

        for(Integer id : permissionIds) {
            permissionService.deletePermissionId(id);
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
        Select<?> query = this.joinTables(this.create.select(this.table.getIdAlias(),
                this.table.getXidAlias(),
                this.table.getNameAlias(),
                this.table.getAlias("dataSourceId"),
                this.table.getAlias("deviceName"),
                this.table.getAlias("readPermissionId"),
                this.table.getAlias("editPermissionId"),
                this.table.getAlias("setPermissionId"))
                .from(this.table.getTableAsAlias()), null).where(this.table.getXidAlias().eq(xid)).limit(1);

        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        DataPointSummary item = this.ejt.query(sql, args.toArray(new Object[args.size()]),
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
                        return summary;
                    }else {
                        return null;
                    }
                });
        return item;
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
        Collections.sort(counts, new Comparator<PointHistoryCount>() {

            @Override
            public int compare(PointHistoryCount count1, PointHistoryCount count2) {
                return count2.getCount() - count1.getCount();
            }

        });

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
        Iterator<PointHistoryCount> iter = counts.iterator();
        while (iter.hasNext()) {
            PointHistoryCount c = iter.next();
            if (c.getPointName() == null)
                iter.remove();
        }

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
    protected Object[] voToObjectArray(DataPointVO vo) {
        return new Object[] {
                vo.getXid(),
                vo.getName(),
                SerializationHelper.writeObjectToArray(vo),
                vo.getDataSourceId(),
                vo.getDeviceName(),
                boolToChar(vo.isEnabled()),
                vo.getLoggingType(),
                vo.getIntervalLoggingPeriodType(),
                vo.getIntervalLoggingPeriod(),
                vo.getIntervalLoggingType(),
                vo.getTolerance(),
                boolToChar(vo.isPurgeOverride()),
                vo.getPurgeType(),
                vo.getPurgePeriod(),
                vo.getDefaultCacheSize(),
                boolToChar(vo.isDiscardExtremeValues()),
                vo.getEngineeringUnits(),
                vo.getRollup(),
                vo.getPointLocator().getDataTypeId(),
                boolToChar(vo.getPointLocator().isSettable()),
                convertData(vo.getData()),
                vo.getReadPermission().getId(),
                vo.getEditPermission().getId(),
                vo.getSetPermission().getId()
        };
    }

    @Override
    public RowMapper<DataPointVO> getRowMapper() {
        return new DataPointMapper();
    }

    public class DataPointMapper implements RowMapper<DataPointVO> {
        @Override
        public DataPointVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;
            int id = (rs.getInt(++i));
            String xid = rs.getString(++i);
            String name = rs.getString(++i);

            DataPointVO dp = (DataPointVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(++i));

            dp.setId(id);
            dp.setXid(xid);
            dp.setName(name);
            dp.setDataSourceId(rs.getInt(++i));
            dp.setDeviceName(rs.getString(++i));
            dp.setEnabled(charToBool(rs.getString(++i)));
            dp.setLoggingType(rs.getInt(++i));
            dp.setIntervalLoggingPeriodType(rs.getInt(++i));
            dp.setIntervalLoggingPeriod(rs.getInt(++i));
            dp.setIntervalLoggingType(rs.getInt(++i));
            dp.setTolerance(rs.getDouble(++i));
            dp.setPurgeOverride(charToBool(rs.getString(++i)));
            dp.setPurgeType(rs.getInt(++i));
            dp.setPurgePeriod(rs.getInt(++i));
            dp.setDefaultCacheSize(rs.getInt(++i));
            dp.setDiscardExtremeValues(charToBool(rs.getString(++i)));
            dp.setEngineeringUnits(rs.getInt(++i));
            dp.setRollup(rs.getInt(++i));

            // read and discard dataTypeId
            rs.getInt(++i);
            // read and discard settable boolean
            rs.getString(++i);

            dp.setData(extractData(rs.getClob(++i)));

            MangoPermission read = new MangoPermission(rs.getInt(++i));
            dp.supplyReadPermission(() -> read);

            MangoPermission edit = new MangoPermission(rs.getInt(++i));
            dp.supplyEditPermission(() -> edit);

            MangoPermission set = new MangoPermission(rs.getInt(++i));
            dp.supplySetPermission(() -> set);

            // Data source information from join
            dp.setDataSourceName(rs.getString(++i));
            dp.setDataSourceXid(rs.getString(++i));
            dp.setDataSourceTypeName(rs.getString(++i));
            dp.ensureUnitsCorrect();
            return dp;
        }
    }

    @Override
    public void savePreRelationalData(DataPointVO existing, DataPointVO vo) {
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
        create.deleteFrom(EventHandlerTableDefinition.EVENT_HANDLER_MAPPING_TABLE)
        .where(EventHandlerTableDefinition.EVENT_HANDLER_MAPPING_EVENT_TYPE_NAME.eq(EventType.EventTypeNames.DATA_POINT),
                EventHandlerTableDefinition.EVENT_HANDLER_MAPPING_TYPEREF1.eq(vo.getId())).execute();

        //delete user comments
        create.deleteFrom(userCommentTable.getTable()).where(userCommentTable.getField("commentType").eq(2),
                userCommentTable.getField("typeKey").eq(vo.getId())).execute();

        //delete event detectors
        create.deleteFrom(eventDetectorTable.getTable()).where(eventDetectorTable.getField("dataPointId").eq(vo.getId())).execute();

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
    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(this.table.getSelectFields());
        fields.add(dataSourceTable.getAlias("name"));
        fields.add(dataSourceTable.getAlias("xid"));
        fields.add(dataSourceTable.getAlias("dataSourceType"));
        return fields;
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        select = select.join(dataSourceTable.getTableAsAlias())
                .on(DSL.field(dataSourceTable.getAlias("id"))
                        .eq(this.table.getAlias("dataSourceId")));

        if (conditions instanceof ConditionSortLimitWithTagKeys) {
            Map<String, Name> tagKeyToColumn = ((ConditionSortLimitWithTagKeys) conditions).getTagKeyToColumn();
            if (!tagKeyToColumn.isEmpty()) {
                Table<Record> pivotTable = dataPointTagsDao.createTagPivotSql(tagKeyToColumn).asTable().as(DATA_POINT_TAGS_PIVOT_ALIAS);
                return select.leftJoin(pivotTable).on(DataPointTagsDao.PIVOT_ALIAS_DATA_POINT_ID.eq(this.table.getIdAlias()));
            }
        }
        return select;
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions,
            PermissionHolder user) {

        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = permissionService.getAllInheritedRoles(user).stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);

            Table<?> mintermsGranted = this.create.select(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .from(MintermMappingTable.MINTERMS_MAPPING)
                    .groupBy(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .having(DSL.count().eq(DSL.count(
                            DSL.case_().when(roleIdsIn, DSL.inline(1))
                            .else_(DSL.inline((Integer)null))))).asTable("mintermsGranted");

            Table<?> permissionsGranted = this.create.selectDistinct(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId)
                    .from(PermissionMappingTable.PERMISSIONS_MAPPING)
                    .join(mintermsGranted).on(mintermsGranted.field(MintermMappingTable.MINTERMS_MAPPING.mintermId).eq(PermissionMappingTable.PERMISSIONS_MAPPING.mintermId))
                    .asTable("permissionsGranted");

            select = select.join(permissionsGranted).on(
                    permissionsGranted.field(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId).in(
                            DataPointTableDefinition.READ_PERMISSION_ALIAS));

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
            List<Integer> roleIds = permissionService.getAllInheritedRoles(user).stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);

            Table<?> mintermsGranted = this.create.select(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .from(MintermMappingTable.MINTERMS_MAPPING)
                    .groupBy(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .having(DSL.count().eq(DSL.count(
                            DSL.case_().when(roleIdsIn, DSL.inline(1))
                            .else_(DSL.inline((Integer)null))))).asTable("mintermsGranted");

            Table<?> permissionsGranted = this.create.select(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId)
                    .from(PermissionMappingTable.PERMISSIONS_MAPPING)
                    .join(mintermsGranted).on(mintermsGranted.field(MintermMappingTable.MINTERMS_MAPPING.mintermId).eq(PermissionMappingTable.PERMISSIONS_MAPPING.mintermId))
                    .asTable("permissionsGranted");

            select = select.join(permissionsGranted).on(
                    permissionsGranted.field(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId).in(
                            DataPointTableDefinition.EDIT_PERMISSION_ALIAS));

        }
        return select;
    }


    protected void notifyTagsUpdated(DataPointVO dataPoint) {
        this.eventPublisher.publishEvent(new DataPointTagsUpdatedEvent(this, dataPoint));
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
        return new DataPointQueryBuilder(table.getAliasMap(), valueConverterMap,
                csl -> customizedCount(csl, user),
                (csl, consumer) -> customizedQuery(csl, user, consumer));
    }

    /**
     * Get the read permission ID for in memory checks
     * @param dataPointId
     * @return permission id or null
     */
    public Integer getReadPermissionId(int dataPointId) {
        return this.create.select(DataPointTableDefinition.READ_PERMISSION).from(DataPointTableDefinition.TABLE).where(this.table.getIdField().eq(dataPointId)).fetchOneInto(Integer.class);
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
        protected ConditionSortLimit createConditionSortLimit(Condition condition, List<SortField<Object>> sort, Integer limit, Integer offset) {
            return new ConditionSortLimitWithTagKeys(condition, sort, limit, offset, tagKeyToColumn);
        }
    }
}
