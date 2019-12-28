/*
 *  Copyright (C) 2013 Deltamation Software. All rights reserved.
 *  @author Jared Wiltshire
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

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
public abstract class AbstractDao<T extends AbstractVO<?>> extends AbstractBasicDao<T> {

    protected final String xidPrefix;
    protected final String typeName; //Type name for Audit Events

    /**
     * @param typeName - Type name for Audit events
     * @param tablePrefix - Table prefix for Selects/Joins
     * @param extraProperties - Any extra SQL for queries
     * @param useSubQuery - Compute queries as sub-queries
     * @param countMonitorName - If not null create a monitor to track table row count
     */
    protected AbstractDao(String typeName, String tablePrefix, String[] extraProperties, boolean useSubQuery, TranslatableMessage countMonitorName) {
        super(tablePrefix, extraProperties, useSubQuery, countMonitorName);
        this.xidPrefix = getXidPrefix();
        this.typeName = typeName;
    }

    /**
     * @param typeName - Type name for Audit events
     * @param tablePrefix - Table prefix for Selects/Joins
     * @param extraProperties - Any extra SQL for queries
     */
    protected AbstractDao(String typeName, String tablePrefix, String[] extraProperties) {
        this(typeName, tablePrefix, extraProperties, false, null);
    }

    /**
     * @param typeName - Type name for Audit events
     */
    protected AbstractDao(String typeName) {
        this(typeName, null, new String[0]);
    }

    /**
     * @param typeName - Type name for Audit events
     * @param countMonitorName - If not null used to track count of table rows
     */
    protected AbstractDao(String typeName, TranslatableMessage countMonitorName) {
        this(typeName, null, new String[0], false, countMonitorName);
    }

    /**
     * Gets the XID prefix for XID generation
     *
     * @return XID prefix, null if XIDs not supported
     */
    protected abstract String getXidPrefix();

    /**
     * Generates a unique XID
     *
     * @return A new unique XID, null if XIDs are not supported
     */
    public String generateUniqueXid() {
        if (xidPrefix == null) {
            return null;
        }
        return generateUniqueXid(xidPrefix, tableName);
    }

    /**
     * Checks if a XID is unique
     *
     * @param XID
     *            to check
     * @param excludeId
     * @return True if XID is unique
     */
    public boolean isXidUnique(String xid, int excludeId) {
        return isXidUnique(xid, excludeId, tableName);
    }

    /**
     * Find a VO by its XID
     *
     * @param xid
     *            XID to search for
     * @return vo if found, otherwise null
     */
    public T getByXid(String xid, boolean full) {
        if (xid == null || !this.propertyTypeMap.keySet().contains("xid")) {
            return null;
        }

        T vo = queryForObject(SELECT_BY_XID, new Object[] { xid }, getRowMapper(), null);
        if(vo != null && full) {
            loadRelationalData(vo);
        }
        return vo;
    }

    /**
     * Find VOs by name
     *
     * @param name
     *            name to search for
     * @return List of VO with matching name
     */
    public List<T> getByName(String name, boolean full) {
        if (name == null || !this.propertyTypeMap.keySet().contains("name")) {
            return null;
        }
        List<T> items = new ArrayList<>();
        query(SELECT_BY_NAME, new Object[] { name }, getCallbackResultSetExtractor((item, index)->{
            if(full) {
                loadRelationalData(item);
            }
            items.add(item);
        }));
        return items;
    }

    /**
     * Get all vo in the system
     *
     * @return List of all vo
     */
    public List<T> getRange(int offset, int limit) {
        List<Object> args = new ArrayList<>();
        String sql = SELECT_ALL_FIXED_SORT;
        sql = applyRange(sql, args, offset, limit);
        return query(sql, args.toArray(), getRowMapper());
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
    public void insert(T vo, boolean full) {
        if (vo.getXid() == null) {
            vo.setXid(generateUniqueXid());
        }
        super.insert(vo, full);
        AuditEventType.raiseAddedEvent(this.typeName, vo);
    }
    
    @Override
    public void update(T existing, T vo, boolean full) {
        if (vo.getXid() == null) {
            vo.setXid(existing.getXid());
        }
        super.update(existing, vo, full);
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
                T vo = get(existingId, full);

                // Copy the vo
                @SuppressWarnings("unchecked")
                T copy = (T)vo.copy();
                copy.setId(Common.NEW_ID);
                copy.setXid(newXid);
                copy.setName(newName);
                insert(copy, full);
                
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
