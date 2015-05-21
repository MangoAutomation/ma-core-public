/*
 *  Copyright (C) 2013 Deltamation Software. All rights reserved.
 *  @author Jared Wiltshire
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jazdw.rql.parser.ASTNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import com.infiniteautomation.mango.db.query.BaseSqlQuery;
import com.infiniteautomation.mango.db.query.QueryAttribute;
import com.infiniteautomation.mango.db.query.RQLToSQLSelect;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;
import com.infiniteautomation.mango.db.query.SQLStatement;
import com.infiniteautomation.mango.db.query.StreamableSqlQuery;
import com.infiniteautomation.mango.db.query.TableModel;
import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;

/**
 * Provides an API to retrieve, update and save
 * VO objects from and to the database.
 * 
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * @author Jared Wiltshire
 */
public abstract class AbstractBasicDao<T> extends BaseDao {
    protected Log LOG = LogFactory.getLog(AbstractBasicDao.class);
    
    public static final String WHERE = " WHERE ";
    public static final String OR = " OR ";
    public static final String AND = " AND ";
    public static final String LIMIT = " LIMIT ";
    
    //Map UI or Model member names to the Database Column Names they will get translated when the query is generated
    protected final Map<String, IntStringPair> propertiesMap;
    
    //Map of Database Column Names to Column SQL Type
    protected final LinkedHashMap<String, Integer> propertyTypeMap;
    
    
    /*
     * SQL templates
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

    protected final String typeName; //Type name for Audit Events

    protected int[] updateStatementPropertyTypes; //Required for Derby LOBs
    protected int[] insertStatementPropertyTypes; //Required for Derby LOBs
    
    protected TableModel tableModel;
    
    public final String tablePrefix;  //Select * from table as tablePrefix
  
    /**
     * Override as necessary
     * Can be null if no Pk Exists
     * @return String name of Pk Column
     */
    public String getPkColumnName(){
    	return "id";
    }
	

	
	/**
     * Provide a table prefix to use for complex queries.  Ie. Joins
     * Do not include the . at the end of the prefix
     * @param tablePrefix
     */
    public AbstractBasicDao(String typeName, String tablePrefix, String[] extraProperties, String extraSQL){
       Map<String,IntStringPair> propMap = getPropertiesMap();
       if(propMap == null)
    	   this.propertiesMap = new HashMap<String, IntStringPair>();
       else
    	   this.propertiesMap = propMap;
       
       LinkedHashMap<String,Integer> propTypeMap = getPropertyTypeMap();
       if(propTypeMap == null)
    	   throw new ShouldNeverHappenException("Property Type Map is required!");
       else
    	   this.propertyTypeMap = propTypeMap;

    	
    	if(tablePrefix != null)
    		this.tablePrefix = tablePrefix + ".";
    	else
    		this.tablePrefix = "";
    	
        this.typeName = typeName;
        tableName = getTableName();


        // generate SQL statements
        String selectAll = "SELECT ";
        String insert = "INSERT INTO " + tableName + " (";
        String insertValues = "";
        String update = "UPDATE " + tableName + " SET ";

        //Map of properties to their QueryAttribute
		Map<String, QueryAttribute> attributeMap = new HashMap<String, QueryAttribute>();

        Set<String> properties = this.propertyTypeMap.keySet();
        // don't the first property - "id", in the insert statements
        int i=0;
        for (String prop : properties) {
        	
        	//Add this attribute
        	QueryAttribute attribute = new QueryAttribute();
        	attribute.setColumnName(this.tablePrefix + prop);
        	attribute.addAlias(prop);
        	attribute.setSqlType(this.propertyTypeMap.get(prop));
        	attributeMap.put(prop, attribute);
        	
        	String selectPrefix = (i == 0) ? this.tablePrefix : "," + this.tablePrefix;
            selectAll += selectPrefix + prop;

            String insertPrefix = (i == 1) ? "" : ",";
            if (i >= 1) {
                insert += insertPrefix + prop;
                insertValues += insertPrefix + "?";
                update += insertPrefix + prop + "=?";
            }
            i++;
        }

        for (String prop : extraProperties) {
            selectAll += "," + prop;
        }

        //Add the table prefix to the queries if necessary
        if (this.tablePrefix.equals("")) {
            if (extraSQL != null)
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
            if (extraSQL != null)
                COUNT = "SELECT COUNT(*) FROM " + tableName + " " + extraSQL;
            else
                COUNT = "SELECT COUNT(*) FROM " + tableName;

        }
        else {
            //this.tablePrefix will end in a . where the local tablePrefix shouldn't
            if (extraSQL != null)
                SELECT_ALL = selectAll + " FROM " + tableName + " AS " + tablePrefix + " " + extraSQL;
            else
                SELECT_ALL = selectAll + " FROM " + tableName + " AS " + tablePrefix;

            SELECT_ALL_SORT = SELECT_ALL + " ORDER BY ";
            if (properties.contains("name")) {
                SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY " + this.tablePrefix + "name ASC";
            }
            else {
                SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY " + this.tablePrefix + "id ASC";
            }

            SELECT_BY_ID = SELECT_ALL + " WHERE " + this.tablePrefix + "id=?";
            SELECT_BY_XID = SELECT_ALL + " WHERE " + this.tablePrefix + "xid=?";
            SELECT_BY_NAME = SELECT_ALL + " WHERE " + this.tablePrefix + "name=?";
            INSERT = insert + ") VALUES (" + insertValues + ")";
            UPDATE = update + " WHERE id=?";
            DELETE = "DELETE FROM " + tableName + " WHERE id=?";
            if (extraSQL != null)
                COUNT = "SELECT COUNT(*) FROM " + tableName + " AS " + tablePrefix + " " + extraSQL;
            else
                COUNT = "SELECT COUNT(*) FROM " + tableName + " AS " + tablePrefix;
        }

        //Create the Update and Insert property types lists
        if((getPkColumnName() != null)&&(this.propertyTypeMap.get(getPkColumnName()) != null)){
        	this.updateStatementPropertyTypes = new int[this.propertyTypeMap.size()];
        	this.insertStatementPropertyTypes = new int[this.propertyTypeMap.size()-1];
        }else{
        	this.updateStatementPropertyTypes = new int[this.propertyTypeMap.size()];
        	this.insertStatementPropertyTypes = new int[this.propertyTypeMap.size()];
        }
        

        Iterator<String> it = this.propertyTypeMap.keySet().iterator();
        int j=0;
        while(it.hasNext()){
        	String property = it.next();
        	if(!property.equals(getPkColumnName())){
	        	Integer type = this.propertyTypeMap.get(property);
	        	this.updateStatementPropertyTypes[j] = type;
	        	this.insertStatementPropertyTypes[j] = type;
	        	j++;
        	}
        }
        
        if((getPkColumnName() != null)&&(this.propertyTypeMap.get(getPkColumnName()) != null)){
        	Integer pkType = this.propertyTypeMap.get(getPkColumnName());
       		this.updateStatementPropertyTypes[j] =  pkType;
       		attributeMap.put(getPkColumnName(), new QueryAttribute(getPkColumnName(), "id", new HashSet<String>(), pkType));
        }
        
		Iterator<String> propertyMapIterator = this.propertiesMap.keySet().iterator();	
        while(propertyMapIterator.hasNext()){
        	String propertyName = propertyMapIterator.next();
        	IntStringPair pair = this.propertiesMap.get(propertyName);
        	
        	QueryAttribute attribute = attributeMap.get(pair.getValue());
        	if(attribute != null){
        		attribute.addAlias(propertyName);
        	}else{
        		QueryAttribute newAttribute = new QueryAttribute();
        		newAttribute.setColumnName(pair.getValue());
        		newAttribute.setSqlType(pair.getKey());
        		newAttribute.addAlias(propertyName);
        		attributeMap.put(propertyName, newAttribute);
        	}
        }

		//Create the model
		this.tableModel = new TableModel(this.getTableName(), new ArrayList<QueryAttribute>(attributeMap.values()));
    	
    }


