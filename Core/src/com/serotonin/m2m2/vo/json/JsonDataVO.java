/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * @author Terry Packer
 *
 */
public class JsonDataVO extends AbstractVO {

    public static final String XID_PREFIX = "JSON_";

    private static final long serialVersionUID = 1L;

    private JsonNode jsonData;

    @JsonProperty
    private MangoPermission editPermission = new MangoPermission();
    @JsonProperty
    private MangoPermission readPermission = new MangoPermission();

    public JsonNode getJsonData() {
        return jsonData;
    }

    public void setJsonData(JsonNode data) {
        this.jsonData = data;
    }

    public MangoPermission getEditPermission() {
        return editPermission;
    }

    public void setEditPermission(MangoPermission editPermission) {
        this.editPermission = editPermission;
    }

    public MangoPermission getReadPermission() {
        return readPermission;
    }

    public void setReadPermission(MangoPermission readPermission) {
        this.readPermission = readPermission;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("jsonData", JsonDataDao.getInstance().writeValueAsString(jsonData));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        String json = jsonObject.getString("jsonData");
        try {
            jsonData = JsonDataDao.getInstance().readValueFromString(json);
        } catch (Exception e) {
            throw new TranslatableJsonException("emport.error.parseError", "jsonData");
        }
    }

    @Override
    public String getTypeKey() {
        return "event.audit.jsonData";
    }

}
