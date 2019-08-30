/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.publish;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublishedPointModel;

/**
 * @author Matthew Lohbihler
 */
abstract public class PublishedPointVO extends AbstractActionVO<PublishedPointVO> {
    
    public static final String XID_PREFIX = "PP_";
    
    private int publisherId;
    private int dataPointId;
    
    public int getPublisherId() {
        return publisherId;
    }
    
    public void setPublisherId(int publisherId) {
        this.publisherId = publisherId;
    }
    
    public int getDataPointId() {
        return dataPointId;
    }

    public void setDataPointId(int dataPointId) {
        this.dataPointId = dataPointId;
    }

    @Override
    public void validate(ProcessResult response) {
        super.validate(response);
        
        if(publisherId <= 0) {
            response.addContextualMessage("publisherId", "validate.invalidValue");
            
        }else {
            String xid = PublisherDao.getInstance().getXidById(publisherId);
            if(xid == null) {
                response.addContextualMessage("publisherId", "validate.invalidValue");
                return;
            }
        }
        
        if(dataPointId <= 0) {
            response.addContextualMessage("dataPointId", "validate.invalidValue");
            
        }else {
            String xid = DataPointDao.getInstance().getXidById(dataPointId);
            if(xid == null) {
                response.addContextualMessage("dataPointId", "validate.invalidValue");
                return;
            }
        }
        
    }
    
    //
    //
    // Serialization
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
        String xid = DataPointDao.getInstance().getXidById(dataPointId);
        writer.writeEntry("dataPointId", xid);
        xid = PublisherDao.getInstance().getXidById(publisherId);
        writer.writeEntry("publisherXid", xid);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        String xid = jsonObject.getString("dataPointId");
        if (xid == null)
            throw new TranslatableJsonException("emport.error.publishedPoint.missing", "dataPointId");
        
        Integer id = DataPointDao.getInstance().getIdByXid(xid);
        if (id == null)
            throw new TranslatableJsonException("emport.error.missingPoint", xid);
        dataPointId = id;
        
        xid = jsonObject.getString("publisherXid");
        if (xid == null)
            throw new TranslatableJsonException("emport.error.publishedPoint.missing", "publisherXid");
        
        id = PublisherDao.getInstance().getIdByXid(xid);
        if (id == null)
            throw new TranslatableJsonException("emport.error.missingPublisher", xid);
        publisherId = id;
    }
    
    @Override
    public String getTypeKey() {
        return "event.audit.publishedPoint";
    }
    
    @Override
    protected AbstractDao<PublishedPointVO> getDao() {
        return PublishedPointDao.getInstance();
    }
    
    public abstract AbstractPublishedPointModel<?> asModel();
}