    /**
     * Gets the table name that the Dao operates on
     * 
     * @return table name
     */
    protected abstract String getTableName();


    /**
     * Converts a VO object into an array of objects for insertion
     * or updating of database
     * 
     * @param vo
     *            to convert
     * @return object array
     */
    protected abstract Object[] voToObjectArray(T vo);
	
	/**
	 * Both the properties and the type maps must be setup
	 * @return
	 */
	protected LinkedHashMap<String,Integer> getPropertyTypeMap(){
		return new LinkedHashMap<String,Integer>();
	}
	
	/**
     * Returns a map which maps a virtual property to a real one used
     * for sorting/filtering from the database
     * e.g. dateFormatted -> timestamp
     * @return map of properties
     */
    protected abstract Map<String, IntStringPair> getPropertiesMap();
    
    /**
     * Gets the row mapper for converting the retrieved database
     * values into a VO object
     * @return row mapper
     */
    public abstract RowMapper<T> getRowMapper();


    /**
     * Return a VO with FKs populated
     * @param id
     * @return
     */
    public T getFull(int id) {
        return get(id);
    }
    
    
    /**
     * Get By ID
     * @param id
     * @return
     */
    public T get(int id) {
        return queryForObject(SELECT_BY_ID, new Object[] { id }, getRowMapper(), null);
    }

    
    /**
     * Get All from table
     * @return
     */
    public List<T> getAll() {
        return query(SELECT_ALL_FIXED_SORT, getRowMapper());
    }

