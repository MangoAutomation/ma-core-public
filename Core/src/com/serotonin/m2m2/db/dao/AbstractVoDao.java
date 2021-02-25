/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.events.audit.AuditEvent;
import com.infiniteautomation.mango.spring.events.audit.ChangeAuditEvent;
import com.infiniteautomation.mango.spring.events.audit.CreateAuditEvent;
import com.infiniteautomation.mango.spring.events.audit.DeleteAuditEvent;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Provides an API to retrieve, update and save
 * VO objects from and to the database.
 *
 *
 * @author Jared Wiltshire
 * @author Terry Packer
 */
public abstract class AbstractVoDao<T extends AbstractVO, R extends Record, TABLE extends Table<R>> extends AbstractBasicDao<T, R, TABLE> implements AbstractVOAccess<T> {

    /**
     * For generating XIDs this is prepended to any XIDs generated
     */
    protected final String xidPrefix;
    /**
     * Audit event type name
     */
    protected final String auditEventType;

    /**
     *
     * @param auditEventType
     * @param table
     * @param mapper
     * @param publisher
     */
    protected AbstractVoDao(String auditEventType, TABLE table, ObjectMapper mapper, ApplicationEventPublisher publisher, PermissionService permissionService) {
        this(auditEventType, table, null, mapper, publisher, permissionService);
    }

    /**
     *
     * @param auditEventType
     * @param table
     * @param countMonitorName - If not null create a monitor to track table row count
     * @param mapper
     * @param publisher
     */
    protected AbstractVoDao(String auditEventType, TABLE table,
                            TranslatableMessage countMonitorName,
                            ObjectMapper mapper, ApplicationEventPublisher publisher, PermissionService permissionService) {
        super(table, countMonitorName, mapper, publisher, permissionService);
        this.xidPrefix = getXidPrefix();
        this.auditEventType = auditEventType;
    }

    /**
     * Gets the XID prefix for XID generation
     *
     * @return XID prefix, null if XIDs not supported
     */
    protected abstract String getXidPrefix();

    @Override
    public String generateUniqueXid() {
        if (xidPrefix == null) {
            return null;
        }
        return generateUniqueXid(xidPrefix, table.getName());
    }

    @Override
    public boolean isXidUnique(String xid, int excludeId) {
        return isXidUnique(xid, excludeId, table.getName());
    }

    public Field<String> getXidField() {
        Field<?> field = table.field("xid");
        if (field != null) {
            if (field.getDataType().isString()) {
                return field.coerce(String.class);
            }
        }
        return null;
    }

    public Field<String> getNameField() {
        Field<?> field = table.field("name");
        if (field != null) {
            if (field.getDataType().isString()) {
                return field.coerce(String.class);
            }
        }
        return null;
    }

    @Override
    public T getByXid(String xid) {
        Field<String> xidField = getXidField();
        if (xidField == null) {
            throw new UnsupportedOperationException("This table does not have an XID column");
        }
        Assert.notNull(xid, "Must supply xid");

        return getJoinedSelectQuery()
                .where(xidField.eq(xid))
                .limit(1)
                .fetchOne(this::mapRecordLoadRelationalData);
    }

    @Override
    public List<T> getByName(String name) {
        Field<String> nameField = getXidField();
        if (nameField == null) {
            throw new UnsupportedOperationException("This table does not have a name column");
        }
        Assert.notNull(name, "Must supply name");

        return getJoinedSelectQuery()
                .where(nameField.eq(name))
                .fetch(this::mapRecordLoadRelationalData);
    }

    @Override
    public Integer getIdByXid(String xid) {
        return this.create.select(getIdField()).from(table)
                .where(getXidField().eq(xid))
                .limit(1).fetchOneInto(Integer.class);
    }

    @Override
    public String getXidById(int id) {
        return this.create.select(getXidField()).from(table)
                .where(getIdField().eq(id))
                .limit(1).fetchOneInto(String.class);
    }

    @Override
    public boolean delete(T vo) {
        if(super.delete(vo)) {
            if (this.auditEventType != null) {
                publishAuditEvent(new DeleteAuditEvent<T>(this.auditEventType, Common.getUser(), vo));
            }
            return true;
        }else {
            return false;
        }
    }

    @Override
    public void insert(T vo) {
        if (vo.getXid() == null) {
            vo.setXid(generateUniqueXid());
        }
        super.insert(vo);
        if (this.auditEventType != null) {
            publishAuditEvent(new CreateAuditEvent<T>(this.auditEventType, Common.getUser(), vo));
        }
    }

    @Override
    public void update(T existing, T vo) {
        if (vo.getXid() == null) {
            vo.setXid(existing.getXid());
        }
        super.update(existing, vo);
        if (this.auditEventType != null) {
            publishAuditEvent(new ChangeAuditEvent<T>(this.auditEventType, Common.getUser(), existing, vo));
        }
    }

    @Override
    public void lockRow(String xid) {
        this.create.select().from(table)
        .where(getXidField().eq(xid))
        .forUpdate()
        .fetch();
    }

    protected void publishAuditEvent(AuditEvent event) {
        if (this.eventPublisher != null) {
            this.eventPublisher.publishEvent(event);
        }
    }

}
