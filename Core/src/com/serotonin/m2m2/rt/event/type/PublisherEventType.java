/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;

import com.infiniteautomation.mango.spring.dao.PublisherDao;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.PublisherEventTypeModel;

/**
 * @author Matthew Lohbihler
 */
public class PublisherEventType extends EventType {
    private int publisherId;
    private int publisherEventTypeId;

    public PublisherEventType() {
        // Required for reflection.
    }

    public PublisherEventType(int publisherId, int publisherEventTypeId) {
        this.publisherId = publisherId;
        this.publisherEventTypeId = publisherEventTypeId;
    }

    @Override
    public String getEventType() {
        return EventType.EventTypeNames.PUBLISHER;
    }

    @Override
    public String getEventSubtype() {
        return null;
    }

    @Override
    public boolean isRateLimited() {
        return true;
    }

    public int getPublisherEventTypeId() {
        return publisherEventTypeId;
    }

    @Override
    public int getPublisherId() {
        return publisherId;
    }

    @Override
    public String toString() {
        return "PublisherEventType(publisherId=" + publisherId + ", eventTypeId=" + publisherEventTypeId + ")";
    }

    @Override
    public int getDuplicateHandling() {
        return EventType.DuplicateHandling.IGNORE;
    }

    @Override
    public int getReferenceId1() {
        return publisherId;
    }

    @Override
    public int getReferenceId2() {
        return publisherEventTypeId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + publisherEventTypeId;
        result = prime * result + publisherId;
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
        PublisherEventType other = (PublisherEventType) obj;
        if (publisherEventTypeId != other.publisherEventTypeId)
            return false;
        if (publisherId != other.publisherId)
            return false;
        return true;
    }

    //
    //
    // Serialization
    //
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        PublisherVO<?> pb = getPublisher(jsonObject, "XID");
        publisherId = pb.getId();
        publisherEventTypeId = getInt(jsonObject, "publisherEventTypeId", pb.getEventCodes());
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        PublisherVO<?> pub = PublisherDao.instance.getPublisher(publisherId);
        writer.writeEntry("XID", pub.getXid());
        writer.writeEntry("publisherEventTypeId", pub.getEventCodes().getCode(publisherEventTypeId));
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#asModel()
     */
    @Override
    public EventTypeModel asModel() {
        return new PublisherEventTypeModel(this);
    }

    @Override
    public boolean hasPermission(PermissionHolder user) {
        return Permissions.hasAdminPermission(user);
    }
}
