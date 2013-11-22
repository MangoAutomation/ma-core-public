/*
 *  Copyright (C) 2013 Deltamation Software. All rights reserved.
 *  @author Jared Wiltshire
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Provides an API to retrieve, update and save
 * VO objects from and to the database.
 * 
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Jared Wiltshire
 */
public abstract class AbstractDao<T extends AbstractVO<T>> extends AbstractBasicDao<T> {
    /*
     * SQL templates
     * 
     */
    protected final String SELECT_ALL;
    protected final String SELECT_ALL_SORT;
    protected final String SELECT_ALL_FIXED_SORT;
    protected final String SELECT_BY_ID;
    protected final String SELECT_BY_XID;
    protected final String SELECT_BY_NAME;
    protected final String INSERT;
    protected final String UPDATE;
    protected final String DELETE;
    protected final String COUNT;
    
    public final String tableName;
    public final String xidPrefix;
    
    protected final String typeName; //Type name for Audit Events
    
    protected int[] updateStatementPropertyTypes; //Required for Derby LOBs
    protected int[] insertStatementPropertyTypes; //Required for Derby LOBs
    
    protected AbstractDao(String typeName, String tablePrefix, String[] extraProperties, String extraSQL) {
        super(tablePrefix);
        this.typeName = typeName;
        tableName = getTableName();
        xidPrefix = getXidPrefix();
       
        
        // generate SQL statements
        
        String selectAll = "SELECT ";
        String insert = "INSERT INTO " + tableName + " (";
        String insertValues = "";
        String update = "UPDATE " + tableName + " SET ";
        
        // don't the first property - "id", in the insert statements
        for (int i = 0; i < properties.size(); i++) {
            String prop = properties.get(i);
            
            String selectPrefix = (i == 0) ? this.tablePrefix : "," + this.tablePrefix;
            selectAll += selectPrefix + prop;
            
            String insertPrefix = (i == 1) ? "" : ",";
            if (i >= 1) {
                insert += insertPrefix + prop;
                insertValues += insertPrefix + "?";
                update += insertPrefix + prop + "=?";
            }
        }
        
        for(String prop : extraProperties){
        	selectAll += "," + prop;
        }
        
        //Add the table prefix to the queries if necessary
        if(this.tablePrefix.equals("")){
	        if(extraSQL != null)
	        	SELECT_ALL = selectAll + " FROM " + tableName + " " + extraSQL;
	        else
	        	SELECT_ALL = selectAll + " FROM " + tableName;
	 
	        SELECT_ALL_SORT = SELECT_ALL + " ORDER BY ";
	        if (properties.contains("name")) {
	            SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY name ASC";
	        }
	        else {
	            SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY id ASC";
	        }
	        
	        SELECT_BY_ID = SELECT_ALL + " WHERE id=?";
	        SELECT_BY_XID = SELECT_ALL + " WHERE xid=?";
	        SELECT_BY_NAME = SELECT_ALL + " WHERE name=?";
	        INSERT = insert + ") VALUES (" + insertValues + ")";
	        UPDATE = update + " WHERE id=?";
	        DELETE = "DELETE FROM " + tableName + " WHERE id=?";
	        if(extraSQL != null)
	        	COUNT = "SELECT COUNT(*) FROM " + tableName  + " " + extraSQL;
	        else
	           	COUNT = "SELECT COUNT(*) FROM " + tableName;

        }else{
	        //this.tablePrefix will end in a . where the local tablePrefix shouldn't
	        if(extraSQL != null)
	        	SELECT_ALL = selectAll + " FROM " + tableName + " AS " + tablePrefix + " " + extraSQL;
	        else
	        	SELECT_ALL = selectAll + " FROM " + tableName + " AS " + tablePrefix;
	 
	        SELECT_ALL_SORT = SELECT_ALL + " ORDER BY ";
	        if (properties.contains("name")) {
	            SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY "+ this.tablePrefix + "name ASC";
	        }
	        else {
	            SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY " + this.tablePrefix + "id ASC";
	        }
	        
	        SELECT_BY_ID = SELECT_ALL + " WHERE " +  this.tablePrefix + "id=?";
	        SELECT_BY_XID = SELECT_ALL + " WHERE " + this.tablePrefix + "xid=?";
	        SELECT_BY_NAME = SELECT_ALL + " WHERE " + this.tablePrefix + "name=?";
	        INSERT = insert + ") VALUES (" + insertValues + ")";
	        UPDATE = update + " WHERE id=?";
	        DELETE = "DELETE FROM " + tableName + " WHERE id=?";
	        if(extraSQL != null)
	        	COUNT = "SELECT COUNT(*) FROM " + tableName + " AS " + tablePrefix + " " + extraSQL;
	        else
	           	COUNT = "SELECT COUNT(*) FROM " + tableName + " AS " + tablePrefix ;
        }
        
        
        //Create the Update Properties
        if(this.getPropertyTypes() != null){
        	this.updateStatementPropertyTypes = new int[this.propertyTypes.size() + 1];
        	this.insertStatementPropertyTypes = new int[this.propertyTypes.size()];
 
        	int i = 0;
        	for(i=0; i<this.propertyTypes.size(); i++){
        		this.updateStatementPropertyTypes[i] = this.propertyTypes.get(i);
           		this.insertStatementPropertyTypes[i] = this.propertyTypes.get(i);
        	}
        	
        	this.updateStatementPropertyTypes[i] = getIndexType();
        }
        
    }
    
