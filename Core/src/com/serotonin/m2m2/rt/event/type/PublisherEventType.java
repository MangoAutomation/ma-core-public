/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;
import java.util.Map;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * @author Matthew Lohbihler
 */
public class PublisherEventType extends EventType {
    private int publisherId;
    private int publisherEventTypeId;

    public PublisherEventType() {
        // Required for reflection.
    }

    public PublisherEventType(PublisherVO<?> pub, int publisherEventTypeId) {
        this.publisherId = pub.getId();
        this.publisherEventTypeId = publisherEventTypeId;
        supplyReference1(() -> {
            return pub;
        });
    }

    public PublisherEventType(int publisherId, int publisherEventTypeId) {
        this.publisherId = publisherId;
        this.publisherEventTypeId = publisherEventTypeId;
        supplyReference1(() -> {
            return Common.getBean(PublisherDao.class).get(publisherId);
        });
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
    public DuplicateHandling getDuplicateHandling() {
        return DuplicateHandling.IGNORE;
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
        publisherId = 0;
        publisherEventTypeId = 0;
        String xid = jsonObject.getString("XID");
        String eventType = jsonObject.getString("publisherEventTypeId");
        if (xid != null) {
            PublisherVO<?> pb = getPublisher(jsonObject, "XID");
            publisherId = pb.getId();
            if (eventType != null) {
                publisherEventTypeId = getInt(jsonObject, "publisherEventTypeId", pb.getEventCodes());
            }
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        String xid = null;
        String eventType = null;
        if(publisherId > 0) {
            PublisherVO<?> pub = PublisherDao.getInstance().get(publisherId);
            xid = pub.getXid();
            if (publisherEventTypeId > 0) {
                eventType = pub.getEventCodes().getCode(publisherEventTypeId);
            }
        }
        writer.writeEntry("XID", xid);
        writer.writeEntry("publisherEventTypeId", eventType);
    }

    @Override
    public boolean hasPermission(PermissionHolder user, PermissionService service) {
        return service.hasAdminRole(user) || service.hasEventsSuperadminViewPermission(user);
    }

    @Override
    public MangoPermission getEventPermission(Map<String, Object> context, PermissionService service) {
        return MangoPermission.superadminOnly();
    }
}
