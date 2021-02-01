/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.Table;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
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
    protected final String typeName; //Type name for Audit Events

    /**
     *
     * @param typeName
     * @param table
     * @param mapper
     * @param publisher
     */
    protected AbstractVoDao(String typeName, TABLE table, ObjectMapper mapper, ApplicationEventPublisher publisher) {
        this(typeName, table, null, mapper, publisher);
    }

    /**
     *
     * @param typeName
     * @param table
     * @param countMonitorName - If not null create a monitor to track table row count
     * @param mapper
     * @param publisher
     */
    protected AbstractVoDao(String typeName, TABLE table,
            TranslatableMessage countMonitorName,
            ObjectMapper mapper, ApplicationEventPublisher publisher) {
        super(table, countMonitorName, mapper, publisher);
        this.xidPrefix = getXidPrefix();
        this.typeName = typeName;
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
                return field.cast(String.class);
            }
        }
        return null;
    }

    public Field<String> getNameField() {
        Field<?> field = table.field("name");
        if (field != null) {
            if (field.getDataType().isString()) {
                return field.cast(String.class);
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

        Select<Record> query = this.getJoinedSelectQuery()
                .where(xidField.eq(xid))
                .limit(1);
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        T vo = ejt.query(sql, args.toArray(new Object[0]), getObjectResultSetExtractor());
        if(vo != null) {
            loadRelationalData(vo);
        }
        return vo;
    }

    @Override
    public List<T> getByName(String name) {
        Field<String> nameField = getXidField();
        if (nameField == null) {
            throw new UnsupportedOperationException("This table does not have a name column");
        }
        Assert.notNull(name, "Must supply name");

        Select<Record> query = this.getJoinedSelectQuery()
                .where(nameField.eq(name));
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        List<T> items = new ArrayList<>();
        query(sql, args.toArray(new Object[0]), getCallbackResultSetExtractor((item, index)->{
            loadRelationalData(item);
            items.add(item);
        }));
        return items;
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
            AuditEventType.raiseDeletedEvent(this.typeName, vo);
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
        AuditEventType.raiseAddedEvent(this.typeName, vo);
    }

    @Override
    public void update(T existing, T vo) {
        if (vo.getXid() == null) {
            vo.setXid(existing.getXid());
        }
        super.update(existing, vo);
        AuditEventType.raiseChangedEvent(this.typeName, existing, vo);
    }

    @Override
    public void lockRow(String xid) {
        this.create.select().from(table)
        .where(getXidField().eq(xid))
        .forUpdate()
        .fetch();
    }

}
