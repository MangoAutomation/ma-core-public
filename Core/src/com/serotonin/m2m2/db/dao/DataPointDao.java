/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import static com.serotonin.m2m2.db.dao.DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.db.query.Index;
import com.infiniteautomation.mango.db.query.QueryAttribute;
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.db.query.RQLToConditionWithTagKeys;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.DataPointTableDefinition;
import com.infiniteautomation.mango.spring.db.DataSourceTableDefinition;
import com.infiniteautomation.mango.spring.db.EventDetectorTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.events.DataPointTagsUpdatedEvent;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.usage.DataPointUsageStatistics;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataPointChangeDefinition;
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

/**
 * This class is a Half-Breed between the legacy Dao and the new type that extends AbstractDao.
 *
 * The top half of the code is the legacy code, the bottom is the new style.
 *
 * Eventually all the method innards will be reworked, leaving the names the same.
 *
 * @author Terry Packer
 *
 */
@Repository()
public class DataPointDao extends AbstractDao<DataPointVO, DataPointTableDefinition> {
    static final Log LOG = LogFactory.getLog(DataPointDao.class);

    private final DataSourceTableDefinition dataSourceTable;
    private final EventDetectorTableDefinition eventDetectorTable;

    private static final LazyInitSupplier<DataPointDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(DataPointDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (DataPointDao)o;
    });

    @Autowired
    private DataPointDao(DataPointTableDefinition table,
            DataSourceTableDefinition dataSourceTable,
            EventDetectorTableDefinition eventDetectorTable,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(EventType.EventTypeNames.DATA_POINT, table,
                new TranslatableMessage("internal.monitor.DATA_POINT_COUNT"),
                mapper, publisher);
        this.dataSourceTable = dataSourceTable;
        this.eventDetectorTable = eventDetectorTable;
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
     * Get data points for a data source
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
     * Get points for runtime in an efficient manner by joining with the event detectors
     * @param dataSourceId
     * @return
     */
    public List<DataPointWithEventDetectors> getDataPointsForDataSourceStart(int dataSourceId) {
        List<Field<?>> fields = new ArrayList<>(this.getSelectFields());
        fields.addAll(this.eventDetectorTable.getSelectFields());

        Select<Record> select = this.joinTables(this.getSelectQuery(fields), null).leftOuterJoin(this.eventDetectorTable.getTableAsAlias())
                .on(this.table.getIdAlias().eq(this.eventDetectorTable.getField("dataPointId")))
                .where(this.table.getAlias("dataSourceId").eq(dataSourceId));

        return this.customizedQuery(select, new DataPointStartupResultSetExtractor());
    }

    class DataPointStartupResultSetExtractor implements ResultSetExtractor<List<DataPointWithEventDetectors>> {

        private final int firstEventDetectorColumn;
        private final EventDetectorRowMapper<?> eventRowMapper;

        public DataPointStartupResultSetExtractor() {
            this.firstEventDetectorColumn = getSelectFields().size() + 1;
            this.eventRowMapper = new EventDetectorRowMapper<>(this.firstEventDetectorColumn, 5);
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
            AbstractEventDetectorVO edvo = eventRowMapper.mapRow(rs, rs.getRow());
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
        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.beforeInsert(vo);

        // Create a default text renderer
        if (vo.getTextRenderer() == null)
            vo.defaultTextRenderer();

        super.insert(vo);

        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.afterInsert(vo);
    }

    @Override
    public void update(DataPointVO existing, DataPointVO vo) {
        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.beforeUpdate(vo);

        //If have a new data type we will wipe our history
        if (existing.getPointLocator().getDataTypeId() != vo.getPointLocator().getDataTypeId())
            Common.databaseProxy.newPointValueDao().deletePointValues(vo.getId());

        super.update(existing, vo);

        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.afterUpdate(vo);
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

    public void deleteDataPoints(final int dataSourceId) {
        List<DataPointVO> old = getDataPoints(dataSourceId);

        for (DataPointVO dp : old) {
            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.beforeDelete(dp.getId());
        }

        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                List<Integer> pointIds = queryForList("select id from dataPoints where dataSourceId=?",
                        new Object[] { dataSourceId }, Integer.class);
                if (pointIds.size() > 0) {
                    String dataPointIdList = createDelimitedList(new HashSet<>(pointIds), ",", null);
                    dataPointIdList = "(" + dataPointIdList + ")";
                    ejt.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1 in " + dataPointIdList,
                            new Object[] { EventType.EventTypeNames.DATA_POINT });
                    ejt.update("delete from userComments where commentType=2 and typeKey in " + dataPointIdList);
                    ejt.update("delete from eventDetectors where dataPointId in " + dataPointIdList);
                    ejt.update("delete from dataPoints where id in " + dataPointIdList);
                }
            }
        });

        for (DataPointVO dp : old) {
            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.afterDelete(dp.getId());
            this.publishEvent(new DaoEvent<DataPointVO>(this, DaoEventType.DELETE, dp));
            AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_DATA_POINT, dp);
            this.countMonitor.decrement();
        }
    }

    @Override
    public boolean delete(DataPointVO vo) {
        boolean deleted = false;
        if (vo != null) {
            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.beforeDelete(vo.getId());
            deleted = super.delete(vo);
            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.afterDelete(vo.getId());
        }
        return deleted;
    }

    @Override
    public void deleteRelationalData(DataPointVO vo) {
        ejt.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1 = " + vo.getId(),
                new Object[] { EventType.EventTypeNames.DATA_POINT });
        ejt.update("delete from userComments where commentType=2 and typeKey = " + vo.getId());
        ejt.update("delete from eventDetectors where dataPointId = " + vo.getId());
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.READ);
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.SET);

        DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
        if(def != null) {
            def.deleteRelationalData(vo);
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
                this.table.getAlias("deviceName"))
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
                        return summary;
                    }else {
                        return null;
                    }
                });
        if (item != null) {
            item.setReadRoles(RoleDao.getInstance().getRoles(item.getId(), DataPointVO.class.getSimpleName(), PermissionService.READ));
            item.setSetRoles(RoleDao.getInstance().getRoles(item.getId(), DataPointVO.class.getSimpleName(), PermissionService.SET));
        }
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
            long count = dao.dateRangeCount(point.getId(), 0L, Long.MAX_VALUE);
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
                boolToChar(vo.getPointLocator().isSettable())};
    }

    //TODO Mango 4.0 should we re-add this?
    protected List<Index> getIndexes() {
        List<Index> indexes = new ArrayList<Index>();
        List<QueryAttribute> columns = new ArrayList<QueryAttribute>();
        //Data Source Name Force
        columns.add(new QueryAttribute("name", new HashSet<String>(), Types.VARCHAR));
        indexes.add(new Index("nameIndex", "ds", columns, "ASC"));

        //Data Source xid Force
        columns = new ArrayList<QueryAttribute>();
        columns.add(new QueryAttribute("xid", new HashSet<String>(), Types.VARCHAR));
        indexes.add(new Index("dataSourcesUn1", "ds", columns, "ASC"));

        //DeviceNameName Index Force
        columns = new ArrayList<QueryAttribute>();
        columns.add(new QueryAttribute("deviceName", new HashSet<String>(), Types.VARCHAR));
        columns.add(new QueryAttribute("name", new HashSet<String>(), Types.VARCHAR));
        indexes.add(new Index("deviceNameNameIndex", "dp", columns, "ASC"));

        //xid point name force
        columns = new ArrayList<QueryAttribute>();
        columns.add(new QueryAttribute("xid", new HashSet<String>(), Types.VARCHAR));
        columns.add(new QueryAttribute("name", new HashSet<String>(), Types.VARCHAR));
        indexes.add(new Index("xidNameIndex", "dp", columns, "ASC"));

        return indexes;
    }

    @Override
    public RowMapper<DataPointVO> getRowMapper() {
        return new DataPointMapper();
    }

    public static class DataPointMapper implements RowMapper<DataPointVO> {
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

            // Data source information from join
            dp.setDataSourceName(rs.getString(++i));
            dp.setDataSourceXid(rs.getString(++i));
            dp.setDataSourceTypeName(rs.getString(++i));

            dp.ensureUnitsCorrect();
            return dp;
        }
    }

    /**
     * Loads the event detectors, point comments, tags data source and template name
     * Used by getFull()
     * @param vo
     */
    @Override
    public void loadRelationalData(DataPointVO vo) {
        //TODO Mango 4.0 loading tags always is much slower
        vo.setTags(DataPointTagsDao.getInstance().getTagsForDataPointId(vo.getId()));

        //Populate permissions
        vo.setReadRoles(RoleDao.getInstance().getRoles(vo, PermissionService.READ));
        vo.setSetRoles(RoleDao.getInstance().getRoles(vo, PermissionService.SET));
        vo.setDataSourceEditRoles(RoleDao.getInstance().getRoles(vo.getDataSourceId(), DataSourceVO.class.getSimpleName(), PermissionService.EDIT));

        DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
        if(def != null) {
            def.loadRelationalData(vo);
        }
    }

    @Override
    public void saveRelationalData(DataPointVO vo, boolean insert) {
        Map<String, String> tags = vo.getTags();
        if (tags == null) {
            if (!insert) {
                // only delete the name and device tags, leave existing tags intact
                DataPointTagsDao.getInstance().deleteNameAndDeviceTagsForDataPointId(vo.getId());
            }
            tags = Collections.emptyMap();
        } else if (!insert) {
            // we only need to delete tags when doing an update
            DataPointTagsDao.getInstance().deleteTagsForDataPointId(vo.getId());
        }

        DataPointTagsDao.getInstance().insertTagsForDataPoint(vo, tags);
        //Replace the role mappings
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getReadRoles(), vo, PermissionService.READ, insert);
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getSetRoles(), vo, PermissionService.SET, insert);

        DataSourceDefinition<? extends DataSourceVO> def = ModuleRegistry.getDataSourceDefinition(vo.getPointLocator().getDataSourceType());
        if(def != null) {
            def.saveRelationalData(vo, insert);
        }
    }

    @Override
    public Condition hasPermission(PermissionHolder user) {
        List<Integer> roleIds = user.getRoles().stream().map(r -> r.getId()).collect(Collectors.toList());
        return RoleTableDefinition.roleIdFieldAlias.in(roleIds);
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinRoles(SelectJoinStep<R> select, String permissionType) {

        if(PermissionService.EDIT.equals(permissionType)) {

            Condition editJoinCondition = DSL.and(
                    RoleTableDefinition.voTypeFieldAlias.eq(DataSourceVO.class.getSimpleName()),
                    RoleTableDefinition.voIdFieldAlias.eq(this.table.getAlias("dataSourceId")),
                    RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.EDIT)
                    );
            return select.join(RoleTableDefinition.roleMappingTableAsAlias).on(editJoinCondition);

        }else if(PermissionService.SET.equals(permissionType)) {

            Condition setJoinCondition = DSL.or(
                    DSL.and(
                            RoleTableDefinition.voTypeFieldAlias.eq(DataPointVO.class.getSimpleName()),
                            RoleTableDefinition.voIdFieldAlias.eq(this.table.getIdAlias()),
                            RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.SET)
                            ),
                    DSL.and(
                            RoleTableDefinition.voTypeFieldAlias.eq(DataSourceVO.class.getSimpleName()),
                            RoleTableDefinition.voIdFieldAlias.eq(this.table.getAlias("dataSourceId")),
                            RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.EDIT)
                            )
                    );

            return select.join(RoleTableDefinition.roleMappingTableAsAlias).on(setJoinCondition);
        }else if(PermissionService.READ.equals(permissionType)) {

            Condition readJoinCondition = DSL.or(
                    DSL.and(
                            RoleTableDefinition.voTypeFieldAlias.eq(DataPointVO.class.getSimpleName()),
                            RoleTableDefinition.voIdFieldAlias.eq(this.table.getIdAlias()),
                            DSL.or(
                                    RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.SET),
                                    RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.READ)
                                    )
                            ),
                    DSL.and(
                            RoleTableDefinition.voTypeFieldAlias.eq(DataSourceVO.class.getSimpleName()),
                            RoleTableDefinition.voIdFieldAlias.eq(this.table.getAlias("dataSourceId")),
                            RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.EDIT)
                            )
                    );

            return select.join(RoleTableDefinition.roleMappingTableAsAlias).on(readJoinCondition);
        }
        return select;
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

        if(conditions != null) {
            for(BiFunction<SelectJoinStep<?>, ConditionSortLimit, SelectJoinStep<?>> join : conditions.getJoins()) {
                select = (SelectJoinStep<R>) join.apply(select, conditions);
            }
        }

        if (conditions instanceof ConditionSortLimitWithTagKeys) {
            Map<String, Name> tagKeyToColumn = ((ConditionSortLimitWithTagKeys) conditions).getTagKeyToColumn();
            if (!tagKeyToColumn.isEmpty()) {
                Table<Record> pivotTable = DataPointTagsDao.getInstance().createTagPivotSql(tagKeyToColumn).asTable().as(DATA_POINT_TAGS_PIVOT_ALIAS);
                return select.leftJoin(pivotTable).on(DataPointTagsDao.PIVOT_ALIAS_DATA_POINT_ID.eq(this.table.getIdAlias()));
            }
        }
        return select;
    }

    @Override
    protected RQLToCondition createRqlToCondition(Map<String, Field<?>> fieldMap,
            Map<String, Function<Object, Object>> converterMap) {
        return new RQLToConditionWithTagKeys(fieldMap, converterMap);
    }

    protected void notifyTagsUpdated(DataPointVO dataPoint) {
        this.eventPublisher.publishEvent(new DataPointTagsUpdatedEvent(this, dataPoint));
    }

    @Override
    protected Map<String, Function<Object, Object>> createValueConverterMap() {
        Map<String, Function<Object, Object>> map = new HashMap<>(super.createValueConverterMap());
        map.put("dataTypeId", value -> {
            if (value instanceof String) {
                return DataTypes.CODES.getId((String) value);
            }
            return value;
        });
        return map;
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

}
