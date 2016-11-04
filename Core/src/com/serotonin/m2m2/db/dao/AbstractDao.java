/*
 *  Copyright (C) 2013 Deltamation Software. All rights reserved.
 *  @author Jared Wiltshire
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.infiniteautomation.mango.db.query.SortOption;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DeltamationCommon;
import com.serotonin.m2m2.module.WebSocketDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler;

/**
 * Provides an API to retrieve, update and save
 * VO objects from and to the database.
 * 
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * 
 * @author Jared Wiltshire
 */
public abstract class AbstractDao<T extends AbstractVO<T>> extends AbstractBasicDao<T> {

    public final String xidPrefix;
    
    protected final String typeName; //Type name for Audit Events
    
    //Map of Property to a Comparator, useful when the Property is stored in a BLOB in the database
    protected final Map<String, Comparator<T>> comparatorMap = getComparatorMap();
    
    //Map of  Property to a filter, useful when the Property is stored in a BLOB in the database
    protected final Map<String, IFilter<T>> filterMap = getFilterMap();
    
    //Provide Arguments for Mapped Property members to be sorted by (Not really sure if this is necessary)
    protected final Map<String,PropertyArguments> propertyArgumentsMap = getPropertyArgumentsMap();
    
    protected AbstractDao(DaoNotificationWebSocketHandler<T> handler, String typeName, String tablePrefix, String[] extraProperties, boolean useSubQuery) {
        super(handler, tablePrefix, extraProperties, useSubQuery);
        this.xidPrefix = getXidPrefix();
        this.typeName = typeName;
    }
    
    @SuppressWarnings("unchecked")
	protected AbstractDao(WebSocketDefinition def, String typeName, String tablePrefix, String[] extraProperties, boolean useSubQuery) {
        super((DaoNotificationWebSocketHandler<T>) (def != null ? def.getHandler() : null), tablePrefix, extraProperties, useSubQuery);
        this.xidPrefix = getXidPrefix();
        this.typeName = typeName;
    }
    
    /**
     * 
     * @param typeName - Audit Event Type
     * @param tablePrefix - Table prefix for use in Joins
     * @param extraProperties - Properties from other tables
     * @param extraSQL - Any additional query pieces
     */
    protected AbstractDao(DaoNotificationWebSocketHandler<T> handler, String typeName, String tablePrefix, String[] extraProperties) {
        this(handler, typeName, tablePrefix, extraProperties, false);
    }

    protected AbstractDao(WebSocketDefinition def, String typeName, String tablePrefix, String[] extraProperties) {
        this(def, typeName, tablePrefix, extraProperties, false);
    }
    
    
    /**
     * 
     * @param typeName - Audit Event Type Name
     */
    protected AbstractDao(DaoNotificationWebSocketHandler<T> handler, String typeName) {
        this(handler, typeName, null, new String[0]);
    }

    protected AbstractDao(WebSocketDefinition def, String typeName) {
        this(def, typeName, null, new String[0]);
    }
    
    /**
     * Gets the XID prefix for XID generation
     * 
     * @return XID prefix, null if XIDs not supported
     */
    protected abstract String getXidPrefix();
    
    /**
     * Override to add a mapping for properties that are not 
     * directly accessible via a database column.
     * 
	 * @return
	 */
	protected Map<String, Comparator<T>> getComparatorMap() {
		return new HashMap<String,Comparator<T>>();
	}
	
	/**
	 * Override to add mappings for properties that are not
	 * directly accessible via a database column.
	 * @return
	 */
	protected Map<String, IFilter<T>> getFilterMap(){
		return new HashMap<String,IFilter<T>>();
	}
	
	protected Map<String, PropertyArguments> getPropertyArgumentsMap(){
		return new HashMap<String,PropertyArguments>();
	}

	/**
	 * Both the properties and the type maps must be setup
	 * @return
	 */
	protected HashMap<String,Integer> getMappedPropertyTypeMap(){
		return new HashMap<String,Integer>();
	}
    

	interface PropertyArguments{
		public Object[] getArguments();
	}

	
	
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
     * Find a vo by its XID
     * 
     * @param xid
     *            XID to search for
     * @return vo if found, otherwise null
     */
    public T getByXid(String xid) {
        if (!this.propertyTypeMap.keySet().contains("xid") || xid == null) {
            return null;
        }

        return queryForObject(SELECT_BY_XID, new Object[] { xid }, getRowMapper(), null);
    }

