/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.io.ObjectStreamException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.jazdw.rql.parser.ASTNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.infiniteautomation.mango.db.query.RQLToSQLSelect;
import com.infiniteautomation.mango.db.query.SQLStatement;
import com.infiniteautomation.mango.db.query.StreamableSqlQuery;
import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DataPointChangeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointExtendedNameComparator;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchyEventDispatcher;
import com.serotonin.m2m2.vo.template.BaseTemplateVO;
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
public class DataPointDao extends AbstractDao<DataPointVO> {
    static final Log LOG = LogFactory.getLog(DataPointDao.class);
    public static final DataPointDao instance = new DataPointDao();

    /**
     * TODO make protected, remove access to constructor
     * 
     * @param typeName
     */
    public DataPointDao() {
        super(AuditEventType.TYPE_DATA_POINT, "dp", 
        		new String[] { "ds.name", "ds.xid", "ds.dataSourceType", "template.name" }, //Extra Properties not in table
                "join dataSources ds on ds.id = dp.dataSourceId left outer join templates template on template.id = dp.templateId"); //Extra Joins to get the data we need

    }

    //
    //
    // Data Points
    //
    @Override
    public String generateUniqueXid() {
        return generateUniqueXid(DataPointVO.XID_PREFIX, "dataPoints");
    }

    @Override
    public boolean isXidUnique(String xid, int excludeId) {
        return isXidUnique(xid, excludeId, "dataPoints");
    }

    public String getExtendedPointName(int dataPointId) {
        DataPointVO vo = getDataPoint(dataPointId);
        if (vo == null)
            return "?";
        return vo.getExtendedName();
    }

    private static final String DATA_POINT_SELECT = //
    "select dp.data, dp.id, dp.xid, dp.dataSourceId, dp.name, dp.deviceName, dp.enabled, dp.pointFolderId, " //
            + "  dp.loggingType, dp.intervalLoggingPeriodType, dp.intervalLoggingPeriod, dp.intervalLoggingType, " //
            + "  dp.tolerance, dp.purgeOverride, dp.purgeType, dp.purgePeriod, dp.defaultCacheSize, " //
            + "  dp.discardExtremeValues, dp.engineeringUnits, dp.readPermission, dp.setPermission, dp.templateId, ds.name, " //
            + "  ds.xid, ds.dataSourceType " //
            + "from dataPoints dp join dataSources ds on ds.id = dp.dataSourceId ";

    public List<DataPointVO> getDataPoints(Comparator<IDataPoint> comparator, boolean includeRelationalData) {
        List<DataPointVO> dps = query(DATA_POINT_SELECT, new DataPointRowMapper());
        if (includeRelationalData)
            setRelationalData(dps);
        if (comparator != null)
            Collections.sort(dps, comparator);
        return dps;
    }

    public List<DataPointVO> getDataPoints(int dataSourceId, Comparator<DataPointVO> comparator) {
        return getDataPoints(dataSourceId, comparator, true);
    }

    public List<DataPointVO> getDataPoints(int dataSourceId, Comparator<DataPointVO> comparator,
            boolean includeRelationalData) {
        List<DataPointVO> dps = query(DATA_POINT_SELECT + " where dp.dataSourceId=?", new Object[] { dataSourceId },
                new DataPointRowMapper());
        if (includeRelationalData)
            setRelationalData(dps);
        if (comparator != null)
            Collections.sort(dps, comparator);
        return dps;
    }

    public DataPointVO getDataPoint(int id) {
        return getDataPoint(id, true);
    }

    public DataPointVO getDataPoint(int id, boolean includeRelationalData) {
        try {
            DataPointVO dp = queryForObject(DATA_POINT_SELECT + " where dp.id=?", new Object[] { id },
                    new DataPointRowMapper(), null);
            if (includeRelationalData)
                setRelationalData(dp);
            return dp;
        }
        catch (ShouldNeverHappenException e) {
            // If the module was removed but there are still records in the database, this exception will be
            // thrown. Check the inner exception to confirm.
            if (e.getCause() instanceof ObjectStreamException) {
                // Yep. Log the occurrence and continue.
                LOG.error("Data point with id '" + id + "' could not be loaded. Is its module missing?", e);
            }
        }
        return null;
    }

    public DataPointVO getDataPoint(String xid) {
        DataPointVO dp = queryForObject(DATA_POINT_SELECT + " where dp.xid=?", new Object[] { xid },
                new DataPointRowMapper(), null);
        setRelationalData(dp);
        return dp;
    }

    class DataPointRowMapper implements RowMapper<DataPointVO> {
        @Override
        public DataPointVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;

            DataPointVO dp = (DataPointVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(++i));
            dp.setId(rs.getInt(++i));
            dp.setXid(rs.getString(++i));
            dp.setDataSourceId(rs.getInt(++i));
            dp.setName(rs.getString(++i));
            dp.setDeviceName(rs.getString(++i));
            dp.setEnabled(charToBool(rs.getString(++i)));
            dp.setPointFolderId(rs.getInt(++i));
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
            dp.setReadPermission(rs.getString(++i));
            dp.setSetPermission(rs.getString(++i));
            //Because we read 0 for null
            dp.setTemplateId(rs.getInt(++i));
            if(rs.wasNull())
            	dp.setTemplateId(null);

            // Data source information.
            dp.setDataSourceName(rs.getString(++i));
            dp.setDataSourceXid(rs.getString(++i));
            dp.setDataSourceTypeName(rs.getString(++i));

            dp.ensureUnitsCorrect();

