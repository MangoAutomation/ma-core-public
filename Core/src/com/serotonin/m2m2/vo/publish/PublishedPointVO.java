/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
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
        // yes we write the XID into a field named dataPointId
        writer.writeEntry("dataPointId", dataPointXid);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        String xid = jsonObject.getString("dataPointId");
        if (xid == null)
            throw new TranslatableJsonException("emport.error.publishedPoint.missing", "dataPointId");
        
        Integer id = DataPointDao.getInstance().getIdByXid(xid);
        if (id == null)
            throw new TranslatableJsonException("emport.error.missingPoint", xid);
        dataPointId = id;
    }

}