    /**
     * Find VOs by name
     * 
     * @param name
     *            name to search for
     * @return List of VO with matching name
     */
    public List<T> getByName(String name) {
        return query(SELECT_BY_NAME, new Object[] { name }, getRowMapper());
    }

    /**
     * Get all vo in the system, sorted by column
     * 
     * @param column
     * @return List of all vo
     */
    public List<T> getAllSorted(String column) {
        return getAllSorted(column, true);
    }

    /**
     * Get all vo in the system, sorted by column
     * 
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
     * 
     * @return List of all vo
     */
    public List<T> getRange(int offset, int limit) {
        List<Object> args = new ArrayList<>();
        String sql = SELECT_ALL_FIXED_SORT;
        sql = applyRange(sql, args, offset, limit);
        return query(sql, args.toArray(), getRowMapper());
    }

    /**
     * Persist the vo or if it already exists update it
     * 
     * @param vo
     *            to save
     */
    @Override
    public void save(T vo) {
    	save(vo, null);
    }

	/**
	 * 
	 * @param vo
	 * @param initiatorId
	 */
    public void save(T vo, String initiatorId) {
        if (vo.getId() == Common.NEW_ID) {
            insert(vo, initiatorId);
        }
        else {
            update(vo, initiatorId);
        }
    }
    
    /**
     * Insert a new vo and assign the ID
     * 
     * @param vo
     *            to insert
     */
    @Override
    protected void insert(T vo) {
    	insert(vo, null);
    }
    @Override
    protected void insert(T vo, String initiatorId) {
        if (vo.getXid() == null) {
            vo.setXid(generateUniqueXid());
        }
        super.insert(vo, initiatorId);
        AuditEventType.raiseAddedEvent(this.typeName, vo);
    }    