            return dp;
        }
    }

    private void setRelationalData(List<DataPointVO> dps) {
        for (DataPointVO dp : dps)
            setRelationalData(dp);
    }

    private void setRelationalData(DataPointVO dp) {
        if (dp == null)
            return;
        setEventDetectors(dp);
        setPointComments(dp);
    }

    public void saveDataPoint(final DataPointVO dp) {
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                // Decide whether to insert or update.
                if (dp.getId() == Common.NEW_ID)
                    insertDataPoint(dp);
                else
                    updateDataPoint(dp);

                // Reset the point hierarchy so that the new or changed point
                // gets reflected.
                clearPointHierarchyCache();
            }
        });
    }

    void insertDataPoint(final DataPointVO dp) {
        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.beforeInsert(dp);

        // Create a default text renderer
        if (dp.getTextRenderer() == null)
            dp.defaultTextRenderer();

        dp.setId(ejt.doInsert(
                "insert into dataPoints (xid, dataSourceId, name, deviceName, enabled, pointFolderId, loggingType, " //
                        + "intervalLoggingPeriodType, intervalLoggingPeriod, intervalLoggingType, tolerance, " //
                        + "purgeOverride, purgeType, purgePeriod, defaultCacheSize, discardExtremeValues, " //
                        + "engineeringUnits, readPermission, setPermission, templateId, data) " //
                        + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", //
                new Object[] { dp.getXid(), dp.getDataSourceId(), dp.getName(), dp.getDeviceName(),
                        boolToChar(dp.isEnabled()), dp.getPointFolderId(), dp.getLoggingType(),
                        dp.getIntervalLoggingPeriodType(), dp.getIntervalLoggingPeriod(), dp.getIntervalLoggingType(),
                        dp.getTolerance(), boolToChar(dp.isPurgeOverride()), dp.getPurgeType(), dp.getPurgePeriod(),
                        dp.getDefaultCacheSize(), boolToChar(dp.isDiscardExtremeValues()), dp.getEngineeringUnits(),
                        dp.getReadPermission(), dp.getSetPermission(), dp.getTemplateId(), SerializationHelper.writeObject(dp) }, //
                new int[] { Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.CHAR, Types.INTEGER,
                        Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.DOUBLE, Types.CHAR,
                        Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.CHAR, Types.INTEGER, Types.VARCHAR,
                        Types.VARCHAR, Types.INTEGER, Types.BINARY }));

        // Save the relational information.
        saveEventDetectors(dp);

        AuditEventType.raiseAddedEvent(AuditEventType.TYPE_DATA_POINT, dp);

        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.afterInsert(dp);
    }

    void updateDataPoint(final DataPointVO dp) {
        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.beforeUpdate(dp);

        DataPointVO old = getDataPoint(dp.getId());

        if (old.getPointLocator().getDataTypeId() != dp.getPointLocator().getDataTypeId())
            // Delete any point values where data type doesn't match the vo,
            // just in case the data type was changed.
            // Only do this if the data type has actually changed because it is
            // just really slow if the database is
            // big or busy.
            //new PointValueDao().deletePointValuesWithMismatchedType(dp.getId(), dp.getPointLocator().getDataTypeId());
            Common.databaseProxy.newPointValueDao().deletePointValuesWithMismatchedType(dp.getId(),
                    dp.getPointLocator().getDataTypeId());
        // Save the VO information.
        updateDataPointShallow(dp);

        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_DATA_POINT, old, dp);

        // Save the relational information.
        saveEventDetectors(dp);

        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.afterUpdate(dp);
    }

    public void updateDataPointShallow(final DataPointVO dp) {
        ejt.update(
                "update dataPoints set xid=?, name=?, deviceName=?, enabled=?, pointFolderId=?, loggingType=?, " //
                        + "intervalLoggingPeriodType=?, intervalLoggingPeriod=?, intervalLoggingType=?, " //
                        + "tolerance=?, purgeOverride=?, purgeType=?, purgePeriod=?, defaultCacheSize=?, " //
                        + "discardExtremeValues=?, engineeringUnits=?, readPermission=?, setPermission=?, templateId=?, data=? " //
                        + "where id=?", //
                new Object[] { dp.getXid(), dp.getName(), dp.getDeviceName(), boolToChar(dp.isEnabled()),
                        dp.getPointFolderId(), dp.getLoggingType(), dp.getIntervalLoggingPeriodType(),
                        dp.getIntervalLoggingPeriod(), dp.getIntervalLoggingType(), dp.getTolerance(),
                        boolToChar(dp.isPurgeOverride()), dp.getPurgeType(), dp.getPurgePeriod(),
                        dp.getDefaultCacheSize(), boolToChar(dp.isDiscardExtremeValues()), dp.getEngineeringUnits(),
                        dp.getReadPermission(), dp.getSetPermission(), dp.getTemplateId(), SerializationHelper.writeObject(dp), dp.getId() }, //
                new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CHAR, Types.INTEGER, Types.INTEGER,
                        Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.DOUBLE, Types.CHAR, Types.INTEGER,
                        Types.INTEGER, Types.INTEGER, Types.CHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                        Types.BINARY, Types.INTEGER });
    }

    public void deleteDataPoints(final int dataSourceId) {
        List<DataPointVO> old = getDataPoints(dataSourceId, null, true);

        for (DataPointVO dp : old) {
            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.beforeDelete(dp.getId());
        }

        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @SuppressWarnings("synthetic-access")
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                List<Integer> pointIds = queryForList("select id from dataPoints where dataSourceId=?",
                        new Object[] { dataSourceId }, Integer.class);
                if (pointIds.size() > 0)
                    deleteDataPointImpl(createDelimitedList(new HashSet<>(pointIds), ",", null));
            }
        });

        for (DataPointVO dp : old) {
            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.afterDelete(dp.getId());
            AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_DATA_POINT, dp);
        }
    }

    /**
     * Override the delete method to perform extra work
     */
    @Override
    public void delete(int id) {
        deleteDataPoint(id);
    }

    public void deleteDataPoint(final int dataPointId) {
        DataPointVO dp = getDataPoint(dataPointId);
        if (dp != null) {
            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.beforeDelete(dataPointId);

            getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    deleteDataPointImpl(Integer.toString(dataPointId));
                }
            });

            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.afterDelete(dataPointId);

            AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_DATA_POINT, dp);
        }
    }

    void deleteDataPointImpl(String dataPointIdList) {
        dataPointIdList = "(" + dataPointIdList + ")";
        ejt.update("delete from eventHandlers where eventTypeName=? and eventTypeRef1 in " + dataPointIdList,
                new Object[] { EventType.EventTypeNames.DATA_POINT });
        ejt.update("delete from userComments where commentType=2 and typeKey in " + dataPointIdList);
        ejt.update("delete from pointEventDetectors where dataPointId in " + dataPointIdList);
        ejt.update("delete from dataPoints where id in " + dataPointIdList);

        clearPointHierarchyCache();
    }

    public int countPointsForDataSourceType(String dataSourceType) {
        return ejt.queryForInt("SELECT count(*) FROM dataPoints dp LEFT JOIN dataSources ds ON dp.dataSourceId=ds.id "
                + "WHERE ds.dataSourceType=?", new Object[] { dataSourceType }, 0);
    }

    //
    //
    // Event detectors
    //
    public int getDataPointIdFromDetectorId(int pedId) {
        return ejt.queryForInt("select dataPointId from pointEventDetectors where id=?", new Object[] { pedId }, 0);
    }

    public String getDetectorXid(int pedId) {
        return queryForObject("select xid from pointEventDetectors where id=?", new Object[] { pedId }, String.class,
                null);
    }

    public int getDetectorId(String pedXid, int dataPointId) {
        return ejt.queryForInt("select id from pointEventDetectors where xid=? and dataPointId=?", new Object[] {
                pedXid, dataPointId }, -1);
    }

    public String generateEventDetectorUniqueXid(int dataPointId) {
        String xid = Common.generateXid(PointEventDetectorVO.XID_PREFIX);
        while (!isEventDetectorXidUnique(dataPointId, xid, -1))
            xid = Common.generateXid(PointEventDetectorVO.XID_PREFIX);
        return xid;
    }

    public boolean isEventDetectorXidUnique(int dataPointId, String xid, int excludeId) {
        return ejt.queryForInt("select count(*) from pointEventDetectors where dataPointId=? and xid=? and id<>?",
                new Object[] { dataPointId, xid, excludeId }, 0) == 0;
    }

    public void setEventDetectors(DataPointVO dp) {
        dp.setEventDetectors(getEventDetectors(dp));
    }

    private List<PointEventDetectorVO> getEventDetectors(DataPointVO dp) {
        return query(
                "select id, xid, alias, detectorType, alarmLevel, stateLimit, duration, durationType, binaryState, "
                        + "  multistateState, changeCount, alphanumericState, weight " + "from pointEventDetectors "
                        + "where dataPointId=? " + "order by id", new Object[] { dp.getId() },
                new EventDetectorRowMapper(dp));
    }

    public PointEventDetectorVO getEventDetector(int id) {
        return ejt.queryForObject(
                "select id, xid, alias, detectorType, alarmLevel, stateLimit, duration, durationType, binaryState, "
                        + "  multistateState, changeCount, alphanumericState, weight " + "from pointEventDetectors "
                        + "where id=? ", new Object[] { id }, new EventDetectorRowMapper(null));
    }

    class EventDetectorRowMapper implements RowMapper<PointEventDetectorVO> {
        private final DataPointVO dp;

        public EventDetectorRowMapper(DataPointVO dp) {
            this.dp = dp;
        }

        @Override
        public PointEventDetectorVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            PointEventDetectorVO detector = new PointEventDetectorVO();
            int i = 0;
            detector.setId(rs.getInt(++i));
            detector.setXid(rs.getString(++i));
            detector.setAlias(rs.getString(++i));
            detector.setDetectorType(rs.getInt(++i));
            detector.setAlarmLevel(rs.getInt(++i));
            detector.setLimit(rs.getDouble(++i));
            detector.setDuration(rs.getInt(++i));
            detector.setDurationType(rs.getInt(++i));
            detector.setBinaryState(charToBool(rs.getString(++i)));
            detector.setMultistateState(rs.getInt(++i));
            detector.setChangeCount(rs.getInt(++i));
            detector.setAlphanumericState(rs.getString(++i));
            detector.setWeight(rs.getDouble(++i));
            detector.njbSetDataPoint(dp);
            return detector;
        }
    }

    private void saveEventDetectors(DataPointVO dp) {
        // Get the ids of the existing detectors for this point.
        final List<PointEventDetectorVO> existingDetectors = getEventDetectors(dp);

        // Insert or update each detector in the point.
        for (PointEventDetectorVO ped : dp.getEventDetectors()) {
            if (ped.getId() < 0) {
                // Insert the record.
                ped.setId(ejt.doInsert(
                        "insert into pointEventDetectors "
                                + "  (xid, alias, dataPointId, detectorType, alarmLevel, stateLimit, duration, durationType, "
                                + "  binaryState, multistateState, changeCount, alphanumericState, weight) "
                                + "values (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        new Object[] { ped.getXid(), ped.getAlias(), dp.getId(), ped.getDetectorType(),
                                ped.getAlarmLevel(), ped.getLimit(), ped.getDuration(), ped.getDurationType(),
                                boolToChar(ped.isBinaryState()), ped.getMultistateState(), ped.getChangeCount(),
                                ped.getAlphanumericState(), ped.getWeight() }, new int[] { Types.VARCHAR,
                                Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.DOUBLE,
                                Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
                                Types.VARCHAR, Types.DOUBLE }));
                AuditEventType.raiseAddedEvent(AuditEventType.TYPE_POINT_EVENT_DETECTOR, ped);
            }
            else {
                PointEventDetectorVO old = removeFromList(existingDetectors, ped.getId());

                ejt.update(
                        "update pointEventDetectors set xid=?, alias=?, alarmLevel=?, stateLimit=?, duration=?, "
                                + "  durationType=?, binaryState=?, multistateState=?, changeCount=?, alphanumericState=?, "
                                + "  weight=? " + "where id=?",
                        new Object[] { ped.getXid(), ped.getAlias(), ped.getAlarmLevel(), ped.getLimit(),
                                ped.getDuration(), ped.getDurationType(), boolToChar(ped.isBinaryState()),
                                ped.getMultistateState(), ped.getChangeCount(), ped.getAlphanumericState(),
                                ped.getWeight(), ped.getId() }, new int[] { Types.VARCHAR, Types.VARCHAR,
                                Types.INTEGER, Types.DOUBLE, Types.INTEGER, Types.INTEGER, Types.VARCHAR,
                                Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.DOUBLE, Types.INTEGER });

                AuditEventType.raiseChangedEvent(AuditEventType.TYPE_POINT_EVENT_DETECTOR, old, ped);
            }
        }

        // Delete detectors for any remaining ids in the list of existing
        // detectors.
        for (PointEventDetectorVO ped : existingDetectors) {
            ejt.update("delete from eventHandlers where eventTypeName=? and eventTypeRef1=? and eventTypeRef2=?",
                    new Object[] { EventType.EventTypeNames.DATA_POINT, dp.getId(), ped.getId() });
            ejt.update("delete from pointEventDetectors where id=?", new Object[] { ped.getId() });

            AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_POINT_EVENT_DETECTOR, ped);
        }
    }

    private PointEventDetectorVO removeFromList(List<PointEventDetectorVO> list, int id) {
        for (PointEventDetectorVO ped : list) {
            if (ped.getId() == id) {
                list.remove(ped);
                return ped;
            }
        }
        return null;
    }

    //
    //
    // Point comments
    //
    private static final String POINT_COMMENT_SELECT = UserCommentRowMapper.USER_COMMENT_SELECT
            + "where uc.commentType= " + UserComment.TYPE_POINT + " and uc.typeKey=? " + "order by uc.ts";

    private void setPointComments(DataPointVO dp) {
        dp.setComments(query(POINT_COMMENT_SELECT, new Object[] { dp.getId() }, new UserCommentRowMapper()));
    }
    
    
    /**
     * Get the
     * 
     * @return
     */
    public List<PointHistoryCount> getTopPointHistoryCounts() {
        if (Common.databaseProxy.getNoSQLProxy() == null)
            return this.getTopPointHistoryCountsSql();
        return this.getTopPointHistoryCountsNoSql();
    }

    private List<PointHistoryCount> getTopPointHistoryCountsNoSql() {

        PointValueDao dao = Common.databaseProxy.newPointValueDao();
        //For now we will do this the slow way
        List<DataPointVO> points = getDataPoints(DataPointExtendedNameComparator.instance, false);
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

        List<DataPointVO> points = getDataPoints(DataPointExtendedNameComparator.instance, false);

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

    //
    // Data point summaries
    private static final String DATA_POINT_SUMMARY_SELECT = //
    "select id, xid, dataSourceId, name, deviceName, pointFolderId, readPermission, setPermission from dataPoints ";

    public List<DataPointSummary> getDataPointSummaries(Comparator<IDataPoint> comparator) {

        List<DataPointSummary> dps = query(DATA_POINT_SUMMARY_SELECT, new ResultSetExtractor<List<DataPointSummary>>() {

            @Override
            public List<DataPointSummary> extractData(ResultSet rs) throws SQLException, DataAccessException {
                DataPointSummaryRowMapper rowMapper = new DataPointSummaryRowMapper();
                List<DataPointSummary> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    try {
                        results.add(rowMapper.mapRow(rs, rowNum++));
                    }
                    catch (ShouldNeverHappenException e) {
                        // If the module was removed but there are still records in the database, this exception will be
                        // thrown. Check the inner exception to confirm.
                        if (e.getCause() instanceof ObjectStreamException) {
                            // Yep. Log the occurrence and continue.
                            LOG.error("Data point with id '" + rs.getInt("id") + "' and name '" + rs.getString("name")
                                    + "' could not be loaded. Is its module missing?", e);
                        }
                    }
                }
                return results;

            }

        });

        if (comparator != null)
            Collections.sort(dps, comparator);
        return dps;
    }

    class DataPointSummaryRowMapper implements RowMapper<DataPointSummary> {
        @Override
        public DataPointSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;
            DataPointSummary d = new DataPointSummary();
            d.setId(rs.getInt(++i));
            d.setXid(rs.getString(++i));
            d.setDataSourceId(rs.getInt(++i));
            d.setName(rs.getString(++i));
            d.setDeviceName(rs.getString(++i));
            d.setPointFolderId(rs.getInt(++i));
            d.setReadPermission(rs.getString(++i));
            d.setSetPermission(rs.getString(++i));
            return d;
        }
    }

    //
    //
    // Point hierarchy
    //
    static PointHierarchy cachedPointHierarchy;

    public PointHierarchy getPointHierarchy(boolean useCache) {
        if (cachedPointHierarchy == null || !useCache) {
            final Map<Integer, List<PointFolder>> folders = new HashMap<>();

            // Get the folder list.
            ejt.query("select id, parentId, name from dataPointHierarchy order by name", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    PointFolder f = new PointFolder(rs.getInt(1), rs.getString(3));
                    int parentId = rs.getInt(2);
                    List<PointFolder> folderList = folders.get(parentId);
                    if (folderList == null) {
                        folderList = new LinkedList<>();
                        folders.put(parentId, folderList);
                    }
                    folderList.add(f);
                }
            });

            // Create the folder hierarchy.
            PointHierarchy ph = new PointHierarchy();
            addFoldersToHeirarchy(ph, 0, folders);

            // Add data points.
            List<DataPointSummary> points = getDataPointSummaries(DataPointExtendedNameComparator.instance);
            for (DataPointSummary dp : points)
                ph.addDataPoint(dp.getPointFolderId(), dp);

            cachedPointHierarchy = ph;
        }

        return cachedPointHierarchy;
    }

    public static void clearPointHierarchyCache() {
        cachedPointHierarchy = null;
        PointHierarchyEventDispatcher.firePointHierarchyCleared();
    }

    private void addFoldersToHeirarchy(PointHierarchy ph, int parentId, Map<Integer, List<PointFolder>> folders) {
        List<PointFolder> folderList = folders.remove(parentId);
        if (folderList == null)
            return;

        for (PointFolder f : folderList) {
            ph.addPointFolder(f, parentId);
            addFoldersToHeirarchy(ph, f.getId(), folders);
        }
    }

    public void savePointHierarchy(final PointFolder root) {
        // Assign ids to the folders.
        final List<Object> params = new ArrayList<>();
        final AtomicInteger folderId = new AtomicInteger(0);
        for (PointFolder sf : root.getSubfolders())
            assignFolderIds(sf, 0, folderId, params);

        cachedPointHierarchy = new PointHierarchy(root);
        PointHierarchyEventDispatcher.firePointHierarchySaved(root);

        final ExtendedJdbcTemplate ejt2 = ejt;
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                // Dump the hierarchy table.
                ejt2.update("DELETE FROM dataPointHierarchy");

                // Reset the current point folders values in the points.
                ejt2.update("UPDATE dataPoints SET pointFolderId=0");

                // Save the point folders.
                if (folderId.get() > 0) {
                    StringBuilder sql = new StringBuilder();
                    sql.append("INSERT INTO dataPointHierarchy (id, parentId, name) VALUES ");
                    for (int i = 0; i < folderId.get(); i++) {
                        if (i > 0)
                            sql.append(",");
                        sql.append("(?,?,?)");
                    }
                    ejt2.update(sql.toString(), params.toArray(new Object[params.size()]));
                }

                // Save the folder ids for the points.
                savePointsInFolder(root);
            }
        });
    }

    private void assignFolderIds(PointFolder folder, int parentId, AtomicInteger nextId, List<Object> params) {
        int id = nextId.incrementAndGet();
        folder.setId(id);
        params.add(id);
        params.add(parentId);
        params.add(StringUtils.abbreviate(folder.getName(), 100));
        for (DataPointSummary point : folder.getPoints())
            point.setPointFolderId(id);
        for (PointFolder sf : folder.getSubfolders())
            assignFolderIds(sf, id, nextId, params);
    }

    void savePointsInFolder(PointFolder folder) {
        // Save the points in the subfolders
        for (PointFolder sf : folder.getSubfolders())
            savePointsInFolder(sf);

        // Update the folder references in the points.
        if (!folder.getPoints().isEmpty()) {
            List<Integer> ids = new ArrayList<>(folder.getPoints().size());
            for (DataPointSummary p : folder.getPoints())
                ids.add(p.getId());
            ejt.update("UPDATE dataPoints SET pointFolderId=? WHERE id in (" + createDelimitedList(ids, ",", null)
                    + ")", folder.getId());
        }
    }

    /**
     * Methods for AbstractDao below here
     */

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractDao#getTableName()
     */
    @Override
    protected String getTableName() {
        return SchemaDefinition.DATAPOINTS_TABLE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
     */
    @Override
    protected String getXidPrefix() {
        return DataPointVO.XID_PREFIX;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractVO)
     */
    @Override
    protected Object[] voToObjectArray(DataPointVO vo) {
        return new Object[] { SerializationHelper.writeObject(vo), vo.getXid(), vo.getDataSourceId(), vo.getName(),
                vo.getDeviceName(), boolToChar(vo.isEnabled()), vo.getPointFolderId(), vo.getLoggingType(),
                vo.getIntervalLoggingPeriodType(), vo.getIntervalLoggingPeriod(), vo.getIntervalLoggingType(),
                vo.getTolerance(), boolToChar(vo.isPurgeOverride()), vo.getPurgeType(), vo.getPurgePeriod(),
                vo.getDefaultCacheSize(), boolToChar(vo.isDiscardExtremeValues()), vo.getEngineeringUnits(),
                vo.getReadPermission(), vo.getSetPermission(), };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
     */
    @Override
    public DataPointVO getNewVo() {
        return new DataPointVO();
    }


    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
    @Override
    protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
    	LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("id", Types.INTEGER);
		map.put("data", Types.BINARY); //Locator
		map.put("xid", Types.VARCHAR); //Xid
		map.put("dataSourceId",Types.INTEGER); //Dsid
		map.put("name", Types.VARCHAR); //Name
		map.put("deviceName", Types.VARCHAR); //Device Name
        map.put("enabled", Types.CHAR); //Enabled
        map.put("pointFolderId", Types.INTEGER); //Point Folder Id
        map.put("loggingType", Types.INTEGER); //Logging Type
        map.put("intervalLoggingPeriodType", Types.INTEGER); //Interval Logging Period Type
        map.put("intervalLoggingPeriod", Types.INTEGER); //Interval Logging Period
        map.put("intervalLoggingType", Types.INTEGER); //Interval Logging Type
        map.put("tolerance", Types.DOUBLE); //Tolerance
        map.put("purgeOverride", Types.CHAR); //Purge Override
        map.put("purgeType", Types.INTEGER); //Purge Type
        map.put("purgePeriod", Types.INTEGER); //Purge Period
        map.put("defaultCacheSize", Types.INTEGER); //Default Cache Size
        map.put("discardExtremeValues", Types.CHAR); //Discard Extremem Values
        map.put("engineeringUnits", Types.INTEGER); //get Engineering Units
        map.put("readPermission", Types.VARCHAR); // Read permission
        map.put("setPermission", Types.VARCHAR); // Set permission
        map.put("templateId", Types.INTEGER); //Template ID FK

        return map;
    }

    @Override
    protected Map<String, Comparator<DataPointVO>> getComparatorMap() {
        HashMap<String, Comparator<DataPointVO>> comparatorMap = new HashMap<>();

        comparatorMap.put("dataTypeString", new Comparator<DataPointVO>() {
            @Override
            public int compare(DataPointVO lhs, DataPointVO rhs) {
                return lhs.getDataTypeString().compareTo(rhs.getDataTypeString());
            }
        });

        comparatorMap.put("loggingTypeString", new Comparator<DataPointVO>() {
            @Override
            public int compare(DataPointVO lhs, DataPointVO rhs) {
                return lhs.getLoggingTypeString().compareTo(rhs.getLoggingTypeString());
            }
        });

        comparatorMap.put("loggingIntervalString", new Comparator<DataPointVO>() {
            @Override
            public int compare(DataPointVO lhs, DataPointVO rhs) {
                return lhs.getLoggingIntervalString().compareTo(rhs.getLoggingIntervalString());
            }
        });
        return comparatorMap;
    }

    @Override
    protected Map<String, IFilter<DataPointVO>> getFilterMap() {
        HashMap<String, IFilter<DataPointVO>> filterMap = new HashMap<>();

        filterMap.put("dataTypeString", new IFilter<DataPointVO>() {

            private String regex;

            @Override
            public boolean filter(DataPointVO vo) {
                return !vo.getDataTypeString().matches(regex);
            }

            @Override
            public void setFilter(Object matches) {
                this.regex = "(?i)" + (String) matches;

            }

        });

        filterMap.put("loggingTypeString", new IFilter<DataPointVO>() {
            private String regex;

            @Override
            public boolean filter(DataPointVO vo) {
                return !vo.getLoggingTypeString().matches(regex);
            }

            @Override
            public void setFilter(Object matches) {
                this.regex = "(?i)" + (String) matches;

            }
        });

        filterMap.put("loggingIntervalString", new IFilter<DataPointVO>() {
            private String regex;

            @Override
            public boolean filter(DataPointVO vo) {
                return !vo.getLoggingIntervalString().matches(regex);
            }

            @Override
            public void setFilter(Object matches) {
                this.regex = "(?i)" + (String) matches;

            }
        });
        return filterMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
     */
    @Override
    protected Map<String, IntStringPair> getPropertiesMap() {
        HashMap<String, IntStringPair> map = new HashMap<String, IntStringPair>();
        map.put("dataSourceName", new IntStringPair(Types.VARCHAR, "ds.name"));
        map.put("dataSourceTypeName", new IntStringPair(Types.VARCHAR, "ds.dataSourceType"));
        map.put("dataSourceXid", new IntStringPair(Types.VARCHAR, "ds.xid"));
        map.put("templateName", new IntStringPair(Types.VARCHAR, "template.name"));
        return map;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
     */
    @Override
    public RowMapper<DataPointVO> getRowMapper() {
        return new DataPointMapper();
    }

    class DataPointMapper implements RowMapper<DataPointVO> {
        @Override
        public DataPointVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;
            int id = (rs.getInt(++i));

            //TODO Should catch Stream exceptions when a module is missing for an existing Datasource.
            DataPointVO dp = (DataPointVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(++i));

            dp.setId(id);
            dp.setXid(rs.getString(++i));
            dp.setDataSourceId(rs.getInt(++i));
            dp.setName(rs.getString(++i));
            dp.setDeviceName(rs.getString(++i));
            dp.setEnabled(charToBool(rs.getString(++i)));
            dp.setPointFolderId(rs.getInt(++i));
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
            dp.setReadPermission(rs.getString(++i));
            dp.setSetPermission(rs.getString(++i));
            //Because we read 0 for null
            dp.setTemplateId(rs.getInt(++i));
            if(rs.wasNull())
            	dp.setTemplateId(null);

            // Data source information from Extra Joins set in Constructor
            dp.setDataSourceName(rs.getString(++i));
            dp.setDataSourceXid(rs.getString(++i));
            dp.setDataSourceTypeName(rs.getString(++i));
            dp.setTemplateName(rs.getString(++i));

            dp.ensureUnitsCorrect();
            return dp;
        }
    }

    @Override
    public DataPointVO getFull(int id) {
        //Get the values from local table
        DataPointVO vo = this.get(id);

        this.loadDataSource(vo);
        this.setEventDetectors(vo);
        this.setPointComments(vo);
        this.setTemplateName(vo);
        return vo;
    }

    /**
     * Load the datasource info into the DataPoint
     * 
     * @param vo
     * @return
     */
    public void loadDataSource(DataPointVO vo) {

        //Get the values from the datasource table
        //TODO Could speed this up if necessary...
        DataSourceVO<?> dsVo = DataSourceDao.instance.get(vo.getDataSourceId());
        vo.setDataSourceName(dsVo.getName());
        vo.setDataSourceTypeName(dsVo.getDefinition().getDataSourceTypeName());
        vo.setDataSourceXid(dsVo.getXid());

    }

    @Override
    public List<DataPointVO> getAllFull() {
        List<DataPointVO> list = this.getAll();
        for (DataPointVO vo : list) {
            this.loadDataSource(vo);
            this.setEventDetectors(vo);
            this.setPointComments(vo);
            this.setTemplateName(vo);
        }
        return list;
    }

    public void setTemplateName(DataPointVO vo){
    	if(vo.getTemplateId() != null){
	    	BaseTemplateVO<?> template = TemplateDao.instance.get(vo.getTemplateId());
	    	if(template != null)
	    		vo.setTemplateName(template.getName());
    	}
    }
    /**
     * Persist the vo or if it already exists update it
     * 
     * @param vo
     *            to save
     */
    @Override
    public void save(DataPointVO vo) {
        if (vo.getId() == Common.NEW_ID) {
            insert(vo);
        }
        else {
            update(vo);
        }
    }

    @Override
    public void saveFull(DataPointVO vo) {
        //TODO Eventually Fix this up by using the new AbstractDao for the query
        this.saveDataPoint(vo);
    }

    /**
     * 
     * Overridable method to extract the data
     * 
     * @return
     */
    @Override
    public ResultSetExtractor<List<DataPointVO>> getResultSetExtractor(final RowMapper<DataPointVO> rowMapper,
            final FilterListCallback<DataPointVO> filters) {

        return new ResultSetExtractor<List<DataPointVO>>() {
            List<DataPointVO> results = new ArrayList<>();
            int rowNum = 0;

            @Override
            public List<DataPointVO> extractData(ResultSet rs) throws SQLException, DataAccessException {
                while (rs.next()) {
                    try {
                        DataPointVO row = rowMapper.mapRow(rs, rowNum);
                        //Should we filter the row?
                        if (!filters.filterRow(row, rowNum++))
                            results.add(row);
                    }
                    catch (ShouldNeverHappenException e) {
                        // If the module was removed but there are still records in the database, this exception will be
                        // thrown. Check the inner exception to confirm.
                        if (e.getCause() instanceof ObjectStreamException) {
                            // Yep. Log the occurrence and continue.
                            LOG.error("Data point with xid '" + rs.getString("xid")
                                    + "' could not be loaded. Is its module missing?", e);
                        }
                        else {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
                return results;
            }
        };
    }

	/**
	 * Get a list of all Data Points using a given template
	 * @param id
	 * @return
	 */
	public List<DataPointVO> getByTemplate(int id) {
		List<DataPointVO> dps = query(DATA_POINT_SELECT + " WHERE templateId=?", new Object[]{id}, new DataPointRowMapper());
		setRelationalData(dps); //We will need this to restart the points after updating them
		return dps;
	}

	/**
	 * @param node
	 * @param permissions
	 * @param updateSetPermissions - true will update set, false will update read
	 * @return
	 */
	public long bulkUpdatePermissions(ASTNode root, String permissions,
			boolean updateSetPermissions) {
		
		long count;
		DataPointRestartCallback callback = new DataPointRestartCallback();
		
		if(root == null){
			//All points
			if(updateSetPermissions)
				count = ejt.update("UPDATE dataPoints AS dp SET dp.setPermission = CASE WHEN (dp.setPermission IS NULL) THEN (?) ELSE CONCAT(dp.setPermission, CONCAT(',',?)) END ", new Object[]{permissions, permissions});
			else
				count = ejt.update("UPDATE dataPoints AS dp SET dp.readPermission = CASE WHEN (dp.readPermission IS NULL) THEN (?) ELSE CONCAT(dp.readPermission, CONCAT(',',?)) END ", new Object[]{permissions, permissions});
			//Walk through all the DPs and restart them
			this.query(this.SELECT_ALL, new Object[]{}, this.getRowMapper(), callback);
		}else{
			//TODO Pass in The ModelMap and Modifiers on next CORE upgrade to 2.8.x
			Map<String,String> modelMap = new HashMap<String, String>();
			Map<String, SQLColumnQueryAppender> modifiers = new HashMap<String, SQLColumnQueryAppender>();
			StreamableSqlQuery<DataPointVO> query = this.createQuery(root, callback, null, modelMap, modifiers);
			SQLStatement updateStatement;
			List<Object> selectArgs = new ArrayList<Object>();	

			selectArgs.add(permissions);
			selectArgs.add(permissions);
			
			if(updateSetPermissions){
				switch(Common.databaseProxy.getType()){
				case H2:
					updateStatement = new SQLStatement("UPDATE dataPoints AS dp SET dp.setPermission = CASE WHEN (dp.setPermission IS NULL) THEN (?) ELSE CONCAT(dp.setPermission, CONCAT(',',?)) END WHERE dp.id IN (SELECT dp.id FROM dataPoints JOIN dataSources ds on ds.id = dp.dataSourceId ", selectArgs, COUNT);
					root.accept(new RQLToSQLSelect<DataPointVO>(this), updateStatement);
					count = ejt.update(updateStatement.getSelectSql() + " );", updateStatement.getSelectArgs().toArray());
					query.query();
					break;
				case DERBY:
				case MSSQL:
				case MYSQL:
				case POSTGRES:
				default:
					updateStatement = new SQLStatement("UPDATE dataPoints AS dp, dataSources AS ds SET dp.setPermission = CASE WHEN (dp.setPermission IS NULL) THEN (?) ELSE CONCAT(dp.setPermission, CONCAT(',',?)) END ", selectArgs, COUNT);
					root.accept(new RQLToSQLSelect<DataPointVO>(this), updateStatement);
					count = ejt.update(updateStatement.getSelectSql() + " AND ds.id = dp.dataSourceId", updateStatement.getSelectArgs().toArray());
					query.query();
					break;
				}
			}else{
				switch(Common.databaseProxy.getType()){
				case H2:
					updateStatement = new SQLStatement("UPDATE dataPoints AS dp SET dp.readPermission = CASE WHEN (dp.readPermission IS NULL) THEN (?) ELSE CONCAT(dp.readPermission, CONCAT(',',?)) END WHERE dp.id IN (SELECT dp.id FROM dataPoints JOIN dataSources ds on ds.id = dp.dataSourceId ", selectArgs, COUNT);
					root.accept(new RQLToSQLSelect<DataPointVO>(this), updateStatement);
					count = ejt.update(updateStatement.getSelectSql() + " );", updateStatement.getSelectArgs().toArray());
					query.query();
					break;
				case DERBY:
				case MSSQL:
				case MYSQL:
				case POSTGRES:
				default:
					updateStatement = new SQLStatement("UPDATE dataPoints AS dp, dataSources AS ds SET dp.readPermission = CASE WHEN (dp.readPermission IS NULL) THEN (?) ELSE CONCAT(dp.readPermission, CONCAT(',',?)) END ", selectArgs, COUNT);
					root.accept(new RQLToSQLSelect<DataPointVO>(this), updateStatement);
					count = ejt.update(updateStatement.getSelectSql() + " AND ds.id = dp.dataSourceId", updateStatement.getSelectArgs().toArray());
					query.query();
					break;
				}
			}
		}
		//For sanity
		if(count != callback.getCount()){
			throw new ShouldNeverHappenException("Changed more permissions than for found datapoints!");
		}
		
		return count;
		
	}

	/**
	 * @param node
	 * @param updateSetPermissions - true will update set, false will update read
	 * @return
	 * 
	 */
	public long bulkClearPermissions(ASTNode root, boolean updateSetPermissions) {

		long count;
		DataPointRestartCallback callback = new DataPointRestartCallback();
		if(root == null){
			//All points
			if(updateSetPermissions)
				count = ejt.update("UPDATE dataPoints AS dp SET dp.setPermission = null");
			else
				count = ejt.update("UPDATE dataPoints AS dp SET dp.readPermission = null");
		
			//Walk through all the DPs and restart them
			this.query(this.SELECT_ALL, new Object[]{}, this.getRowMapper(), callback);
		}else{
			//TODO Pass in The ModelMap and Modifiers on next CORE upgrade to 2.8.x
			Map<String,String> modelMap = new HashMap<String, String>();
			Map<String, SQLColumnQueryAppender> modifiers = new HashMap<String, SQLColumnQueryAppender>();
			StreamableSqlQuery<DataPointVO> query = this.createQuery(root, callback, null, modelMap, modifiers);
			SQLStatement updateStatement;
			List<Object> selectArgs = new ArrayList<Object>();	

			if(updateSetPermissions){
				switch(Common.databaseProxy.getType()){
				case H2:
					updateStatement = new SQLStatement("UPDATE dataPoints AS dp SET dp.setPermission = null WHERE dp.id IN (SELECT dp.id FROM dataPoints JOIN dataSources ds on ds.id = dp.dataSourceId ", selectArgs, COUNT);
					root.accept(new RQLToSQLSelect<DataPointVO>(this), updateStatement);
					count = ejt.update(updateStatement.getSelectSql() + " );", updateStatement.getSelectArgs().toArray());
					query.query();
					break;
				case DERBY:
				case MSSQL:
				case MYSQL:
				case POSTGRES:
				default:
					updateStatement = new SQLStatement("UPDATE dataPoints AS dp, dataSources as ds SET dp.setPermission = null ", selectArgs, COUNT);
					root.accept(new RQLToSQLSelect<DataPointVO>(this), updateStatement);
					count = ejt.update(updateStatement.getSelectSql() + " AND ds.id = dp.dataSourceId", updateStatement.getSelectArgs().toArray());
					query.query();
					break;
				}
			}else{
				switch(Common.databaseProxy.getType()){
				case H2:
					updateStatement = new SQLStatement("UPDATE dataPoints AS dp SET dp.readPermission = null WHERE dp.id IN (SELECT dp.id FROM dataPoints JOIN dataSources ds on ds.id = dp.dataSourceId ", selectArgs, COUNT);
					root.accept(new RQLToSQLSelect<DataPointVO>(this), updateStatement);
					count = ejt.update(updateStatement.getSelectSql() + " );", updateStatement.getSelectArgs().toArray());
					query.query();
					break;
				case DERBY:
				case MSSQL:
				case MYSQL:
				case POSTGRES:
				default:
					updateStatement = new SQLStatement("UPDATE dataPoints AS dp, dataSources as ds SET dp.readPermission = null ", selectArgs, COUNT);
					root.accept(new RQLToSQLSelect<DataPointVO>(this), updateStatement);
					count = ejt.update(updateStatement.getSelectSql() + " AND ds.id = dp.dataSourceId", updateStatement.getSelectArgs().toArray());
					query.query();
					break;
				}
			}
		}
		
		//For sanity
		if(count != callback.getCount()){
			throw new ShouldNeverHappenException("Changed more permissions than for found datapoints!");
		}
		
		return count;
	}
	
	/*
	 * Assumes Event Detectors are not yet set
	 */
	class DataPointRestartCallback implements MappedRowCallback<DataPointVO>{
		
		private long count;
		
		@Override
		public void row(DataPointVO dp, int index) {
			count++;
			if(Common.runtimeManager.isDataPointRunning(dp.getId())){
				try{
					setEventDetectors(dp);
					Common.runtimeManager.restartDataPoint(dp);
				}catch(Exception e){
					LOG.error(e.getMessage(), e);
				}
			}	
		}
		
		public long getCount(){
			return count;
		}
	}

}
