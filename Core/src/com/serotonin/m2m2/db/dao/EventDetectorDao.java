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
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Field;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.DataPointTableDefinition;
import com.infiniteautomation.mango.spring.db.DataSourceTableDefinition;
import com.infiniteautomation.mango.spring.db.EventDetectorTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.tables.MintermMappingTable;
import com.serotonin.m2m2.db.dao.tables.PermissionMappingTable;
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
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class EventDetectorDao extends AbstractVoDao<AbstractEventDetectorVO, EventDetectorTableDefinition> {

    private static final LazyInitSupplier<EventDetectorDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(EventDetectorDao.class);
    });

    /* Map of Source Type to Source ID Column Names */
    private LinkedHashMap<String, Field<Integer>> sourceTypeToColumnNameMap;
    private final DataPointTableDefinition dataPointTable;
    private final DataSourceTableDefinition dataSourceTable;

    private final PermissionService permissionService;
    private final PermissionDao permissionDao;

    @Autowired
    private EventDetectorDao(EventDetectorTableDefinition table,
            DataPointTableDefinition dataPointTable,
            DataSourceTableDefinition dataSourceTable,
            PermissionService permissionService,
            PermissionDao permissionDao,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher){
        super(AuditEventType.TYPE_EVENT_DETECTOR,
                table,
                new TranslatableMessage("internal.monitor.EVENT_DETECTOR_COUNT"),
                mapper, publisher);
        this.dataPointTable = dataPointTable;
        this.dataSourceTable = dataSourceTable;
        this.permissionService = permissionService;
        this.permissionDao = permissionDao;
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
        String data = null;
        try{
            data = writeValueAsString(vo);
        }catch(JsonException | IOException e){
            LOG.error(e.getMessage(), e);
        }

        //Find the index of our sourceIdColumn
        int sourceIdIndex = getSourceIdIndex(vo.getDefinition().getSourceTypeName());

        Object[] o = new Object[7 + this.sourceTypeToColumnNameMap.size()];
        o[0] = vo.getXid();
        o[1] = vo.getDetectorSourceType();
        o[2] = vo.getDetectorType();
        o[3] = convertData(vo.getData());
        o[4] = data;
        o[5] = vo.getReadPermission().getId();
        o[6] = vo.getEditPermission().getId();
        o[7 + sourceIdIndex] = vo.getSourceId();
        return o;
    }

    @Override
    public RowMapper<AbstractEventDetectorVO> getRowMapper() {
        return new EventDetectorRowMapper<AbstractEventDetectorVO>((c) -> this.extractData(c), this);
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
    public void savePreRelationalData(AbstractEventDetectorVO existing, AbstractEventDetectorVO vo) {
        permissionDao.permissionId(vo.getReadPermission());
        permissionDao.permissionId(vo.getEditPermission());
        vo.getDefinition().savePreRelationalData(existing, vo);
    }

    @Override
    public void saveRelationalData(AbstractEventDetectorVO existing, AbstractEventDetectorVO vo) {
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
        if(existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionDao.permissionDeleted(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(vo.getEditPermission())) {
                permissionDao.permissionDeleted(existing.getEditPermission());
            }
        }
    }

    @Override
    public void loadRelationalData(AbstractEventDetectorVO vo) {
        vo.setEventHandlerXids(EventHandlerDao.getInstance().getEventHandlerXids(vo.getEventType().getEventType()));
        //Populate permissions
        vo.setReadPermission(permissionDao.get(vo.getReadPermission().getId()));
        vo.setEditPermission(permissionDao.get(vo.getEditPermission().getId()));
        vo.getDefinition().loadRelationalData(vo);
    }

    @Override
    public void deleteRelationalData(AbstractEventDetectorVO vo) {
        //Also update the Event Handlers
        ejt.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1=? and eventTypeRef2=?",
                new Object[] { vo.getEventType().getEventType().getEventType(), vo.getSourceId(), vo.getId() });
    }

    @Override
    public void deletePostRelationalData(AbstractEventDetectorVO vo) {
        //Clean permissions
        permissionDao.permissionDeleted(vo.getReadPermission(), vo.getEditPermission());
        vo.getDefinition().deletePostRelationalData(vo);
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select,
            ConditionSortLimit conditions, PermissionHolder user) {
        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = user.getAllInheritedRoles().stream().map(r -> r.getId()).collect(Collectors.toList());

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
                            EventDetectorTableDefinition.READ_PERMISSION_ALIAS, EventDetectorTableDefinition.EDIT_PERMISSION_ALIAS));

        }
        return select;
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
        return query(sql, args.toArray(new Object[args.size()]), new PointEventDetectorRowMapper(dp, (c) -> this.extractData(c), this));
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

        private final int firstEventDetectorColumn;

        public EventDetectorWithDataPointResultSetExtractor(int firstEventDetectorColumn){
            this.firstEventDetectorColumn = firstEventDetectorColumn;
        }


        @Override
        public List<AbstractPointEventDetectorVO> extractData(ResultSet rs) throws SQLException, DataAccessException {
            List<AbstractPointEventDetectorVO> results = new ArrayList<>();
            RowMapper<DataPointVO> pointRowMapper = DataPointDao.getInstance().getRowMapper();
            while(rs.next()) {
                DataPointVO dpvo = pointRowMapper.mapRow(rs, rs.getRow());
                PointEventDetectorRowMapper mapper = new PointEventDetectorRowMapper(firstEventDetectorColumn, (c) -> EventDetectorDao.this.extractData(c), dpvo, EventDetectorDao.this);
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

                int firstEventDetectorColumn = fields.size() + 1; //next field is the start of our columns
                fields.addAll(getSelectFields());

                Select<Record> select = this.joinTables(this.getSelectQuery(fields), null)
                        .leftOuterJoin(this.dataPointTable.getTableAsAlias())
                        .on(this.dataPointTable.getIdAlias().eq(this.table.getField("dataPointId")))
                        .join(dataSourceTable.getTableAsAlias())
                        .on(DSL.field(dataSourceTable.getAlias("id"))
                                .eq(this.table.getAlias("dataSourceId")))
                        .where(this.table.getAlias("sourceTypeName").eq(sourceType));
                customizedQuery(select, new EventDetectorWithDataPointResultSetExtractor(firstEventDetectorColumn));
            default:
                return query(getJoinedSelectQuery().getSQL() + " WHERE sourceTypeName=?", new Object[]{sourceType}, new EventDetectorRowMapper<X>((c) -> this.extractData(c), this));
        }
    }
}
