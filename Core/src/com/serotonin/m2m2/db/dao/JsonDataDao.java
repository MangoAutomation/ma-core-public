/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
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
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.JsonDataTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.tables.MintermMappingTable;
import com.serotonin.m2m2.db.dao.tables.PermissionMappingTable;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class JsonDataDao extends AbstractVoDao<JsonDataVO, JsonDataTableDefinition> {

    private static final LazyInitSupplier<JsonDataDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(JsonDataDao.class);
    });

    private final PermissionService permissionService;

    /**
     * @param handler
     * @param typeName
     */
    @Autowired
    private JsonDataDao(JsonDataTableDefinition table, @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper mapper,
            ApplicationEventPublisher publisher,
            PermissionService permissionService) {
        super(AuditEventType.TYPE_JSON_DATA, table, new TranslatableMessage("internal.monitor.JSON_DATA_COUNT"), mapper, publisher);
        this.permissionService = permissionService;
    }

    /**
     * Get cached instance from Spring Context
     *
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
        try {
            JsonNode data = vo.getJsonData();
            if (data != null) {
                jsonData = writeValueAsString(data);
            }
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }

        return new Object[] {vo.getXid(), vo.getName(), jsonData, vo.getReadPermission().getId(), vo.getEditPermission().getId()};
    }

    @Override
    public RowMapper<JsonDataVO> getRowMapper() {
        return new JsonDataRowMapper();
    }

    class JsonDataRowMapper implements RowMapper<JsonDataVO> {

        @Override
        public JsonDataVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;
            JsonDataVO vo = new JsonDataVO();
            vo.setId(rs.getInt(++i));
            vo.setXid(rs.getString(++i));
            vo.setName(rs.getString(++i));

            // Read the data
            try {
                Clob clob = rs.getClob(++i);
                if (clob != null) {
                    vo.setJsonData(getObjectReader(JsonNode.class).readTree(clob.getCharacterStream()));
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            vo.setReadPermission(new MangoPermission(rs.getInt(++i)));
            vo.setEditPermission(new MangoPermission(rs.getInt(++i)));

            return vo;
        }
    }

    @Override
    public void savePreRelationalData(JsonDataVO existing, JsonDataVO vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission().getRoles());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission().getRoles());
        vo.setEditPermission(editPermission);
    }

    @Override
    public void saveRelationalData(JsonDataVO existing, JsonDataVO vo) {
        if(existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.permissionDeleted(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(vo.getEditPermission())) {
                permissionService.permissionDeleted(existing.getEditPermission());
            }
        }
    }

    @Override
    public void loadRelationalData(JsonDataVO vo) {
        // Populate permissions
        vo.setReadPermission(permissionService.get(vo.getReadPermission().getId()));
        vo.setEditPermission(permissionService.get(vo.getEditPermission().getId()));
    }

    @Override
    public void deletePostRelationalData(JsonDataVO vo) {
        // Clean permissions
        permissionService.permissionDeleted(vo.getReadPermission(), vo.getEditPermission());
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions, PermissionHolder user) {
        if (!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = permissionService.getAllInheritedRoles(user).stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);

            Table<?> mintermsGranted = this.create.select(MintermMappingTable.MINTERMS_MAPPING.mintermId).from(MintermMappingTable.MINTERMS_MAPPING)
                    .groupBy(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .having(DSL.count().eq(DSL.count(DSL.case_().when(roleIdsIn, DSL.inline(1)).else_(DSL.inline((Integer) null))))).asTable("mintermsGranted");

            Table<?> permissionsGranted = this.create.selectDistinct(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId)
                    .from(PermissionMappingTable.PERMISSIONS_MAPPING).join(mintermsGranted)
                    .on(mintermsGranted.field(MintermMappingTable.MINTERMS_MAPPING.mintermId).eq(PermissionMappingTable.PERMISSIONS_MAPPING.mintermId))
                    .asTable("permissionsGranted");

            select = select.join(permissionsGranted).on(permissionsGranted.field(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId)
                    .in(JsonDataTableDefinition.READ_PERMISSION_ALIAS));

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
    public JsonNode readValueFromString(String json) throws JsonParseException, JsonMappingException, IOException {
        return getObjectReader(JsonNode.class).readTree(json);
    }

    public String writeValueAsString(JsonNode value) throws JsonProcessingException {
        return getObjectWriter(JsonNode.class).writeValueAsString(value);
    }

    public JsonNodeFactory getNodeFactory() {
        return this.mapper.getNodeFactory();
    }
}
