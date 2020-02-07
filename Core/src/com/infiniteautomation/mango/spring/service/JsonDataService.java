/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.infiniteautomation.mango.spring.db.JsonDataTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.JsonDataCreatePermissionDefinition;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
@Service
public class JsonDataService extends AbstractVOService<JsonDataVO, JsonDataTableDefinition, JsonDataDao> {

    public JsonDataService(JsonDataDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public Set<Role> getCreatePermissionRoles() {
        return ModuleRegistry.getPermissionDefinition(JsonDataCreatePermissionDefinition.TYPE_NAME).getRoles();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, JsonDataVO vo) {
        return permissionService.hasAnyRole(user, vo.getEditRoles());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, JsonDataVO vo) {
        return permissionService.hasAnyRole(user, vo.getReadRoles());
    }

    /**
     * Get data and ensure it is public,
     * @param xid
     * @return
     * @throws NotFoundException if it doesn't exist or if it exists and is not public
     */
    public JsonDataVO getPublicData(String xid) throws NotFoundException {
        JsonDataVO vo = this.getPermissionService().runAsSystemAdmin(() -> {
            return super.get(xid);
        });
        if(!vo.isPublicData()) {
            throw new NotFoundException();
        }else {
            return vo;
        }
    }

    @Override
    public ProcessResult validate(JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);

        permissionService.validateVoRoles(result, "readRoles", user, false, null, vo.getReadRoles());
        permissionService.validateVoRoles(result, "editRoles", user, false, null, vo.getEditRoles());

        return result;
    }

    @Override
    public ProcessResult validate(JsonDataVO existing, JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        //Additional checks for existing list
        permissionService.validateVoRoles(result, "readRoles", user, false, existing.getReadRoles(), vo.getReadRoles());
        permissionService.validateVoRoles(result, "editRoles", user, false, existing.getEditRoles(), vo.getEditRoles());

        return result;
    }

    private ProcessResult commonValidation(JsonDataVO vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        try{
            JsonDataDao.getInstance().writeValueAsString(vo.getJsonData());
        }catch(Exception e){
            result.addMessage("jsonData", new TranslatableMessage("common.default", e.getMessage()));
        }
        return result;
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

    /**
     * Set data inside an item using a JSON pointer
     *
     * @param xid
     * @param pointer RFC 6901 JSON pointer
     * @param data
     */
    public void setDataAtPointer(String xid, String pointer, JsonNode data) {
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
    }

    /**
     * Delete data inside an item using a JSON pointer
     *
     * @param xid
     * @param pointer RFC 6901 JSON pointer
     * @param data
     */
    public boolean deleteDataAtPointer(String xid, String pointer) {
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
        return removed != null;
    }

    /**
     * Retrieves a list of values inside an item using a JSON pointer.
     * Target must be an object or array
     *
     * @param xid
     * @param pointer RFC 6901 JSON pointer
     */
    public List<JsonNode> valuesForDataAtPointer(String xid, String pointer) {
        JsonNode data = this.getDataAtPointer(xid, pointer);

        if (!(data instanceof ObjectNode || data instanceof ArrayNode)) {
            throw new UnsupportedOperationException("Can't list values for " + data.getClass().getSimpleName());
        }

        List<JsonNode> list = new ArrayList<>(data.size());
        data.elements().forEachRemaining(list::add);

        return list;
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