    //TODO Make this call the other constructor
    protected AbstractDao(String typeName) {
        this(typeName, null, new String[0], null);
    }
    
    /**
     * Gets the table name that the Dao operates on
     * @return table name
     */
    protected abstract String getTableName();
    
    /**
     * Gets the XID prefix for XID generation
     * @return XID prefix, null if XIDs not supported
     */
    protected abstract String getXidPrefix();
    
    /**
     * Converts a VO object into an array of objects for insertion
     * or updating of database
     * @param vo to convert
     * @return object array
     */
    protected abstract Object[] voToObjectArray(T vo);
    
    /**
     * Generates a unique XID
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
     * @param XID to check
     * @param excludeId
     * @return True if XID is unique
     */
    public boolean isXidUnique(String xid, int excludeId) {
        return isXidUnique(xid, excludeId, tableName);
    }
    
    /**
     * Delete the vo from the db
     * @param id
     */
    public void delete(int id) {
        T vo = get(id);
        if (vo != null) {
            ejt.update(DELETE, id);
            AuditEventType.raiseDeletedEvent(this.typeName, vo);
        }
    }
    
    /**
     * Deletes a machine and also any associated data points
     * @param id
     */
    public void deleteIncludingPoints(int id) {
        delete(id);
    }

    /* (non-Javadoc)
     * @see com.deltamation.mango.downtime.db.AbstractBasicDao#get(int)
     */
    public T get(int id) {
        return queryForObject(SELECT_BY_ID, new Object[] {id}, getRowMapper(), null);
    }

    /* (non-Javadoc)
     * @see com.deltamation.mango.downtime.db.AbstractBasicDao#getAll()
     */
    public List<T> getAll() {
        return query(SELECT_ALL_FIXED_SORT, getRowMapper());
    }
    
    /* (non-Javadoc)
     * @see com.deltamation.mango.downtime.db.AbstractBasicDao#count()
     */
    public int count() {
        return ejt.queryForInt(COUNT);
    }
    
    /**
     * Find a vo by its XID
     * @param xid XID to search for
     * @return vo if found, otherwise null
     */
    public T getByXid(String xid) {
        if (!getProperties().contains("xid") || xid == null) {
            return null;
        }
        
        return queryForObject(SELECT_BY_XID, new Object[] {xid}, getRowMapper(), null);
    }
    
    /**
     * Find VOs by name
     * @param name name to search for
     * @return List of VO with matching name
     */
    public List<T> getByName(String name) {
        return query(SELECT_BY_NAME,  new Object[] {name}, getRowMapper());
    }

    
    /**
     * Get all vo in the system, sorted by column
     * @param column
     * @return List of all vo
     */
    public List<T> getAllSorted(String column) {
        return getAllSorted(column, true);
    }
    
    /**
     * Get all vo in the system, sorted by column
     * @param column
     * @return List of all vo
     */
    public List<T> getAllSorted(String column, boolean ascending) {
        String sort;
        if (ascending) {
            sort = " ASC";
        }
        else {
            sort = " DESC";
        }
        return query(SELECT_ALL_SORT + column + sort, getRowMapper());
    }
    
