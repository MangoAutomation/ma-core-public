/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.DataPointTableDefinition;
import com.infiniteautomation.mango.spring.db.DataSourceTableDefinition;
import com.infiniteautomation.mango.spring.db.EventDetectorTableDefinition;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.m2m2.Common;
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
public class EventDetectorDao extends AbstractDao<AbstractEventDetectorVO, EventDetectorTableDefinition> {

    private static final LazyInitSupplier<EventDetectorDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(EventDetectorDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (EventDetectorDao)o;
    });

    /* Map of Source Type to Source ID Column Names */
    private LinkedHashMap<String, Field<Integer>> sourceTypeToColumnNameMap;
    private final DataPointTableDefinition dataPointTable;
    private final DataSourceTableDefinition dataSourceTable;

    @Autowired
    private EventDetectorDao(EventDetectorTableDefinition table,
            DataPointTableDefinition dataPointTable,
            DataSourceTableDefinition dataSourceTable,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher){
        super(AuditEventType.TYPE_EVENT_DETECTOR,
                table,
                new TranslatableMessage("internal.monitor.EVENT_DETECTOR_COUNT"),
                mapper, publisher);
        this.dataPointTable = dataPointTable;
        this.dataSourceTable = dataSourceTable;

        //Build our ordered column set from the Module Registry
        List<EventDetectorDefinition<?>> defs = ModuleRegistry.getEventDetectorDefinitions();
        this.sourceTypeToColumnNameMap = new LinkedHashMap<>(defs.size());
        for(EventDetectorDefinition<?> def : defs) {
            this.sourceTypeToColumnNameMap.put(def.getSourceTypeName(), this.table.getField(def.getSourceIdColumnName()));
        }
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static EventDetectorDao getInstance() {
        return springInstance.get();
    }

    @Override
    protected Object[] voToObjectArray(AbstractEventDetectorVO vo) {
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

    @Override
    public RowMapper<AbstractEventDetectorVO> getRowMapper() {
        return new EventDetectorRowMapper<AbstractEventDetectorVO>();
    }

    public JsonObject readValueFromString(String json) throws JsonException, IOException {
        JsonTypeReader reader = new JsonTypeReader(json);

        return (JsonObject)reader.read();
    }

    public static String writeValueAsString(AbstractEventDetectorVO value) throws JsonException, IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.writeObject(value);
        return stringWriter.toString();

    }

    @Override
    protected String getXidPrefix() {
        return AbstractEventDetectorVO.XID_PREFIX;
    }

    @Override
    public void saveRelationalData(AbstractEventDetectorVO vo, boolean insert) {
        EventTypeVO et = vo.getEventType();
        if(vo.getAddedEventHandlers() != null) {
            for(AbstractEventHandlerVO ehVo : vo.getAddedEventHandlers())
                EventHandlerDao.getInstance().addEventHandlerMappingIfMissing(ehVo.getId(), et.getEventType());
        }else if(vo.getEventHandlerXids() != null) {
            //Remove all mappings
            EventHandlerDao.getInstance().deleteEventHandlerMappings(et.getEventType());
            for(String xid : vo.getEventHandlerXids()) {
                EventHandlerDao.getInstance().saveEventHandlerMapping(xid, et.getEventType());
            }
        }
        //Replace the role mappings
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getReadRoles(), vo.getId(), AbstractEventDetectorVO.class.getSimpleName(), PermissionService.READ, insert);
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getEditRoles(), vo.getId(), AbstractEventDetectorVO.class.getSimpleName(), PermissionService.EDIT, insert);

    }

    @Override
    public void loadRelationalData(AbstractEventDetectorVO vo) {
        vo.setEventHandlerXids(EventHandlerDao.getInstance().getEventHandlerXids(vo.getEventType().getEventType()));
        vo.setReadRoles(RoleDao.getInstance().getRoles(vo.getId(), AbstractEventDetectorVO.class.getSimpleName(), PermissionService.READ));
        vo.setEditRoles(RoleDao.getInstance().getRoles(vo.getId(), AbstractEventDetectorVO.class.getSimpleName(), PermissionService.EDIT));
    }

    @Override
    public void deleteRelationalData(AbstractEventDetectorVO vo) {
        //Also update the Event Handlers
        ejt.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1=? and eventTypeRef2=?",
                new Object[] { vo.getEventType().getEventType().getEventType(), vo.getSourceId(), vo.getId() });
        RoleDao.getInstance().deleteRolesForVoPermission(vo.getId(), AbstractEventDetectorVO.class.getSimpleName(), PermissionService.READ);
        RoleDao.getInstance().deleteRolesForVoPermission(vo.getId(), AbstractEventDetectorVO.class.getSimpleName(), PermissionService.EDIT);
    }

    /**
     * Get all data point event detectors with the corresponding sourceId AND the point loaded into it
     * Ordered by detector id.
     *
     * @param sourceId
     * @return
     */
    public List<AbstractPointEventDetectorVO> getWithSource(int sourceId, DataPointVO dp){
        Field<Integer> sourceIdColumnName = getSourceIdColumnName(EventType.EventTypeNames.DATA_POINT);
        Select<Record> query = getJoinedSelectQuery().where(sourceIdColumnName.eq(sourceId)).orderBy(this.table.getIdAlias());
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        return query(sql, args.toArray(new Object[args.size()]), new PointEventDetectorRowMapper(dp));
    }

    /**
     * Get the id for a given row
     * @param xid
     * @return
     */
    public int getId(String xid, int dpId) {
        return queryForObject("SELECT id from " + this.table.getTable().getName() + " WHERE xid=? AND dataPointId=?", new Object[]{xid, dpId}, Integer.class, -1);
    }

    /**
     * Get the column name for the source id using the source type
     * @param sourceType
     * @return
     */
    public Field<Integer> getSourceIdColumnName(String sourceType){
        Field<Integer> columnName = this.sourceTypeToColumnNameMap.get(sourceType);
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

    class EventDetectorWithDataPointResultSetExtractor implements ResultSetExtractor<List<AbstractPointEventDetectorVO>> {

        @Override
        public List<AbstractPointEventDetectorVO> extractData(ResultSet rs) throws SQLException, DataAccessException {
            List<AbstractPointEventDetectorVO> results = new ArrayList<>();
            RowMapper<DataPointVO> pointRowMapper = DataPointDao.getInstance().getRowMapper();
            while(rs.next()) {
                DataPointVO dpvo = pointRowMapper.mapRow(rs, rs.getRow());
                PointEventDetectorRowMapper mapper = new PointEventDetectorRowMapper(dataPointTable.getSelectFields().size() + 3 + 1, 5, dpvo);
                AbstractPointEventDetectorVO ped = mapper.mapRow(rs, rs.getRow());
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
    public <X extends AbstractEventDetectorVO> List<X> getForSourceType(String sourceType){
        switch(sourceType) {
            case EventType.EventTypeNames.DATA_POINT:
                List<Field<?>> fields = new ArrayList<>(this.dataPointTable.getSelectFields());
                //Add data source fields
                fields.add(dataSourceTable.getAlias("name"));
                fields.add(dataSourceTable.getAlias("xid"));
                fields.add(dataSourceTable.getAlias("dataSourceType"));

                fields.addAll(getSelectFields());

                Select<Record> select = this.joinTables(this.getSelectQuery(fields), null)
                        .leftOuterJoin(this.dataPointTable.getTableAsAlias())
                        .on(this.dataPointTable.getIdAlias().eq(this.table.getField("dataPointId")))
                        .join(dataSourceTable.getTableAsAlias())
                        .on(DSL.field(dataSourceTable.getAlias("id"))
                                .eq(this.table.getAlias("dataSourceId")))
                        .where(this.table.getAlias("sourceTypeName").eq(sourceType));
                customizedQuery(select, new EventDetectorWithDataPointResultSetExtractor());
            default:
                return query(getJoinedSelectQuery().getSQL() + " WHERE sourceTypeName=?", new Object[]{sourceType}, new EventDetectorRowMapper<X>());
        }
    }
}
