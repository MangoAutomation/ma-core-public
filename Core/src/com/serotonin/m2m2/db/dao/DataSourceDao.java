/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.util.SerializationHelper;

@Repository()
public class DataSourceDao<T extends DataSourceVO<?>> extends AbstractDao<T> {

    public static final Name DATA_SOURCES_ALIAS = DSL.name("ds");
    public static final Table<Record> DATA_SOURCES = DSL.table(DSL.name(SchemaDefinition.DATASOURCES_TABLE)).as(DATA_SOURCES_ALIAS);
    public static final Field<Integer> ID = DSL.field(DATA_SOURCES_ALIAS.append("id"), SQLDataType.INTEGER.nullable(false));
    public static final Field<String> EDIT_PERMISSION = DSL.field(DATA_SOURCES_ALIAS.append("editPermission"), SQLDataType.VARCHAR(255).nullable(true));
    
    static final Log LOG = LogFactory.getLog(DataSourceDao.class);
    private static final String DATA_SOURCE_SELECT = //
    "SELECT id, xid, name, dataSourceType, data, editPermission FROM dataSources ";

    @SuppressWarnings("unchecked")
    private static final LazyInitSupplier<DataSourceDao<DataSourceVO<?>>> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(DataSourceDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (DataSourceDao<DataSourceVO<?>>)o;
    });

    private DataSourceDao() {
        super(AuditEventType.TYPE_DATA_SOURCE, new TranslatableMessage("internal.monitor.DATA_SOURCE_COUNT"));
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static DataSourceDao<DataSourceVO<?>> getInstance() {
        return springInstance.get();
    }
    
    public List<T> getDataSources() {
        List<T> dss = query(DATA_SOURCE_SELECT, new DataSourceExtractor());
        Collections.sort(dss, new DataSourceNameComparator());
        return dss;
    }

    public List<T> getDataSourcesForType(String type) {
        return query(DATA_SOURCE_SELECT + "WHERE dataSourceType=?", new Object[] { type }, new DataSourceExtractor());
    }

    static class DataSourceNameComparator implements Comparator<DataSourceVO<?>> {
        @Override
        public int compare(DataSourceVO<?> ds1, DataSourceVO<?> ds2) {
            if (StringUtils.isBlank(ds1.getName()))
                return -1;
            return ds1.getName().compareToIgnoreCase(ds2.getName());
        }
    }

    public DataSourceVO<?> getDataSource(int id) {
        return queryForObject(DATA_SOURCE_SELECT + " WHERE id=?", new Object[] { id }, new DataSourceRowMapper(), null);
    }

    public DataSourceVO<?> getDataSource(String xid) {
        return queryForObject(DATA_SOURCE_SELECT + " WHERE xid=?", new Object[] { xid }, new DataSourceRowMapper(),
                null);
    }

    class DataSourceExtractor implements ResultSetExtractor<List<T>> {
        @Override
        public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
            DataSourceRowMapper rowMapper = new DataSourceRowMapper();
            List<T> results = new ArrayList<>();
            int rowNum = 0;
            while (rs.next()) {
                try {
                    results.add(rowMapper.mapRow(rs, rowNum++));
                }
                catch (ShouldNeverHappenException e) {
                    // If the module was removed but there are still records in the database, this exception will be
                    // thrown. Check the inner exception to confirm.
                    if (e.getCause() instanceof ModuleNotLoadedException) {
                        // Yep. Log the occurrence and continue.
                        LOG.error(
                                "Data source with type '" + rs.getString("dataSourceType") + "' and xid '"
                                        + rs.getString("xid") + "' could not be loaded. Is its module missing?", e.getCause());
                    }else {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
            return results;
        }
    }

    class DataSourceRowMapper implements RowMapper<T> {
        @Override
        public T mapRow(ResultSet rs, int rowNum) throws SQLException {
            @SuppressWarnings("unchecked")
			T ds = (T) SerializationHelper.readObjectInContext(rs.getBinaryStream(5));
            ds.setId(rs.getInt(1));
            ds.setXid(rs.getString(2));
            ds.setName(rs.getString(3));
            ds.setDefinition(ModuleRegistry.getDataSourceDefinition(rs.getString(4)));
            ds.setEditPermission(rs.getString(6));
            return ds;
        }
    }

    @Override
    public String generateUniqueXid() {
        return generateUniqueXid(DataSourceVO.XID_PREFIX, tableName);
    }

    public void saveDataSource(final T vo) {
        // Decide whether to insert or update.
        if (vo.getId() == Common.NEW_ID)
            insertDataSource(vo);
        else
            updateDataSource(vo);
    }

    private void insertDataSource(final T vo) {
        vo.setId(ejt.doInsert(
                "INSERT INTO dataSources (xid, name, dataSourceType, data, editPermission) values (?,?,?,?,?)",
                new Object[] { vo.getXid(), vo.getName(), vo.getDefinition().getDataSourceTypeName(),
                        SerializationHelper.writeObject(vo), vo.getEditPermission() }, new int[] { Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.BINARY, Types.VARCHAR }));

        AuditEventType.raiseAddedEvent(AuditEventType.TYPE_DATA_SOURCE, vo);
        this.countMonitor.increment();
    }

    /**
     * Update a data source by calling Super.save(vo)
     * @param vo
     */
    private void updateDataSource(final T vo) {
        super.save(vo);
    }

    public void _updateDataSource(T vo) {
        ejt.update("UPDATE dataSources SET xid=?, name=?, dataSourceType=?, data=?, editPermission WHERE id=?",
                new Object[] { vo.getXid(), vo.getName(), vo.getDefinition().getDataSourceTypeName(),
                        SerializationHelper.writeObject(vo), vo.getEditPermission(), vo.getId() }, new int[] {
                        Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BINARY, Types.VARCHAR, Types.INTEGER });
    }

    public void deleteDataSource(final int dataSourceId) {
        DataSourceVO<?> vo = getDataSource(dataSourceId);
        final ExtendedJdbcTemplate ejt2 = ejt;

        DataPointDao.getInstance().deleteDataPoints(dataSourceId);

        if (vo != null) {
            getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    ejt2.update("DELETE FROM eventHandlersMapping WHERE eventTypeName=? AND eventTypeRef1=?", new Object[] {
                            EventType.EventTypeNames.DATA_SOURCE, dataSourceId });
                    ejt2.update("DELETE FROM dataSources WHERE id=?", new Object[] { dataSourceId });
                }
            });

            AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_DATA_SOURCE, vo);
            this.countMonitor.decrement();
        }
    }

    public void deleteDataSourceType(final String dataSourceType) {
        List<Integer> dsids = queryForList("SELECT id FROM dataSources WHERE dataSourceType=?",
                new Object[] { dataSourceType }, Integer.class);
        for (Integer dsid : dsids)
            deleteDataSource(dsid);
    }

    //TODO See how this works, with the Device name situation
    @Override
    public int copy(int existingId, String newXid, String newName) {
        return this.copyDataSource(existingId, newName, newXid, newName + "Device");
    }

    /**
     * Copy a data source points
     * 
     * @param dataSourceId
     * @param newDataSourceId
     * @return
     */
    public int copyDataSourcePoints(final int dataSourceId, final int newDataSourceId, String deviceName) {
        return getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                DataPointDao dataPointDao = DataPointDao.getInstance();

                // Copy the data source.
                DataSourceVO<?> dataSourceCopy = getDataSource(newDataSourceId);

                // Copy the points.
                for (DataPointVO dataPoint : dataPointDao.getDataPoints(dataSourceId, null)) {
                    DataPointVO dataPointCopy = dataPoint.copy();
                    dataPointCopy.setId(Common.NEW_ID);
                    dataPointCopy.setXid(dataPointDao.generateUniqueXid());
                    dataPointCopy.setName(dataPoint.getName());
                    dataPointCopy.setDeviceName(deviceName != null ? deviceName : dataSourceCopy.getName());
                    dataPointCopy.setDataSourceId(dataSourceCopy.getId());
                    dataPointCopy.setEnabled(dataPoint.isEnabled());
                    dataPointCopy.getComments().clear();

                    // Copy the event detectors
                    for (AbstractPointEventDetectorVO<?> ped : dataPointCopy.getEventDetectors()) {
                        ped.setId(Common.NEW_ID);
                        ped.njbSetDataPoint(dataPointCopy);
                        ped.setXid(EventDetectorDao.getInstance().generateUniqueXid());
                    }

                    //dataPointDao.saveDataPoint(dataPointCopy);
                    Common.runtimeManager.saveDataPoint(dataPointCopy);
                }

                return dataSourceCopy.getId();
            }
        });
    }

    /**
     * Copy Data Source Points and Source itself
     * 
     * @param dataSourceId
     * @param name
     * @param xid
     * @param deviceName
     * @return
     */
    public int copyDataSource(final int dataSourceId, final String name, final String xid, final String deviceName) {
        return getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                DataPointDao dataPointDao = DataPointDao.getInstance();

                DataSourceVO<?> dataSource = getDataSource(dataSourceId);
                // Copy the data source.
                DataSourceVO<?> dataSourceCopy = dataSource.copy();
                dataSourceCopy.setId(Common.NEW_ID);
                dataSourceCopy.setXid(xid);
                dataSourceCopy.setEnabled(false);
                dataSourceCopy.setName(name);
                Common.runtimeManager.saveDataSource(dataSourceCopy);
                
                // Copy the points.
                for (DataPointVO dataPoint : dataPointDao.getDataPoints(dataSourceId, null)) {
                    DataPointVO dataPointCopy = dataPoint.copy();
                    dataPointCopy.setId(Common.NEW_ID);
                    dataPointCopy.setXid(dataPointDao.generateUniqueXid());
                    dataPointCopy.setName(dataPoint.getName());
                    dataPointCopy.setDeviceName(deviceName);
                    dataPointCopy.setDataSourceId(dataSourceCopy.getId());
                    dataPointCopy.setEnabled(dataPoint.isEnabled());
                    dataPointCopy.getComments().clear();

                    // Copy the event detectors
                    for (AbstractPointEventDetectorVO<?> ped : dataPointCopy.getEventDetectors()) {
                        ped.setId(Common.NEW_ID);
                        ped.njbSetDataPoint(dataPointCopy);
                    }

                    Common.runtimeManager.saveDataPoint(dataPointCopy);
                }

                return dataSourceCopy.getId();
            }
        });
    }

    public Object getPersistentData(int id) {
        return query("select rtdata from dataSources where id=?", new Object[] { id },
                new ResultSetExtractor<Serializable>() {
                    @Override
                    public Serializable extractData(ResultSet rs) throws SQLException, DataAccessException {
                        if (!rs.next())
                            return null;

                        InputStream in = rs.getBinaryStream(1);
                        if (in == null)
                            return null;

                        return (Serializable) SerializationHelper.readObjectInContext(in);

                        //                        Blob blob = rs.getBlob(1);
                        //                        if (blob == null)
                        //                            return null;
                        //
                        //                        return (Serializable) SerializationHelper.readObjectInContext(blob.getBinaryStream());
                    }
                });
    }

    public void savePersistentData(int id, Object data) {
        ejt.update("UPDATE dataSources SET rtdata=? WHERE id=?", new Object[] { SerializationHelper.writeObject(data),
                id }, new int[] { Types.BINARY, Types.INTEGER });
    }

    public String getEditPermission(int id) {
        return ejt.queryForObject("SELECT editPermission FROM dataSources WHERE id=?", new Object[] { id },
                String.class, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractDao#getTableName()
     */
    @Override
    protected String getTableName() {
        return SchemaDefinition.DATASOURCES_TABLE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
     */
    @Override
    protected String getXidPrefix() {
        return DataSourceVO.XID_PREFIX;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractVO)
     */
    @Override
    protected Object[] voToObjectArray(T vo) {
        return new Object[] { vo.getXid(), vo.getName(), vo.getDefinition().getDataSourceTypeName(),
                SerializationHelper.writeObject(vo), vo.getEditPermission() };
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
     */
    @Override
    public T getNewVo() {
        throw new ShouldNeverHappenException("Unable to create generic data source, must supply a type");
    }


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
    @Override
    protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
    	LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
    	map.put("id", Types.INTEGER);
    	map.put("xid", Types.VARCHAR);
    	map.put("name", Types.VARCHAR);
    	map.put("dataSourceType", Types.VARCHAR);
    	map.put("data", Types.BINARY);
    	map.put("editPermission", Types.VARCHAR);
    	return map;
    }

    @Override
    protected Map<String, Comparator<T>> getComparatorMap() {
        HashMap<String, Comparator<T>> comparatorMap = new HashMap<>();

        comparatorMap.put("typeDescriptionString", new Comparator<T>() {
            @Override
            public int compare(T lhs, T rhs) {
                return lhs.getTypeDescriptionString().compareTo(rhs.getTypeDescriptionString());
            }
        });

        comparatorMap.put("connectionDescriptionString", new Comparator<T>() {
            @Override
            public int compare(T lhs, T rhs) {
                return lhs.getConnectionDescriptionString().compareTo(rhs.getConnectionDescriptionString());
            }
        });

        return comparatorMap;
    }

    @Override
    protected Map<String, IFilter<T>> getFilterMap() {
        HashMap<String, IFilter<T>> filterMap = new HashMap<>();

        filterMap.put("typeDescriptionString", new IFilter<T>() {

            private String regex;

            @Override
            public boolean filter(T vo) {
                return !vo.getTypeDescriptionString().matches(regex);
            }

            @Override
            public void setFilter(Object matches) {
                this.regex = "(?i)" + (String) matches;

            }

        });

        filterMap.put("connectionDescriptionString", new IFilter<T>() {
            private String regex;

            @Override
            public boolean filter(T vo) {
                return !vo.getConnectionDescriptionString().matches(regex);
            }

            @Override
            public void setFilter(Object matches) {
                this.regex = "(?i)" + (String) matches; //Make case insensitive like DB

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
        return new HashMap<>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
     */
    @Override
    public RowMapper<T> getRowMapper() {
        return new DataSourceRowMapper();
    }

    /**
     * 
     * Overridable method to extract the data
     * 
     * @return
     */
    @Override
    public ResultSetExtractor<List<T>> getResultSetExtractor(final RowMapper<T> rowMapper,
            final FilterListCallback<T> filters) {

        return new ResultSetExtractor<List<T>>() {
            List<T> results = new ArrayList<>();
            int rowNum = 0;

            @Override
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                while (rs.next()) {
                    try {
                        T row = rowMapper.mapRow(rs, rowNum);
                        //Should we filter the row?
                        if (!filters.filterRow(row, rowNum++))
                            results.add(row);
                    }
                    catch (ShouldNeverHappenException e) {
                        // If the module was removed but there are still records in the database, this exception will be
                        // thrown. Check the inner exception to confirm.
                        if (e.getCause() instanceof ModuleNotLoadedException) {
                            // Yep. Log the occurrence and continue.
                            String desc = "Data source with type '" + rs.getString("dataSourceType") + "' and xid '"
                                    + rs.getString("xid") + "' could not be loaded. Is its module missing?";
                            LOG.error(desc, e.getCause());
                        }else {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
                return results;

            }
        };

    }

}
