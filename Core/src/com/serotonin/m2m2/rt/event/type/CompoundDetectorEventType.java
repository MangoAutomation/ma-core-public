/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.CompoundEventDetectorDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.event.CompoundEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;

/**
 * @author Matthew Lohbihler
 */
@JsonEntity
public class CompoundDetectorEventType extends EventType {
    private int compoundDetectorId;
    private int duplicateHandling = EventType.DuplicateHandling.IGNORE;

    public CompoundDetectorEventType() {
        // Required for reflection.
    }

    public CompoundDetectorEventType(int compoundDetectorId) {
        this.compoundDetectorId = compoundDetectorId;
    }

    public int getCompoundDetectorId() {
        return compoundDetectorId;
    }

    @Override
    public String toString() {
        return "CompoundDetectorEventType(compoundDetectorId=" + compoundDetectorId + ")";
    }

    @Override
    public int getDuplicateHandling() {
        return duplicateHandling;
    }

    public void setDuplicateHandling(int duplicateHandling) {
        this.duplicateHandling = duplicateHandling;
    }

    @Override
    public int getReferenceId1() {
        return compoundDetectorId;
    }

    @Override
    public int getReferenceId2() {
        return 0;
    }

    //TODO Implement
    //    @Override
    //    public int getCompoundEventDetectorId() {
    //        return compoundDetectorId;
    //    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + compoundDetectorId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CompoundDetectorEventType other = (CompoundDetectorEventType) obj;
        if (compoundDetectorId != other.compoundDetectorId)
            return false;
        return true;
    }

    //
    //
    // Serialization
    //
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("XID", new CompoundEventDetectorDao().getCompoundEventDetector(compoundDetectorId).getXid());
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        compoundDetectorId = getCompoundEventDetectorId(jsonObject, "XID");
    }


    protected int getCompoundEventDetectorId(JsonObject json, String name) throws JsonException {
        String xid = json.getString(name);
        if (xid == null)
            throw new TranslatableJsonException("emport.error.eventType.missing.reference", name);
        CompoundEventDetectorVO ced = new CompoundEventDetectorDao().getCompoundEventDetector(xid);
        if (ced == null)
            throw new TranslatableJsonException("emport.error.eventType.invalid.reference", name, xid);
        return ced.getId();
    }
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getEventType()
     */
    @Override
    public String getEventType() {
        return EventType.EventTypeNames.COMPOUND;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#getEventSubtype()
     */
    @Override
    public String getEventSubtype() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#asModel()
     */
    @Override
    public EventTypeModel asModel() {
        throw new ShouldNeverHappenException("Un-implemented");
    }

    @Override
    public boolean hasPermission(PermissionHolder user) {
        throw new ShouldNeverHappenException("Un-implemented");
    }
}
