/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.infiniteautomation.mango.db.tables.JsonData;
import com.infiniteautomation.mango.db.tables.records.JsonDataRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.module.definitions.permissions.JsonDataCreatePermissionDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class JsonDataServiceTest extends AbstractVOServiceWithPermissionsTest<JsonDataVO, JsonDataRecord, JsonData, JsonDataDao, JsonDataService>{

    @Override
    String getCreatePermissionType() {
        return JsonDataCreatePermissionDefinition.TYPE_NAME;
    }

    @Override
    void setReadPermission(MangoPermission permission, JsonDataVO vo) {
        vo.setReadPermission(permission);
    }

    @Override
    void setEditPermission(MangoPermission permission, JsonDataVO vo) {
        vo.setEditPermission(permission);
    }

    @Override
    JsonDataService getService() {
        return Common.getBean(JsonDataService.class);
    }

    @Override
    JsonDataDao getDao() {
        return JsonDataDao.getInstance();
    }

    @Override
    void assertVoEqual(JsonDataVO expected, JsonDataVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());

        //TODO assert json value
    }

    @Override
    JsonDataVO newVO(User owner) {
        JsonDataVO vo = new JsonDataVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        JsonNodeFactory nf = this.getDao().getNodeFactory();
        ObjectNode jsonData = nf.objectNode();
        jsonData.set("test", nf.textNode("value"));
        vo.setJsonData(jsonData);
        return vo;
    }

    @Override
    JsonDataVO updateVO(JsonDataVO existing) {
        JsonDataVO copy = (JsonDataVO) existing.copy();
        JsonNodeFactory nf = this.getDao().getNodeFactory();
        ObjectNode jsonData = nf.objectNode();
        jsonData.set("test", nf.textNode("value-updated"));
        copy.setJsonData(jsonData);
        return copy;
    }

    @Override
    void addReadRoleToFail(Role role, JsonDataVO vo) {
        vo.getReadPermission().getRoles().add(Collections.singleton(role));
    }

    @Override
    String getReadPermissionContextKey() {
        return "readPermission";
    }

    @Override
    void addEditRoleToFail(Role role, JsonDataVO vo) {
        vo.getEditPermission().getRoles().add(Collections.singleton(role));
    }

    @Override
    String getEditPermissionContextKey() {
        return "editPermission";
    }
}
