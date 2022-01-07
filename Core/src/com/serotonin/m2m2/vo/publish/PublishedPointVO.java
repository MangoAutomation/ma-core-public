/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.util.LazyField;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.AbstractActionVO;

/**
 * @author Matthew Lohbihler
 */
abstract public class PublishedPointVO extends AbstractActionVO {
    public static final String XID_PREFIX = "PP_";

    private int publisherId;
    private int dataPointId;
    private JsonNode jsonData;

    //
    //
    // Convenience data from publisher
    //
    private String publisherTypeName;

    /**
     * These fields are only used for JSON export, and for outputting to the REST model
     */
    private transient String dataPointXid;
    private transient LazyField<Map<String, String>> dataPointTags = new LazyField<>(new HashMap<>());

    @JsonProperty
    private transient String publisherXid;

    public int getDataPointId() {
        return dataPointId;
    }
    public void setDataPointId(int dataPointId) {
        this.dataPointId = dataPointId;
    }

    public int getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(int publisherId) {
        this.publisherId = publisherId;
    }

    public JsonNode getJsonData() {
        return jsonData;
    }

    public void setJsonData(JsonNode jsonData) {
        this.jsonData = jsonData;
    }

    public String getPublisherTypeName() {
        return publisherTypeName;
    }

    public void setPublisherTypeName(String publisherTypeName) {
        this.publisherTypeName = publisherTypeName;
    }

    public String getDataPointXid() {
        return dataPointXid;
    }
    public void setDataPointXid(String dataPointXid) {
        this.dataPointXid = dataPointXid;
    }

    public String getPublisherXid() {
        return publisherXid;
    }

    public void setPublisherXid(String publisherXid) {
        this.publisherXid = publisherXid;
    }

    public Map<String,String> getDataPointTags() {
        return dataPointTags.get();
    }

    public void supplyDataPointTags(Supplier<Map<String,String>> dataPointTags) {
        this.dataPointTags = new LazyField<>(dataPointTags);
    }

    @Override
    public String getTypeKey() {
        return "event.audit.publishedPoint";
    }

    //
    //
    // Legacy Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(dataPointId);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            dataPointId = in.readInt();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("dataPointXid", dataPointXid);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {

        //Not reading XID so can't do this: super.jsonRead(reader, jsonObject);
        if(jsonObject.containsKey("name"))
            name = getString(jsonObject, "name");
        if(jsonObject.containsKey("enabled"))
            enabled = getBoolean(jsonObject, "enabled");

        //In legacy incarnations the dataPointXid was written as dataPointId
        String xid = jsonObject.getString("dataPointXid");
        if(xid == null) {
            //Try legacy field name
            xid = jsonObject.getString("dataPointId");
            if (xid == null) {
                throw new TranslatableJsonException("emport.error.publishedPoint.missing", "dataPointXid");
            }
        }
        
        Integer id = DataPointDao.getInstance().getIdByXid(xid);
        if (id == null)
            throw new TranslatableJsonException("emport.error.missingPoint", xid);
        dataPointId = id;
    }

    @Override
    public PublishedPointVO copy() {
        PublishedPointVO vo = (PublishedPointVO) super.copy();
        vo.setPublisherId(publisherId);
        vo.setDataPointId(dataPointId);
        vo.setJsonData(jsonData);

        vo.setPublisherTypeName(publisherTypeName);
        vo.setDataPointXid(dataPointXid);
        vo.setPublisherXid(publisherXid);
        return vo;
    }
}