    /**
     * Count all from table
     * @return
     */
    public int count() {
        return ejt.queryForInt(COUNT, new Object[0], 0);
    }
    
    /**
     * Return all VOs with FKs Populated
     * @return
     */
    public List<T> getAllFull() {
        return getAll();
    }

    protected String applyRange(String sql, List<Object> args, Integer offset, Integer limit) {
        if (offset == null || limit == null) {
            return sql;
        }
        
        switch (Common.databaseProxy.getType()) {
        case MYSQL:
        case POSTGRES:
            args.add(limit);
            args.add(offset);
            return sql + " LIMIT ? OFFSET ?";
        case DERBY:
        case MSSQL:
        case H2:
            args.add(offset);
            args.add(limit);
            return sql + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        default:
        	LOG.warn("No case for adding limit to database of type: " + Common.databaseProxy.getType());
            return sql;
        }
    }
    
    protected String applyLimit(String sql, List<Object> args, Integer limit) {
        if (limit == null) {
            return sql;
        }
        
        switch (Common.databaseProxy.getType()) {
        case MYSQL:
        case POSTGRES:
        case DERBY:
        case MSSQL:
        case H2:
            args.add(limit);
            return sql + " LIMIT ? ";
        default:
        	LOG.warn("No case for adding limit to database of type: " + Common.databaseProxy.getType());
            return sql;
        }
    }

	
    /**
     * 
     * @param root
     * @return
     */
    public StreamableSqlQuery<T> createQuery(ASTNode root, MappedRowCallback<T> selectCallback,
            MappedRowCallback<Long> countCallback, Map<String,String> modelMap, Map<String, SQLColumnQueryAppender> modifiers){
    	
    	SQLStatement statement = new SQLStatement(SELECT_ALL, COUNT);
    	root.accept(new RQLToSQLSelect<T>(this, modelMap, modifiers), statement);
    	
        return new StreamableSqlQuery<T>(this, statement, selectCallback, countCallback);
    }
    
    public BaseSqlQuery<T> createQuery(ASTNode root){
    	
    	SQLStatement statement = new SQLStatement(SELECT_ALL, COUNT);
    	root.accept(new RQLToSQLSelect<T>(this), statement);
    	
    	return new BaseSqlQuery<T>(this, statement);
    }
    
    
	/**
	 * @return
	 */
	public TableModel getTableModel() {
		return tableModel;
	}

	/**
	 * @param argument
	 * @return
	 */
	public SQLQueryColumn getQueryColumn(String prop) {
		boolean mapped = false;
        
    	Set<String> properties = this.propertyTypeMap.keySet();
    	String dbCol = prop;

    	int sqlType;
    	if (propertiesMap.containsKey(prop)) {
    		IntStringPair pair = propertiesMap.get(prop);
    		dbCol = pair.getValue();
            sqlType = pair.getKey();
    		mapped = true;
        }else{
        	sqlType = this.propertyTypeMap.get(dbCol);
        }
        
        if (mapped || properties.contains(dbCol)) {
            if(mapped)
            	return new SQLQueryColumn(dbCol, sqlType);
            else
            	return new SQLQueryColumn(this.tablePrefix + dbCol, sqlType);
        }
        
        //TODO Change this to be handled more gracefully
        //No Column matches...
        throw new ShouldNeverHappenException("No column found for: " + prop);
	}

}
