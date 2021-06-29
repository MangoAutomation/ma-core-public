/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;

import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.infiniteautomation.mango.db.tables.JsonData;
import com.infiniteautomation.mango.db.tables.records.JsonDataRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.json.JsonDataVO;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class JsonDataDao extends AbstractVoDao<JsonDataVO, JsonDataRecord, JsonData> {

    private static final LazyInitSupplier<JsonDataDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(JsonDataDao.class);
    });

    @Autowired
    private JsonDataDao(@Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper mapper,
                        ApplicationEventPublisher publisher,
                        PermissionService permissionService) {
        super(AuditEventType.TYPE_JSON_DATA, JsonData.JSON_DATA, new TranslatableMessage("internal.monitor.JSON_DATA_COUNT"), mapper, publisher, permissionService);
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
    protected Record toRecord(JsonDataVO vo) {
        String jsonData = null;
        try {
            JsonNode data = vo.getJsonData();
            if (data != null) {
                jsonData = writeValueAsString(data);
            }
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }

        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.name, vo.getName());
        record.set(table.data, jsonData);
        record.set(table.readPermissionId, vo.getReadPermission().getId());
        record.set(table.editPermissionId, vo.getEditPermission().getId());
        return record;
    }


    @Override
    public JsonDataVO mapRecord(Record record) {
        JsonDataVO vo = new JsonDataVO();
        vo.setId(record.get(table.id));
        vo.setXid(record.get(table.xid));
        vo.setName(record.get(table.name));
        vo.setJsonData(extractDataFromObject(record.get(table.data)));
        vo.setReadPermission(new MangoPermission(record.get(table.readPermissionId)));
        vo.setEditPermission(new MangoPermission(record.get(table.editPermissionId)));
        return vo;
    }

    @Override
    public void savePreRelationalData(JsonDataVO existing, JsonDataVO vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission());
        vo.setEditPermission(editPermission);
    }

    @Override
    public void saveRelationalData(JsonDataVO existing, JsonDataVO vo) {
        if(existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(vo.getEditPermission())) {
                permissionService.deletePermissions(existing.getEditPermission());
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
        permissionService.deletePermissions(vo.getReadPermission(), vo.getEditPermission());
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