    /**
     * Get all vo in the system
     * @return List of all vo
     */
    public List<T> getRange(int offset, int limit) {
        List<Object> args = new ArrayList<Object>();
        String sql = SELECT_ALL_FIXED_SORT;
        sql = applyRange(sql, args, offset, limit);
        return query(sql, args.toArray(), getRowMapper());
    }

    
    /**
     * Persist the vo or if it already exists update it
     * @param vo to save
     */
    public void save(T vo) {
        if (vo.getId() == Common.NEW_ID) {
            insert(vo);
        } else {
            update(vo);
        }
    }
    
    /**
     * Insert a new vo and assign the ID
     * @param vo to insert
     */
    protected void insert(T vo) {
        if (vo.getXid() == null) {
            vo.setXid(generateUniqueXid());
        }
        int id = -1;
        if(insertStatementPropertyTypes == null)
        	id = doInsert(INSERT, voToObjectArray(vo));
        else
        	id = doInsert(INSERT, voToObjectArray(vo), insertStatementPropertyTypes);
        vo.setId(id);
        AuditEventType.raiseAddedEvent(this.typeName, vo);
    }

    /**
     * Update a vo
     * @param vo to update
     */
    protected void update(T vo) {
        List<Object> list = new ArrayList<Object>();
        list.addAll(Arrays.asList(voToObjectArray(vo)));
        list.add(vo.getId());
        
        T old = get(vo.getId());
        if(updateStatementPropertyTypes == null)
        	ejt.update(UPDATE, list.toArray());
        else
        	ejt.update(UPDATE, list.toArray(),updateStatementPropertyTypes);
        	
        
        AuditEventType.raiseChangedEvent(this.typeName, old, vo);
    }
    
    /**
     * Creates a new vo by copying an existing one
     * @param existingId ID of existing vo
     * @param newXid XID for the new vo
     * @param newName Name for the new vo
     * @return Copied vo with new XID and name
     */
    public int copy(final int existingId, final String newXid, final String newName) {
        TransactionCallback<Integer> callback = new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                T vo = get(existingId);
    
                // Copy the vo
                T copy = vo.copy();
                copy.setId(Common.NEW_ID);
                copy.setXid(newXid);
                copy.setName(newName);
                save(copy);

                // Copy permissions. 
                // TODO Add permissions to vo
                copyPermissions(vo.getId(), copy.getId());
                return copy.getId();
            }
        };
        
        return getTransactionTemplate().execute(callback);
    }
    
    /**
     * Copy Permissions from one object to another, 
     * to be over ridden in sub class if necessary
     * @param fromId
     * @param toId
     */
    public void copyPermissions(final int fromId, final int toId){
    	//No Op
    }
    
    /**
     * Return a new VO
     * @return
     */
    public abstract T getNewVo();

    /**
     * Save a VO AND its FKs
     * @param vo
     */
    public void saveFull(T vo) {
        save(vo);
    }
    
    /**
     * Method callback for when a row is returned from the DB
     * but will not make it to the VIEW due to filtering of the result set
     * 
	 * @return
	 */
	protected DojoQueryCallback<T> getOnFilterCallback() {
		return new DojoQueryCallback<T>(false);
	}

	/**
	 * 
	 * Method callback for when a row is returned from the DB
	 * and isn't filtered and will for sure be returned to the View
	 * 
	 * @return
	 */
	protected DojoQueryCallback<T> getOnResultCallback() {
		return new DojoQueryCallback<T>(true);
	}
    
    
    /* (non-Javadoc)
     * @see com.deltamation.mango.downtime.db.AbstractBasicDao#dojoQuery(java.util.Map, java.util.List, java.lang.Integer, java.lang.Integer, boolean)
     */
    @Override
    public ResultsWithTotal dojoQuery(Map<String, String> query,
            List<SortOption> sort, Integer offset, Integer limit, boolean or) {
        return dojoQuery(SELECT_ALL, COUNT, query, sort, offset, limit, or,getOnResultCallback(),getOnFilterCallback());
    }

	/* (non-Javadoc)
     * @see com.deltamation.mango.downtime.db.AbstractBasicDao#dojoQuery(java.util.Map, java.util.List, java.lang.Integer, java.lang.Integer, boolean)
     */
    @Override
    public void exportQuery(Map<String, String> query,
            List<SortOption> sort, Integer offset, Integer limit, boolean or, DojoQueryCallback<T> onResultCallback) {
        dojoQuery(SELECT_ALL, COUNT, query, sort, offset, limit, or,onResultCallback,null);
    }
 
    
    
    
}
