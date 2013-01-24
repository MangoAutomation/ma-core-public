/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

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
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchyEventDispatcher;
import com.serotonin.util.SerializationHelper;
import com.serotonin.util.Tuple;

public class DataPointDao extends BaseDao {
    //
    //
    // Data Points
    //
    public String generateUniqueXid() {
        return generateUniqueXid(DataPointVO.XID_PREFIX, "dataPoints");
    }

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
            + "  dp.discardExtremeValues, dp.engineeringUnits, ds.name, ds.xid, ds.dataSourceType " //
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
        List<DataPointVO> dps = query(DATA_POINT_SELECT + " where dp.dataSourceId=?", new Object[] { dataSourceId },
                new DataPointRowMapper());
        setRelationalData(dps);
        if (comparator != null)
            Collections.sort(dps, comparator);
        return dps;
    }

    public DataPointVO getDataPoint(int id) {
        return getDataPoint(id, true);
    }

    public DataPointVO getDataPoint(int id, boolean includeRelationalData) {
        DataPointVO dp = queryForObject(DATA_POINT_SELECT + " where dp.id=?", new Object[] { id },
                new DataPointRowMapper(), null);
        if (includeRelationalData)
            setRelationalData(dp);
        return dp;
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
            DataPointVO dp = (DataPointVO) SerializationHelper.readObjectInContext(rs.getBlob(++i).getBinaryStream());
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

            // Data source information.
            dp.setDataSourceName(rs.getString(++i));
            dp.setDataSourceXid(rs.getString(++i));
            dp.setDataSourceTypeName(rs.getString(++i));

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

                // Reset the point hierarchy so that the new or changed point gets reflected.
                cachedPointHierarchy = null;
            }
        });
    }

    void insertDataPoint(final DataPointVO dp) {
        for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
            def.beforeInsert(dp);

        // Create a default text renderer
        if (dp.getTextRenderer() == null)
            dp.defaultTextRenderer();

        dp.setId(doInsert(
                "insert into dataPoints (xid, dataSourceId, name, deviceName, enabled, pointFolderId, loggingType, " //
                        + "intervalLoggingPeriodType, intervalLoggingPeriod, intervalLoggingType, tolerance, " //
                        + "purgeOverride, purgeType, purgePeriod, defaultCacheSize, discardExtremeValues, " //
                        + "engineeringUnits, data) " //
                        + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", //
                new Object[] { dp.getXid(), dp.getDataSourceId(), dp.getName(), dp.getDeviceName(),
                        boolToChar(dp.isEnabled()), dp.getPointFolderId(), dp.getLoggingType(),
                        dp.getIntervalLoggingPeriodType(), dp.getIntervalLoggingPeriod(), dp.getIntervalLoggingType(),
                        dp.getTolerance(), boolToChar(dp.isPurgeOverride()), dp.getPurgeType(), dp.getPurgePeriod(),
                        dp.getDefaultCacheSize(), boolToChar(dp.isDiscardExtremeValues()), dp.getEngineeringUnits(),
                        SerializationHelper.writeObject(dp) }, //
                new int[] { Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.CHAR, Types.INTEGER,
                        Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.DOUBLE, Types.CHAR,
                        Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.CHAR, Types.INTEGER, Types.BLOB }));

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
            // Delete any point values where data type doesn't match the vo, just in case the data type was changed.
            // Only do this if the data type has actually changed because it is just really slow if the database is
            // big or busy.
            new PointValueDao().deletePointValuesWithMismatchedType(dp.getId(), dp.getPointLocator().getDataTypeId());

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
                        + "discardExtremeValues=?, engineeringUnits=?, data=? where id=?", //
                new Object[] { dp.getXid(), dp.getName(), dp.getDeviceName(), boolToChar(dp.isEnabled()),
                        dp.getPointFolderId(), dp.getLoggingType(), dp.getIntervalLoggingPeriodType(),
                        dp.getIntervalLoggingPeriod(), dp.getIntervalLoggingType(), dp.getTolerance(),
                        boolToChar(dp.isPurgeOverride()), dp.getPurgeType(), dp.getPurgePeriod(),
                        dp.getDefaultCacheSize(), boolToChar(dp.isDiscardExtremeValues()), dp.getEngineeringUnits(),
                        SerializationHelper.writeObject(dp), dp.getId() }, //
                new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CHAR, Types.INTEGER, Types.INTEGER,
                        Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.DOUBLE, Types.CHAR, Types.INTEGER,
                        Types.INTEGER, Types.INTEGER, Types.CHAR, Types.INTEGER, Types.BLOB, Types.INTEGER });
    }

    public void deleteDataPoints(final int dataSourceId) {
        List<DataPointVO> old = getDataPoints(dataSourceId, null);

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
                    deleteDataPointImpl(createDelimitedList(new HashSet<Integer>(pointIds), ",", null));
            }
        });

        for (DataPointVO dp : old) {
            for (DataPointChangeDefinition def : ModuleRegistry.getDefinitions(DataPointChangeDefinition.class))
                def.afterDelete(dp.getId());
            AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_DATA_POINT, dp);
        }
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
        ejt.update("delete from dataPointUsers where dataPointId in " + dataPointIdList);
        ejt.update("delete from dataPoints where id in " + dataPointIdList);

        cachedPointHierarchy = null;
    }

    public int countPointsForDataSourceType(String dataSourceType) {
        return ejt.queryForInt("SELECT count(*) FROM dataPoints dp LEFT JOIN dataSources ds ON dp.dataSourceId=ds.id "
                + "WHERE ds.dataSourceType=?", new Object[] { dataSourceType });
    }

    //
    //
    // Event detectors
    //
    public int getDataPointIdFromDetectorId(int pedId) {
        return ejt.queryForInt("select dataPointId from pointEventDetectors where id=?", new Object[] { pedId });
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
                new Object[] { dataPointId, xid, excludeId }) == 0;
    }

    private void setEventDetectors(DataPointVO dp) {
        dp.setEventDetectors(getEventDetectors(dp));
    }

    private List<PointEventDetectorVO> getEventDetectors(DataPointVO dp) {
        return query(
                "select id, xid, alias, detectorType, alarmLevel, stateLimit, duration, durationType, binaryState, "
                        + "  multistateState, changeCount, alphanumericState, weight " + "from pointEventDetectors "
                        + "where dataPointId=? " + "order by id", new Object[] { dp.getId() },
                new EventDetectorRowMapper(dp));
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
                ped.setId(doInsert(
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

        // Delete detectors for any remaining ids in the list of existing detectors.
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

    public void copyPermissions(final int fromDataPointId, final int toDataPointId) {
        final List<Tuple<Integer, Integer>> ups = query(
                "select userId, permission from dataPointUsers where dataPointId=?", new Object[] { fromDataPointId },
                new RowMapper<Tuple<Integer, Integer>>() {
                    @Override
                    public Tuple<Integer, Integer> mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Tuple<Integer, Integer>(rs.getInt(1), rs.getInt(2));
                    }
                });

        ejt.batchUpdate("insert into dataPointUsers values (?,?,?)", new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return ups.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, toDataPointId);
                ps.setInt(2, ups.get(i).getElement1());
                ps.setInt(3, ups.get(i).getElement2());
            }
        });
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

    public List<PointHistoryCount> getTopPointHistoryCounts() {
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
    "select id, xid, dataSourceId, name, deviceName, pointFolderId from dataPoints ";

    public List<DataPointSummary> getDataPointSummaries(Comparator<IDataPoint> comparator) {
        List<DataPointSummary> dps = query(DATA_POINT_SUMMARY_SELECT, new DataPointSummaryRowMapper());
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
            final Map<Integer, List<PointFolder>> folders = new HashMap<Integer, List<PointFolder>>();

            // Get the folder list.
            ejt.query("select id, parentId, name from dataPointHierarchy order by name", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    PointFolder f = new PointFolder(rs.getInt(1), rs.getString(3));
                    int parentId = rs.getInt(2);
                    List<PointFolder> folderList = folders.get(parentId);
                    if (folderList == null) {
                        folderList = new LinkedList<PointFolder>();
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
        final List<Object> params = new ArrayList<Object>();
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
            List<Integer> ids = new ArrayList<Integer>(folder.getPoints().size());
            for (DataPointSummary p : folder.getPoints())
                ids.add(p.getId());
            ejt.update("UPDATE dataPoints SET pointFolderId=? WHERE id in (" + createDelimitedList(ids, ",", null)
                    + ")", folder.getId());
        }
    }
}
