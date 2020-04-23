/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.JsonDataTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition.GrantedAccess;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class JsonDataDao extends AbstractDao<JsonDataVO, JsonDataTableDefinition>{

    private static final LazyInitSupplier<JsonDataDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(JsonDataDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (JsonDataDao)o;
    });

    private final PermissionService permissionService;

    /**
     * @param handler
     * @param typeName
     */
    @Autowired
    private JsonDataDao(JsonDataTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher,
            PermissionService permissionService) {
        super(AuditEventType.TYPE_JSON_DATA,
                table,
                new TranslatableMessage("internal.monitor.JSON_DATA_COUNT"),
                mapper, publisher);
        this.permissionService = permissionService;
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static JsonDataDao getInstance() {
        return springInstance.get();
    }

    @Override
    protected String getXidPrefix() {
        return JsonDataVO.XID_PREFIX;
    }

    @Override
    protected Object[] voToObjectArray(JsonDataVO vo) {
        String jsonData = null;
        try{
            JsonNode data = vo.getJsonData();
            if (data != null) {
                jsonData = writeValueAsString(data);
            }
        }catch(JsonProcessingException e){
            LOG.error(e.getMessage(), e);
        }

        return new Object[]{
                vo.getXid(),
                vo.getName(),
                boolToChar(vo.isPublicData()),
                jsonData
        };
    }

    @Override
    public RowMapper<JsonDataVO> getRowMapper() {
        return new JsonDataRowMapper();
    }

    class JsonDataRowMapper implements RowMapper<JsonDataVO>{

        @Override
        public JsonDataVO mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            int i=0;
            JsonDataVO vo = new JsonDataVO();
            vo.setId(rs.getInt(++i));
            vo.setXid(rs.getString(++i));
            vo.setName(rs.getString(++i));
            vo.setPublicData(charToBool(rs.getString(++i)));

            //Read the data
            try{
                Clob clob = rs.getClob(++i);
                if (clob != null) {
                    vo.setJsonData(getObjectReader(JsonNode.class).readTree(clob.getCharacterStream()));
                }
            }catch(Exception e){
                LOG.error(e.getMessage(), e);
            }

            return vo;
        }
    }

    @Override
    public void loadRelationalData(JsonDataVO vo) {
        //Populate permissions
        vo.setReadPermission(RoleDao.getInstance().getPermission(vo, PermissionService.READ));
        vo.setEditPermission(RoleDao.getInstance().getPermission(vo, PermissionService.EDIT));

    }

    @Override
    public void saveRelationalData(JsonDataVO vo, boolean insert) {
        //Replace the role mappings
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getReadPermission(), vo, PermissionService.READ, insert);
        RoleDao.getInstance().replaceRolesOnVoPermission(vo.getEditPermission(), vo, PermissionService.EDIT, insert);
    }

    @Override
    public void deleteRelationalData(JsonDataVO vo) {
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.READ);
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.EDIT);
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions,
            PermissionHolder user) {
        //Join on permissions
        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = user.getAllInheritedRoles().stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);
            Field<Boolean> granted = new GrantedAccess(RoleTableDefinition.maskField, roleIdsIn);

            Table<?> readSubselect = this.create.select(
                    RoleTableDefinition.voIdField,
                    DSL.inline(1).as("granted"))
                    .from(RoleTableDefinition.ROLE_MAPPING_TABLE)
                    .where(RoleTableDefinition.voTypeField.eq(JsonDataVO.class.getSimpleName()),
                            RoleTableDefinition.permissionTypeField.eq(PermissionService.READ))
                    .groupBy(RoleTableDefinition.voIdField)
                    .having(granted)
                    .asTable("jsonDataRead");

            select = select.leftJoin(readSubselect).on(this.table.getIdAlias().eq(readSubselect.field(RoleTableDefinition.voIdField)));

            Table<?> editSubselect = this.create.select(
                    RoleTableDefinition.voIdField,
                    DSL.inline(1).as("granted"))
                    .from(RoleTableDefinition.ROLE_MAPPING_TABLE)
                    .where(RoleTableDefinition.voTypeField.eq(JsonDataVO.class.getSimpleName()),
                            RoleTableDefinition.permissionTypeField.eq(PermissionService.EDIT))
                    .groupBy(RoleTableDefinition.voIdField)
                    .having(granted)
                    .asTable("jsonDataEdit");

            select = select.leftJoin(editSubselect).on(this.table.getIdAlias().eq(editSubselect.field(RoleTableDefinition.voIdField)));

            conditions.addCondition(DSL.or(
                    readSubselect.field("granted").isTrue(),
                    editSubselect.field("granted").isTrue()));
        }
        return select;
    }

    /**
     *
     * @param json
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public JsonNode readValueFromString(String json) throws JsonParseException, JsonMappingException, IOException{
        return getObjectReader(JsonNode.class).readTree(json);
    }

    public String writeValueAsString(JsonNode value) throws JsonProcessingException{
        return getObjectWriter(JsonNode.class).writeValueAsString(value);
    }

    public JsonNodeFactory getNodeFactory() {
        return this.mapper.getNodeFactory();
    }
}