    /**
     * Update a vo
     * 
     * @param vo
     *            to update
     */
    @Override
    protected void update(T vo) {
    	update(vo, null);
    }
    @Override
    protected void update(T vo, String initiatorId){
        T old = get(vo.getId());
        super.update(vo);
        AuditEventType.raiseChangedEvent(this.typeName, old, vo);
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
                return copy.getId();
            }
        };

        return getTransactionTemplate().execute(callback);
    }

    /**
     * Return a new VO
     * 
     * @return
     */
    public abstract T getNewVo();

    /**
     * Save a VO AND its FKs
     * 
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
        return new DojoQueryCallback<>(false); //Don't keep results
    }

    /**
     * 
     * Method callback for when a row is returned from the DB
     * and isn't filtered and will for sure be returned to the View
     * 
     * @return
     */
    protected DojoQueryCallback<T> getOnResultCallback() {
        return new DojoQueryCallback<>(false); //Don't keep results
    }


    public ResultsWithTotal dojoQuery(Map<String, String> query, List<SortOption> sort, Integer offset, Integer limit,
            boolean or) {
        return dojoQuery(SELECT_ALL, COUNT, query, sort, offset, limit, or, getOnResultCallback(),
                getOnFilterCallback());
    }


    public void exportQuery(Map<String, String> query, List<SortOption> sort, Integer offset, Integer limit,
            boolean or, DojoQueryCallback<T> onResultCallback) {
        dojoQuery(SELECT_ALL, COUNT, query, sort, offset, limit, or, onResultCallback, null);
    }

    
    protected ResultsWithTotal dojoQuery(
            String selectSql, String countSql,
            Map<String, String> query, List<SortOption> sort, Integer offset, 
            Integer limit, boolean or, 
            DojoQueryCallback<T> onKeepCallback, DojoQueryCallback<T> onFilterCallback) {
        List<Object> selectArgs = new ArrayList<Object>();
        List<Object> countArgs = new ArrayList<Object>();
      
        String conditions = applyConditions("", selectArgs, query, or);
        countArgs.addAll(selectArgs);
        selectSql += conditions;
        countSql += conditions;
        
        selectSql = applySort(selectSql, sort, selectArgs);
        selectSql = applyRange(selectSql, selectArgs, offset, limit);
        if((LOG !=null)&&(LOG.isDebugEnabled())){
        	LOG.debug("Dojo Query: " + selectSql + " \nArgs: " + selectArgs.toString());
        }
        
        FilterListCallback<T> filter = new FilterListCallback<T>(createFilters(query), onKeepCallback, onFilterCallback);
        //FilterListCallback<T> filter = new FilterListCallback<T>(createFilters(query),createComparatorChain(sort));
      
        //query(selectSql, selectArgs.toArray(), getRowMapper(),filter);
        List<T> results = query(selectSql, selectArgs.toArray(), getResultSetExtractor(getRowMapper(), filter));
        //List<T> results = query(selectSql, selectArgs.toArray(), getRowMapper());
        //TODO modify this ...
        //filter.orderResults();
        //List<T> results = filter.getResults();

        // TODO work out how to do this in one transaction
		int count = ejt.queryForInt(countSql, countArgs.toArray(), 0);
        if((LOG !=null)&&(LOG.isDebugEnabled()))
        	LOG.debug("DB Has: " + count);
        
        //No results, this will mess up the dojo store we need to keep searching
        // until we are sure there are none in the DB or we have found some
//        if(results.size() == 0){
//        	 if(LOG !=null)
//             	LOG.info("All Filtered! ");
//        }

        
        
        //Do Filtering for more complex members that may not be mapped properties
        //int removed = filterComplexMembers(results,query);
        //count = count - removed;
        if(onFilterCallback != null)
        	count = count - onFilterCallback.getResultCount();
        if((LOG !=null)&&(LOG.isDebugEnabled()))
        	LOG.debug("After filter: " + count);
        
        //Sort the remaining list
        //TODO This doesn't work exactly right
        // because we need the whole data set to order properly and we are 
        // only able to sort the data that was returned by the query here
        // this is the best we can do for now
        sortComplexMembers(results,sort);
        
        
        return new ResultsWithTotal(results, count);
    }
 
	/**
	 * Sort on any members that are not directly accessible via the database query
	 * 
	 * Members must be mapped to a comparator in the comparatorMap
	 * 
	 * @param results
	 * @param sort
	 */
	@SuppressWarnings("unchecked")
	private void sortComplexMembers(List<T> results, List<SortOption> sort) {
		
		ComparatorChain chain = this.createComparatorChain(sort);
		
		//Do the sort if we added at least one comparator
		if(chain.size()>0)
			Collections.sort(results,chain);
	}
    /**
     * TODO This needs to be reworked to use the args list
     * to avoid SQL Injection Attacks
     * @param sql
     * @param args
     * @param query
     * @param or
     * @return
     */
    protected String applyConditions(String sql, List<Object> args, Map<String, String> query, boolean or) {
        if (query != null && !query.isEmpty()) {
            int i = 0;
            
            Set<String> properties = this.propertyTypeMap.keySet();
            for (String prop : query.keySet()) {
                boolean mapped = false;
                String dbProp = prop;
                //Don't allow filtering on properties with a filter
                //this will be done after the query
                if(!filterMap.containsKey(prop)){ 
	                if (propertiesMap.containsKey(prop)) {
	                	IntStringPair pair = propertiesMap.get(prop);
	                	dbProp = pair.getValue();
	                    mapped = true;
	                }
	                
	                if (mapped || properties.contains(prop)) {
	                    String tempSql = (i == 0) ? WHERE : (or ? OR : AND);
	                    
	                    String condition = query.get(prop);
	                    if (condition.startsWith("RegExp:")) {
	                        condition = condition.substring(7, condition.length());
	                        // simple RegExp handling
	                        if (condition.startsWith("^") && condition.endsWith("$")) {
	                            condition = condition.substring(1, condition.length() - 1);
	                            condition = condition.replace(".*.*", "%");
	                            condition = condition.replace(".*", "%");
	                            //Derby doesn't handle LIKE for anything but varchars
	                            switch (Common.databaseProxy.getType()) {
	                            case MYSQL:
	                            case POSTGRES:
	                            case MSSQL:
	                            case H2:
	                            	if(mapped)
	                            		tempSql += "lower(" + dbProp + ") LIKE '" + condition.toLowerCase() + "'";
	                            	else
	                            		tempSql += "lower(" + this.tablePrefix + dbProp + ") LIKE '" + condition.toLowerCase() + "'";
		                            break;
	                            case DERBY:
		                            if(mapped)
		                            	tempSql += "(CHAR(" + dbProp + ") LIKE '" + condition + "')";
		                            else
		                            	tempSql += "(CHAR(" + this.tablePrefix + dbProp + ") LIKE '" + condition + "')";
	                            	break;
	                            default:
	                            	LOG.warn("No case for converting regex expressing for database of type: " + Common.databaseProxy.getType());

	                            }
	                        }
	                        else {
	                            // all other cases, add condition which will ensure no results are returned
	                            tempSql += this.tablePrefix + "id = '-1'";
	                        }
	                    }else if(condition.startsWith("Int:")){
	                    	//Parse the value as Int:operatorvalue - Int:>10000 OR Int:>=
	                    	int endAt = 5;
	                    	if(condition.charAt(5) == '=')
	                    		endAt = 6;
	                    	String value = condition.substring(endAt,condition.length());
	                    	String compare = condition.substring(4, endAt);
	                    	if(mapped)
	                    		tempSql += dbProp + " " + compare + " " + value;
	                    	else
	                    		tempSql += this.tablePrefix + dbProp + " " + compare + " " + value;
	                    }else if(condition.startsWith("Long:")){
	                    	//Parse the value as Long:operatorvalue - Long:>10000
	                    	String ms = condition.substring(6,condition.length());
	                    	String compare = condition.substring(5, 6);
	                    	if(mapped)
	                    		tempSql += dbProp + " " + compare + " " + ms;
	                    	else
	                    		tempSql += this.tablePrefix + dbProp + " " + compare + " " + ms;
	                    }else if(condition.startsWith("LongRange:")){
	                    	//Parse the value as LongRange:>startValue:<EndValue
	                    	String[] parts = condition.split(":");
	                    	String startCompare = parts[1].substring(0,1);
	                    	String startMs = parts[1].substring(1,parts[1].length());
	                    	String endCompare = parts[2].substring(0,1);
	                    	String endMs = parts[2].substring(1,parts[2].length());	                    	
	                    	if(mapped)
	                    		tempSql += dbProp + startCompare + startMs + " AND " + dbProp + endCompare + endMs;
	                    	else
	                       		tempSql += this.tablePrefix + dbProp + startCompare + startMs + " AND " + this.tablePrefix + dbProp + endCompare + endMs;
	       	                    	
	                    }else if(condition.startsWith("Duration:")){
	                    	//Parse the value as Duration:operatorvalue - Duration:>1:00:00
	                    	String durationString = condition.substring(10,condition.length());
	                    	String compare = condition.substring(9, 10);
	                    	Long longValue = DeltamationCommon.unformatDuration(durationString);
	                    	if(mapped)
	                    		tempSql += dbProp + " " + compare + " " + longValue;
	                    	else
	                    		tempSql += this.tablePrefix + dbProp + " " + compare + " " + longValue;
	                    	
	                    }else if(condition.startsWith("BooleanIs:")){
	                    	//Parse the value as BooleanIs:value
	                    	String booleanString = condition.substring(10,condition.length());
	                    	//Boolean value = Boolean.parseBoolean(booleanString);
	                    	if(mapped)
	                    		tempSql += dbProp + " IS " + booleanString;
	                    	else
	                    		tempSql += this.tablePrefix + dbProp + " = " + booleanString;
	                    	
	                    }else if(condition.startsWith("NullCheck:")){
	                    	//Parse the value as NullCheck:true or NullCheck:false
	                    	String checkForString = condition.substring(10,condition.length());
	                    	Boolean checkFor = Boolean.parseBoolean(checkForString);
	                    	if(checkFor){
		                    	if(mapped)
		                    		tempSql += dbProp + " IS NULL";
		                    	else
		                    		tempSql += this.tablePrefix + dbProp + " IS NULL";
	                    	}else{
		                    	if(mapped)
		                    		tempSql += dbProp + " IS NOT NULL";
		                    	else
		                    		tempSql += this.tablePrefix + dbProp + " IS NOT NULL";
	                    	}
	                    }
	                    else {
	                        //if (condition.isEmpty()) // occurs when empty array is set in query
	                        //    continue;
	                        
	                        String[] parts = condition.split(",");
	                        String qMarks = "";
	                        for (int j = 0; j < parts.length; j++) {
	                            args.add(parts[j]);
	                            qMarks += j == 0 ? "?" : ",?";
	                        }
	                        // TODO not sure if IN will work with string values
	                        if(mapped)
	                        	tempSql += dbProp + " IN (" + qMarks + ")";
	                        else
	                        	tempSql += this.tablePrefix + dbProp + " IN (" + qMarks + ")";
	                    }
	                    sql += tempSql;
	                    i++;
	                }
                }//end if in filter map
            }
        }
        return sql;
    }
    
    
    /**
     * Apply The Sort to the Query
     * @param sql
     * @param sort
     * @param selectArgs
     * @return
     */
    protected String applySort(String sql, List<SortOption> sort, List<Object> selectArgs) {
        // always sort so that the offset/limit work as intended
        if (sort == null)
            sort = new ArrayList<SortOption>();
        if (sort.isEmpty())
            sort.add(new SortOption("id", false));
        
        int i = 0;
        Set<String> properties = this.propertyTypeMap.keySet();
        for (SortOption option : sort) {
            String prop = option.getAttribute();
            boolean mapped = false;
            if(!comparatorMap.containsKey(prop)){ //Don't allow sorting on values that have a comparator
	            if (propertiesMap.containsKey(prop)) {
	            	IntStringPair pair = propertiesMap.get(prop);
	            	prop = pair.getValue();
	                PropertyArguments args = propertyArgumentsMap.get(option.getAttribute());
	                if(args != null){
	                	Collections.addAll(selectArgs, args.getArguments());
	                }
	                mapped = true;
	            }
	            
	            if (mapped || properties.contains(prop)) {
	                sql += i++ == 0 ? " ORDER BY " : ", ";
	                if(mapped)
	                	sql += prop;
	                else
	                	sql += this.tablePrefix + prop;
	                if (option.isDesc()) {
	                    sql += " DESC";
	                }
	            }
	        }
        }
        return sql;
    }
    
    


    
    /**
     * 
     * Overridable method to extract the data
     * 
	 * @return
	 */
	public ResultSetExtractor<List<T>> getResultSetExtractor(final RowMapper<T> rowMapper, final FilterListCallback<T> filters) {

		return new ResultSetExtractor<List<T>>(){
			List<T> results = new ArrayList<T>();
			int rowNum = 0;
			@Override
			public List<T> extractData(ResultSet rs)
					throws SQLException, DataAccessException {
				while (rs.next()){
						T row = rowMapper.mapRow(rs, rowNum);
						//Should we filter the row?
						if(!filters.filterRow(row, rowNum++))
							results.add(row);
				}
				return results;
			
			}
		};
		
	}

	/**
     * Create a list of filters for the complex members
     * @param query
     * @return
     */
    protected List<IFilter<T>> createFilters(Map<String, String> query){
    	
    	List<IFilter<T>> filters = new ArrayList<IFilter<T>>();
    	
    	
		for (String prop : query.keySet()) {
			 if(filterMap.containsKey(prop)){
				IFilter<T> filter = filterMap.get(prop);
				
	            String condition = query.get(prop);
	            if (condition.startsWith("RegExp:")) {
	                condition = condition.substring(7, condition.length());
	                // simple RegExp handling
	                if (condition.startsWith("^") && condition.endsWith("$")) {
	                    condition = condition.substring(1, condition.length() - 1);
	                    filter.setFilter(condition);
	                }
	                filters.add(filter); //Save for use later
	             } //end if is regex
			 }//end if in filterMap
		 }
    	
    	return filters;
    }
    



	protected ComparatorChain createComparatorChain(List<SortOption> sort){
		
		ComparatorChain chain = new ComparatorChain();
		if(sort != null)
			for (SortOption option : sort) {
	            String prop = option.getAttribute();
	            if(comparatorMap.containsKey(prop)){
	            	chain.addComparator(comparatorMap.get(prop),option.isDesc());
	            }
			}
		
		return chain;
	}
}
