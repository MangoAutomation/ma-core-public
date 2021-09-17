/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.JsonDataCreatePermissionDefinition;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Service
public class JsonDataService extends AbstractVOService<JsonDataVO, JsonDataDao> {

    private final JsonDataCreatePermissionDefinition createPermission;
    private final ObjectMapper mapper;

    @Autowired
    public JsonDataService(JsonDataDao dao,
                           ServiceDependencies dependencies,
                           @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") JsonDataCreatePermissionDefinition createPermission,
                           @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper mapper) {
        super(dao, dependencies);
        this.createPermission = createPermission;
        this.mapper = mapper;
    }

    @Override
    protected PermissionDefinition getCreatePermission() {
        return createPermission;
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, JsonDataVO vo) {
        return permissionService.hasPermission(user, vo.getEditPermission());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, JsonDataVO vo) {
        return permissionService.hasPermission(user, vo.getReadPermission());
    }

    /**
     * Gets JSON data for the public REST end point, does not expose if an item exists but is not accessible.
     *
     * @param xid
     * @return
     * @throws NotFoundException if it doesn't exist or if it exists and is not public
     */
    public JsonDataVO getPublicData(String xid) throws NotFoundException {
        try {
            return get(xid);
        } catch (PermissionException e) {
            throw new NotFoundException();
        }
    }

    @Override
    public ProcessResult validate(JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);

        permissionService.validatePermission(result, "readPermission", user, vo.getReadPermission());
        permissionService.validatePermission(result, "editPermission", user, vo.getEditPermission());

        return result;
    }

    @Override
    public ProcessResult validate(JsonDataVO existing, JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        //Additional checks for existing list
        permissionService.validatePermission(result, "readPermission", user, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validatePermission(result, "editPermission", user, existing.getEditPermission(), vo.getEditPermission());

        return result;
    }

    private ProcessResult commonValidation(JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        try{
            dao.writeValueAsString(vo.getJsonData());
        }catch(Exception e){
            result.addMessage("jsonData", new TranslatableMessage("common.default", e.getMessage()));
        }
        return result;
    }

    /**
     * Update a store without touching its data.
     *
     * @param xid
     * @param item
     */
    public JsonDataVO updateStore(String xid, JsonDataVO item) {
        return dao.withLockedRow(xid, (txStatus) -> {
            JsonDataVO existing = this.get(xid);
            item.setJsonData(existing.getJsonData());
            return update(existing, item);
        });
    }

    private static final Pattern UNESCAPE_JSON_PTR_SLASH = Pattern.compile("~1");
    private static final Pattern UNESCAPE_JSON_PTR_TILDE = Pattern.compile("~0");

    /**
     * Get data inside an item using a JSON pointer
     *
     * @param xid
     * @param pointer RFC 6901 JSON pointer
     * @return
     */
    public JsonNode getDataAtPointer(String xid, String pointer) {
        JsonNode data = this.get(xid).getJsonData();
        return getUsingPointer(data, pointer);
    }

    public void setDataAtPointer(String xid, String pointer, String data) throws JsonProcessingException {
        JsonNode node = mapper.readTree(data);
        setDataAtPointer(xid, pointer, node);
    }

    /**
     * Set data inside an item using a JSON pointer
     *
     * @param xid
     * @param pointer RFC 6901 JSON pointer
     * @param data
     */
    public void setDataAtPointer(String xid, String pointer, JsonNode data) {
        this.dao.withLockedRow(xid, (txStatus) -> {
            JsonDataVO item = this.get(xid);

            if (pointer == null || pointer.isEmpty()) {
                item.setJsonData(data);
                this.update(xid, item);
                return;
            }

            JsonPointer ptr = JsonPointer.compile(pointer);
            JsonNode parent = getUsingPointer(item.getJsonData(), ptr.head());
            String property = getLastPropertyName(ptr);

            if (parent instanceof ObjectNode) {
                ((ObjectNode) parent).set(property, data);
            } else if (parent instanceof ArrayNode) {
                ((ArrayNode) parent).set(Integer.parseInt(property), data);
            } else {
                throw new RuntimeException("Cant set property of " + parent.getClass().getSimpleName());
            }

            this.update(xid, item);
        });
    }

    /**
     * Delete data inside an item using a JSON pointer
     *
     * @param xid
     * @param pointer RFC 6901 JSON pointer
     */
    public JsonNode deleteDataAtPointer(String xid, String pointer) {
        return this.dao.withLockedRow(xid, (txStatus) -> {
            JsonDataVO item = this.get(xid);

            JsonNode removed = null;

            if (pointer == null || pointer.isEmpty()) {
                removed = item.getJsonData();
                item.setJsonData(null);
            } else {
                JsonPointer ptr = JsonPointer.compile(pointer);
                JsonNode parent = getUsingPointer(item.getJsonData(), ptr.head());
                String property = getLastPropertyName(ptr);

                if (parent instanceof ObjectNode) {
                    removed = ((ObjectNode) parent).remove(property);
                } else if (parent instanceof ArrayNode) {
                    removed = ((ArrayNode) parent).remove(Integer.parseInt(property));
                } else {
                    throw new UnsupportedOperationException("Cant delete property of " + parent.getClass().getSimpleName());
                }
            }

            this.update(xid, item);
            return removed;
        });
    }

    /**
     * Retrieves a list of values inside an item using a JSON pointer.
     * Target must be an object or array
     *
     * @param xid
     * @param pointer RFC 6901 JSON pointer
     */
    public ArrayNode valuesForDataAtPointer(String xid, String pointer) {
        JsonNode data = this.getDataAtPointer(xid, pointer);

        if (data instanceof ArrayNode) {
            return (ArrayNode) data;
        } else if (data instanceof ObjectNode) {
            ArrayNode array = ((ObjectNode) data).arrayNode(data.size());
            data.elements().forEachRemaining(array::add);
            return array;
        } else {
            throw new UnsupportedOperationException("Can't list values for " + data.getClass().getSimpleName());
        }
    }

    private String getLastPropertyName(JsonPointer ptr) {
        String property = ptr.last().toString().substring(1);
        property = UNESCAPE_JSON_PTR_SLASH.matcher(property).replaceAll("/");
        property = UNESCAPE_JSON_PTR_TILDE.matcher(property).replaceAll("~");
        return property;
    }

    private JsonNode getUsingPointer(JsonNode data, String pointer) {
        return getUsingPointer(data, pointer == null ? null : JsonPointer.compile(pointer));
    }

    private JsonNode getUsingPointer(JsonNode data, JsonPointer pointer) {
        if (data == null || data instanceof MissingNode) {
            throw new NotFoundException();
        }

        JsonNode dataAtPointer = pointer == null ? data : data.at(pointer);

        if (dataAtPointer instanceof MissingNode) {
            throw new NotFoundException();
        }

        return dataAtPointer;
    }
}
