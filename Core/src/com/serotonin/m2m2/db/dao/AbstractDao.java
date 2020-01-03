/*
 *  Copyright (C) 2013 Deltamation Software. All rights reserved.
 *  @author Jared Wiltshire
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Provides an API to retrieve, update and save
 * VO objects from and to the database.
 *
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 *
 * @author Jared Wiltshire
 */
public abstract class AbstractDao<T extends AbstractVO<?>> extends AbstractBasicDao<T> implements AbstractVOAccess<T> {

    protected final String xidPrefix;
    protected final String typeName; //Type name for Audit Events

    /**
     * @param typeName - Type name for Audit events
     * @param tablePrefix - Table prefix for Selects/Joins
     * @param extraProperties - Any extra SQL for queries
     * @param useSubQuery - Compute queries as sub-queries
     * @param countMonitorName - If not null create a monitor to track table row count
     */
    protected AbstractDao(String typeName, String tablePrefix, String[] extraProperties, 
            boolean useSubQuery, TranslatableMessage countMonitorName,
            ObjectMapper mapper, ApplicationEventPublisher publisher) {
        super(tablePrefix, extraProperties, useSubQuery, countMonitorName, mapper, publisher);
        this.xidPrefix = getXidPrefix();
        this.typeName = typeName;
    }

    /**
     * @param typeName - Type name for Audit events
     * @param tablePrefix - Table prefix for Selects/Joins
     * @param extraProperties - Any extra SQL for queries
     */
    protected AbstractDao(String typeName, String tablePrefix, String[] extraProperties, ObjectMapper mapper, ApplicationEventPublisher publisher) {
        this(typeName, tablePrefix, extraProperties, false, null, mapper, publisher);
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
        return generateUniqueXid(xidPrefix, tableName);
    }

    @Override
    public boolean isXidUnique(String xid, int excludeId) {
        return isXidUnique(xid, excludeId, tableName);
    }

    @Override
    public T getByXid(String xid) {
        if (xid == null || !this.propertyTypeMap.keySet().contains("xid")) {
            return null;
        }

        T vo = queryForObject(SELECT_BY_XID, new Object[] { xid }, getRowMapper(), null);
        if(vo != null) {
            loadRelationalData(vo);
        }
        return vo;
    }

    @Override
    public List<T> getByName(String name) {
        if (name == null || !this.propertyTypeMap.keySet().contains("name")) {
            return null;
        }
        List<T> items = new ArrayList<>();
        query(SELECT_BY_NAME, new Object[] { name }, getCallbackResultSetExtractor((item, index)->{
            loadRelationalData(item);
            items.add(item);
        }));
        return items;
    }

    @Override
    public Integer getIdByXid(String xid) {
        return this.queryForObject(SELECT_ID_BY_XID, new Object[] { xid }, Integer.class, null);
    }

    @Override
    public String getXidById(int id) {
        return this.queryForObject(SELECT_XID_BY_ID, new Object[] { id }, String.class, null);
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
    
    /**
     * Creates a new vo by copying an existing one
     *
     * @param existingId
     *            ID of existing vo
     * @param newXid
     *            XID for the new vo
     * @param newName
     *            Name for the new vo
     * @return Copied vo with new XID and name
     * @param full - copy FKs?
     * @return
     */
    public int copy(final int existingId, final String newXid, final String newName, boolean full) {
        TransactionCallback<Integer> callback = new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                T vo = get(existingId);

                // Copy the vo
                @SuppressWarnings("unchecked")
                T copy = (T)vo.copy();
                copy.setId(Common.NEW_ID);
                copy.setXid(newXid);
                copy.setName(newName);
                insert(copy);
                
                // Copy permissions.
                return copy.getId();
            }
        };

        return getTransactionTemplate().execute(callback);
    }
    
    @Override
    protected DaoEvent<T> createDaoEvent(DaoEventType type, T vo, T existing) {
        switch(type) {
            case CREATE:
                return new DaoEvent<T>(this, type, vo, null);
            case UPDATE:
                return new DaoEvent<T>(this, type, vo, existing.getXid());
            case DELETE:
                return new DaoEvent<T>(this, type, vo, existing.getXid());
            default:
                throw new ShouldNeverHappenException("Uknown dao event type: " + type);
        }
        
    }

}
