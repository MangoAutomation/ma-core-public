/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2.vo;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.util.ChangeComparable;
import com.serotonin.validation.StringValidation;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */
public abstract class AbstractVO<T extends AbstractVO<T>> implements Serializable, ChangeComparable<T>, JsonSerializable, Cloneable {
    /*
     * Mango properties
     */
    
    protected int id = Common.NEW_ID;
    protected String xid;
    protected String name;
    
    /* (non-Javadoc)
     * @see com.serotonin.json.spi.JsonSerializable#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject)
     */
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        // dont user JsonProperty annotation so we can choose whether to read/write in sub type
        xid = jsonObject.getString("xid");
        name = jsonObject.getString("name");
    }

    /* (non-Javadoc)
     * @see com.serotonin.json.spi.JsonSerializable#jsonWrite(com.serotonin.json.ObjectWriter)
     */
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        // dont user JsonProperty annotation so we can choose whether to read/write in sub type
        writer.writeEntry("xid", xid);
        writer.writeEntry("name", name);
    }
    
    /*
     * Serialization
     */
    
    private static final long serialVersionUID = -1;
    
    /*
     * ChangeComparable
     */
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.ChangeComparable#addProperties(java.util.List)
     */
    @Override
    public void addProperties(List<TranslatableMessage> list) {
        AuditEventType.addPropertyMessage(list, "common.xid", xid);
        AuditEventType.addPropertyMessage(list, "common.name", name);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.ChangeComparable#addPropertyChanges(java.util.List, java.lang.Object)
     */
    @Override
    public void addPropertyChanges(List<TranslatableMessage> list, T from) {
        AuditEventType.maybeAddPropertyChangeMessage(list, "common.xid", from.xid, xid);
        AuditEventType.maybeAddPropertyChangeMessage(list, "common.name", from.name, name);
    }
    
    /*
     * Getters and setters
     */
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /*
     * Utility methods
     */
    
    /**
     * Validates a vo
     * @param response
     */
    public void validate(ProcessResult response) {
        if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(xid, 50))
            response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 50));
        else if (!new DataPointDao().isXidUnique(xid, id))
            response.addContextualMessage("xid", "validate.xidUsed");
        
        if (StringUtils.isBlank(name))
            response.addContextualMessage("name", "validate.required");
        else if (StringValidation.isLengthGreaterThan(name, 255))
            response.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
    }
    
    /**
     * Check if a vo is newly created
     * @return true if newly created, false otherwise
     */
    public boolean isNew() {
        return (id == Common.NEW_ID);
    }

    /**
     * Copies a vo
     * @return Copy of this vo
     */
    @SuppressWarnings("unchecked")
    public T copy() {
        // TODO make sure this works
        try {
            return (T) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new ShouldNeverHappenException(e);
        }
    }
    
    /**
     * Useful For Debugging
     */
    public String toString(){
        return "id: " + this.id + " name: " + this.name;
    }
}
