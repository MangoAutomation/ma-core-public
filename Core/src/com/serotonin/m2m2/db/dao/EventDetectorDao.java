/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao.DataPointRowMapper;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class EventDetectorDao<T extends AbstractEventDetectorVO<?>> extends AbstractDao<T> {

    @SuppressWarnings("unchecked")
    private static final LazyInitSupplier<EventDetectorDao<AbstractEventDetectorVO<?>>> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(EventDetectorDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (EventDetectorDao<AbstractEventDetectorVO<?>>)o;
    });

    /* Map of Source Type to Source ID Column Names */
    private LinkedHashMap<String, String> sourceTypeToColumnNameMap;
    
    private EventDetectorDao(){
        super(AuditEventType.TYPE_EVENT_DETECTOR, 
                "edt",
                new String[0],
                false,
                new TranslatableMessage("internal.monitor.EVENT_DETECTOR_COUNT"));
    }
    
    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static EventDetectorDao<AbstractEventDetectorVO<?>> getInstance() {
        return springInstance.get();
    }
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getTableName()
     */
    @Override
    protected String getTableName() {
        return SchemaDefinition.EVENT_DETECTOR_TABLE;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#voToObjectArray(java.lang.Object)
     */
    @Override
    protected Object[] voToObjectArray(T vo) {
        String jsonData = null;
        try{ 
            jsonData = writeValueAsString(vo);
        }catch(JsonException | IOException e){
            LOG.error(e.getMessage(), e);
        }
        
        //Find the index of our sourceIdColumn
        int sourceIdIndex = getSourceIdIndex(vo.getDefinition().getSourceTypeName());

        Object[] o = new Object[4 + this.sourceTypeToColumnNameMap.size()];
        o[0] = vo.getXid();
        o[1] = vo.getDetectorSourceType();
        o[2] = vo.getDetectorType();
        o[3] = jsonData;
        o[4 + sourceIdIndex] = vo.getSourceId();
        return o;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
     */
    @Override
    protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("id", Types.INTEGER);
        map.put("xid", Types.VARCHAR);
        map.put("sourceTypeName", Types.VARCHAR);
        map.put("typeName", Types.VARCHAR);
        map.put("data", Types.CLOB);
        
        //Build our ordered column set from the Module Registry
        List<EventDetectorDefinition<?>> defs = ModuleRegistry.getEventDetectorDefinitions();
        this.sourceTypeToColumnNameMap = new LinkedHashMap<String, String>(defs.size());
        for(EventDetectorDefinition<?> def : defs){
            this.sourceTypeToColumnNameMap.put(def.getSourceTypeName(), this.TABLE_PREFIX + "." + def.getSourceIdColumnName());
            map.put(def.getSourceIdColumnName(), Types.INTEGER);
        }

        return map;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
     */
    @Override
    protected Map<String, IntStringPair> getPropertiesMap() {
        HashMap<String, IntStringPair> map = new HashMap<String, IntStringPair>();
        map.put("detectorSourceType", new IntStringPair(Types.VARCHAR, this.TABLE_PREFIX + "." + "sourceTypeName"));
        return map;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
     */
    @Override
    public RowMapper<T> getRowMapper() {
        return new EventDetectorRowMapper<T>();
    }

    public JsonObject readValueFromString(String json) throws JsonException, IOException {
        JsonTypeReader reader = new JsonTypeReader(json);
        
        return (JsonObject)reader.read();
    }
    
    public static String writeValueAsString(AbstractEventDetectorVO<?> value) throws JsonException, IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.writeObject(value);
        return stringWriter.toString();

    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
     */
    @Override
    protected String getXidPrefix() {
        return AbstractEventDetectorVO.XID_PREFIX; 
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
     */
    @Override
    public T getNewVo() {
        throw new ShouldNeverHappenException("Unable to create generic event detector, must supply a type");
    }
    
    @Override
    public void delete(T vo, String initiatorId) {
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                if (vo != null) {
                    EventDetectorDao.super.delete(vo, initiatorId);
                    //Also update the Event Handlers
                    ejt.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1=? and eventTypeRef2=?",
                            new Object[] { vo.getEventType().getEventType().getEventType(), vo.getSourceId(), vo.getId() });
                }
            }
        });
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#saveRelationalData(com.serotonin.m2m2.vo.AbstractBasicVO, boolean)
     */
    @Override
    public void saveRelationalData(T vo, boolean insert) {
        if(vo.getAddedEventHandlers() != null) {
            EventTypeVO et = vo.getEventType();
            for(AbstractEventHandlerVO<?> ehVo : vo.getAddedEventHandlers())
                EventHandlerDao.getInstance().addEventHandlerMappingIfMissing(ehVo.getId(), et.getEventType());
        }
    }
    
    /**
     * Get all data point event detectors with the corresponding sourceId AND the point loaded into it
     * Ordered by detector id.
     * 
     * @param sourceId
     * @return
     */
    public List<AbstractPointEventDetectorVO<?>> getWithSource(int sourceId, DataPointVO dp){
        String sourceIdColumnName = getSourceIdColumnName(EventType.EventTypeNames.DATA_POINT);
        return query(SELECT_ALL + " WHERE " + sourceIdColumnName +  "=? ORDER BY id", new Object[]{sourceId}, new PointEventDetectorRowMapper(dp));
    }

    /**
     * Get the XID for a given row
     * 
     * @param id
     * @return
     */
    public String getXid(int id) {
        return queryForObject("SELECT xid from " + this.tableName + " WHERE id=?", new Object[]{id}, String.class, null);
    }

    /**
     * Get the id for a given row
     * @param xid
     * @return
     */
    public int getId(String xid, int dpId) {
        return queryForObject("SELECT id from " + this.tableName + " WHERE xid=? AND dataPointId=?", new Object[]{xid, dpId}, Integer.class, -1);
    }

    /**
     * Return only the source id from the table for a given row
     * @param pedid
     * @return
     */
    public int getSourceId(int id, String sourceType) {
        String sourceIdColumn = getSourceIdColumnName(sourceType);
        return queryForObject("SELECT " + sourceIdColumn + " from " + this.tableName + "AS " + this.tablePrefix + " WHERE id=?", new Object[]{id}, Integer.class, -1);
    }
    

    
    /**
     * Get the column name for the source id using the source type
     * @param sourceType
     * @return
     */
    public String getSourceIdColumnName(String sourceType){
        String columnName = this.sourceTypeToColumnNameMap.get(sourceType);
        if(columnName == null)
            throw new ShouldNeverHappenException("Unknown Detector Source Type: " + sourceType);
        else
            return columnName;
    }
    
    /**
     * Get the index of the source id column in the result set returned from a Select
     * @param sourceType
     * @return
     */
    public int getSourceIdIndex(String sourceType){
        int index = 0;
        Iterator<String> it = this.sourceTypeToColumnNameMap.keySet().iterator();
        while(it.hasNext()){
            if(it.next().equals(sourceType))
                break;
            index++;
        }
        
        return index;
    }
    
    class EventDetectorWithDataPointResultSetExtractor implements ResultSetExtractor<List<AbstractPointEventDetectorVO<?>>> {
        private static final int EVENT_DETECTOR_FIRST_COLUMN = 27;
        static final String POINT_EVENT_DETECTOR_WITH_DATA_POINT_SELECT = "select dp.data, dp.id, dp.xid, dp.dataSourceId, dp.name, dp.deviceName, dp.enabled, dp.pointFolderId, " //
                + "  dp.loggingType, dp.intervalLoggingPeriodType, dp.intervalLoggingPeriod, dp.intervalLoggingType, " //
                + "  dp.tolerance, dp.purgeOverride, dp.purgeType, dp.purgePeriod, dp.defaultCacheSize, " //
                + "  dp.discardExtremeValues, dp.engineeringUnits, dp.readPermission, dp.setPermission, dp.templateId, dp.rollup, ds.name, " //
                + "  ds.xid, ds.dataSourceType, ped.id, ped.xid, ped.sourceTypeName, ped.typeName, ped.data, ped.dataPointId " //
                + "  from eventDetectors ped " //
                + "  left outer join dataPoints dp on dp.id = ped.dataPointId"
                + "  join dataSources ds on ds.id = dp.dataSourceId ";

        @Override
        public List<AbstractPointEventDetectorVO<?>> extractData(ResultSet rs) throws SQLException, DataAccessException {
            List<AbstractPointEventDetectorVO<?>> results = new ArrayList<>();
            DataPointRowMapper pointRowMapper = new DataPointRowMapper();
            while(rs.next()) {
                DataPointVO dpvo = pointRowMapper.mapRow(rs, rs.getRow());
                PointEventDetectorRowMapper mapper = new PointEventDetectorRowMapper(EVENT_DETECTOR_FIRST_COLUMN, 5, dpvo);
                AbstractPointEventDetectorVO<?> ped = mapper.mapRow(rs, rs.getRow());
                results.add(ped);
            }
            return results;
        }
    }
    
    /**
     * Get all event detectors for a given source type i.e. DATA_POINT with the source loaded
     * @param sourceType
     * @return
     */
    public <X extends AbstractEventDetectorVO<?>> List<X> getForSourceType(String sourceType){
        switch(sourceType) {
            case EventType.EventTypeNames.DATA_POINT:
                query(EventDetectorWithDataPointResultSetExtractor.POINT_EVENT_DETECTOR_WITH_DATA_POINT_SELECT, new Object[] { },
                        new EventDetectorWithDataPointResultSetExtractor());
            default:
                return query(SELECT_ALL + " WHERE sourceTypeName=?", new Object[]{sourceType}, new EventDetectorRowMapper<X>());
        }
    }
}
