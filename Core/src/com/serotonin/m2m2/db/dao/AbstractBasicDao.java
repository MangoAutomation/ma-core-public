/*
 *  Copyright (C) 2013 Deltamation Software. All rights reserved.
 *  @author Jared Wiltshire
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import com.infiniteautomation.mango.db.query.BaseSqlQuery;
import com.infiniteautomation.mango.db.query.Index;
import com.infiniteautomation.mango.db.query.JoinClause;
import com.infiniteautomation.mango.db.query.QueryAttribute;
import com.infiniteautomation.mango.db.query.RQLToSQLSelect;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;
import com.infiniteautomation.mango.db.query.SQLStatement;
import com.infiniteautomation.mango.db.query.SQLSubQuery;
import com.infiniteautomation.mango.db.query.StreamableRowCallback;
import com.infiniteautomation.mango.db.query.StreamableSqlQuery;
import com.infiniteautomation.mango.db.query.TableModel;
import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy.DatabaseType;
import com.serotonin.m2m2.module.WebSocketDefinition;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler;

import net.jazdw.rql.parser.ASTNode;

/**
 * Provides an API to retrieve, update and save VO objects from and to the
 * database.
 * 
 * Copyright (C) 2013 Deltamation Software. All Rights Reserved.
 * 
 * @author Jared Wiltshire
 */
public abstract class AbstractBasicDao<T extends AbstractBasicVO> extends BaseDao {
	protected Log LOG = LogFactory.getLog(AbstractBasicDao.class);

	protected DaoNotificationWebSocketHandler<T> handler;

	public static final String WHERE = " WHERE ";
	public static final String OR = " OR ";
	public static final String AND = " AND ";
	public static final String LIMIT = " LIMIT ";

	// Map UI or Model member names to the Database Column Names they will get
	// translated when the query is generated
	protected final Map<String, IntStringPair> propertiesMap;

	// Map of Database Column Names to Column SQL Type
	protected final LinkedHashMap<String, Integer> propertyTypeMap;

	// List of our Joins
	protected final List<JoinClause> joins;

	// List of our Indexes
	protected final List<Index> indexes;


	/*
	 * SQL templates
	 */
	protected final String TABLE_PREFIX; // Without ending .
	protected final String SELECT_ALL_BASE; // Without location of FROM
	protected final String SELECT_ALL;
	protected final String SELECT_ALL_SORT;
	protected final String SELECT_ALL_FIXED_SORT;
	protected final String SELECT_BY_ID;
	protected final String SELECT_BY_XID;
	protected final String SELECT_BY_NAME;
	protected final String INSERT;
	protected final String UPDATE;
	protected final String DELETE;
	protected final String COUNT_BASE;
	protected final String COUNT;
	// protected final String EXTRA_SQL;

	public final String tableName;

	protected int[] updateStatementPropertyTypes; // Required for Derby LOBs
	protected int[] insertStatementPropertyTypes; // Required for Derby LOBs

	protected TableModel tableModel;

	public final String tablePrefix; // Select * from table as tablePrefix

	// Use SubQuery for restrictions not in Joined Tables
	protected final boolean useSubQuery;
	// Print out times and SQL for RQL Queries
	protected final boolean useMetrics;
	// The type of database we are using
	protected final DatabaseType databaseType;

	/**
	 * Override as necessary Can be null if no Pk Exists
	 * 
	 * @return String name of Pk Column
	 */
	public String getPkColumnName() {
		return "id";
	}

	public AbstractBasicDao(DaoNotificationWebSocketHandler<T> handler, String tablePrefix, String[] extraProperties) {
		this(handler, tablePrefix, extraProperties, false);
	}

	@SuppressWarnings("unchecked")
	public AbstractBasicDao(WebSocketDefinition def, String tablePrefix, String[] extraProperties) {
		this((DaoNotificationWebSocketHandler<T>) (def != null ? def.getHandler() : null), tablePrefix, extraProperties,
				false);
	}

	/**
	 * Provide a table prefix to use for complex queries. Ie. Joins Do not
	 * include the . at the end of the prefix
	 * 
	 * @param tablePrefix
	 */
	public AbstractBasicDao(DaoNotificationWebSocketHandler<T> handler, String tablePrefix, String[] extraProperties,
			boolean useSubQuery) {
		this.handler = handler;
		this.useMetrics = Common.envProps.getBoolean("db.useMetrics", false);
		this.databaseType = Common.databaseProxy.getType();
		TABLE_PREFIX = tablePrefix;
		if (tablePrefix != null)
			this.tablePrefix = tablePrefix + ".";
		else
			this.tablePrefix = "";

		this.tableName = getTableName();
		this.useSubQuery = useSubQuery;

		Map<String, IntStringPair> propMap = getPropertiesMap();
		if (propMap == null)
			this.propertiesMap = new HashMap<String, IntStringPair>();
		else
			this.propertiesMap = propMap;

		LinkedHashMap<String, Integer> propTypeMap = getPropertyTypeMap();
		if (propTypeMap == null)
			throw new ShouldNeverHappenException("Property Type Map is required!");
		else
			this.propertyTypeMap = propTypeMap;

		// generate SQL statements
		String selectAll = "SELECT ";
		String insert = "INSERT INTO " + tableName + " (";
		String insertValues = "";
		String update = "UPDATE " + tableName + " SET ";

		// Map of properties to their QueryAttribute
		Map<String, QueryAttribute> attributeMap = new HashMap<String, QueryAttribute>();

		Set<String> properties = this.propertyTypeMap.keySet();
		// don't the first property - "id", in the insert statements
		int i = 0;
		for (String prop : properties) {

			// Add this attribute
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

		if (extraProperties != null)
			for (String prop : extraProperties) {
				selectAll += "," + prop;
			}

		// Setup the Joins
		this.joins = getJoins();
		String joinSql = null;
		if (!this.joins.isEmpty()) {
			StringBuilder joinSqlBuilder = new StringBuilder();
			for (JoinClause join : this.joins) {
				joinSqlBuilder.append(join.toString());
			}
			joinSql = joinSqlBuilder.toString();
		}

		// Add the table prefix to the queries if necessary
		SELECT_ALL_BASE = selectAll + " FROM ";
		String pkColumn = getPkColumnName();

		if (this.tablePrefix.equals("")) {
			if (StringUtils.isEmpty(pkColumn))
				COUNT_BASE = "SELECT COUNT(*) FROM ";
			else
				COUNT_BASE = "SELECT COUNT(DISTINCT " + pkColumn + ") FROM ";

			if (joinSql != null)
				SELECT_ALL = selectAll + " FROM " + tableName + joinSql;
			else
				SELECT_ALL = selectAll + " FROM " + tableName;

			SELECT_ALL_SORT = SELECT_ALL + " ORDER BY ";
			if (properties.contains("name")) {
				SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY name ASC";
			} else {
				SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY id ASC";
			}

			SELECT_BY_ID = SELECT_ALL + " WHERE id=?";
			SELECT_BY_XID = SELECT_ALL + " WHERE xid=?";
			SELECT_BY_NAME = SELECT_ALL + " WHERE name=?";
			INSERT = insert + ") VALUES (" + insertValues + ")";
			UPDATE = update + " WHERE id=?";
			DELETE = "DELETE FROM " + tableName + " WHERE id=?";

			if (joinSql != null) {
				if (StringUtils.isEmpty(pkColumn))
					COUNT = "SELECT COUNT(*) FROM " + tableName + joinSql;
				else
					COUNT = "SELECT COUNT(DISTINCT " + pkColumn + ") FROM " + tableName + joins;
			} else {
				if (StringUtils.isEmpty(pkColumn))
					COUNT = "SELECT COUNT(*) FROM " + tableName;
				else
					COUNT = "SELECT COUNT(DISTINCT " + pkColumn + ") FROM " + tableName;
			}

		} else {
			// this.tablePrefix will end in a . where the local tablePrefix
			// shouldn't
			if (StringUtils.isEmpty(pkColumn))
				COUNT_BASE = "SELECT COUNT(*) FROM ";
			else
				COUNT_BASE = "SELECT COUNT(DISTINCT " + this.tablePrefix + pkColumn + ") FROM ";

			if (joinSql != null)
				SELECT_ALL = selectAll + " FROM " + tableName + " AS " + tablePrefix + joinSql;
			else
				SELECT_ALL = selectAll + " FROM " + tableName + " AS " + tablePrefix;

			SELECT_ALL_SORT = SELECT_ALL + " ORDER BY ";
			if (properties.contains("name")) {
				SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY " + this.tablePrefix + "name ASC";
			} else {
				SELECT_ALL_FIXED_SORT = SELECT_ALL + " ORDER BY " + this.tablePrefix + "id ASC";
			}

			SELECT_BY_ID = SELECT_ALL + " WHERE " + this.tablePrefix + "id=?";
			SELECT_BY_XID = SELECT_ALL + " WHERE " + this.tablePrefix + "xid=?";
			SELECT_BY_NAME = SELECT_ALL + " WHERE " + this.tablePrefix + "name=?";
			INSERT = insert + ") VALUES (" + insertValues + ")";
			UPDATE = update + " WHERE id=?";
			DELETE = "DELETE FROM " + tableName + " WHERE id=?";
			if (joinSql != null) {
				if (StringUtils.isEmpty(pkColumn))
					COUNT = "SELECT COUNT(*) FROM " + tableName + " AS " + tablePrefix + joinSql;
				else
					COUNT = "SELECT COUNT(DISTINCT " + this.tablePrefix + pkColumn + ") FROM " + tableName + " AS "
							+ tablePrefix + joinSql;
			} else {
				if (StringUtils.isEmpty(pkColumn))
					COUNT = "SELECT COUNT(*) FROM " + tableName + " AS " + tablePrefix;
				else
					COUNT = "SELECT COUNT(DISTINCT " + this.tablePrefix + pkColumn + ") FROM " + tableName + " AS "
							+ tablePrefix;
			}
		}

		// Create the Update and Insert property types lists
		if ((getPkColumnName() != null) && (this.propertyTypeMap.get(getPkColumnName()) != null)) {
			this.updateStatementPropertyTypes = new int[this.propertyTypeMap.size()];
			this.insertStatementPropertyTypes = new int[this.propertyTypeMap.size() - 1];
		} else {
			this.updateStatementPropertyTypes = new int[this.propertyTypeMap.size()];
			this.insertStatementPropertyTypes = new int[this.propertyTypeMap.size()];
		}

		Iterator<String> it = this.propertyTypeMap.keySet().iterator();
		int j = 0;
		while (it.hasNext()) {
			String property = it.next();
			if (!property.equals(getPkColumnName())) {
				Integer type = this.propertyTypeMap.get(property);
				this.updateStatementPropertyTypes[j] = type;
				this.insertStatementPropertyTypes[j] = type;
				j++;
			}
		}

		if ((getPkColumnName() != null) && (this.propertyTypeMap.get(getPkColumnName()) != null)) {
			Integer pkType = this.propertyTypeMap.get(getPkColumnName());
			this.updateStatementPropertyTypes[j] = pkType;
			attributeMap.put(getPkColumnName(), new QueryAttribute(getPkColumnName(), new HashSet<String>(), pkType));
		}

		Iterator<String> propertyMapIterator = this.propertiesMap.keySet().iterator();
		while (propertyMapIterator.hasNext()) {
			String propertyName = propertyMapIterator.next();
			IntStringPair pair = this.propertiesMap.get(propertyName);

			QueryAttribute attribute = attributeMap.get(pair.getValue());
			if (attribute != null) {
				attribute.addAlias(propertyName);
			} else {
				QueryAttribute newAttribute = new QueryAttribute();
				newAttribute.setColumnName(pair.getValue());
				newAttribute.setSqlType(pair.getKey());
				newAttribute.addAlias(propertyName);
				attributeMap.put(propertyName, newAttribute);
			}
		}

		// Collect the indexes
		this.indexes = getIndexes();

		// Create the model
		this.tableModel = new TableModel(this.getTableName(), new ArrayList<QueryAttribute>(attributeMap.values()));

	}

	/**
	 * Gets the table name that the Dao operates on
	 * 
	 * @return table name
	 */
	protected abstract String getTableName();

	/**
	 * Converts a VO object into an array of objects for insertion or updating
	 * of database
	 * 
	 * @param vo
	 *            to convert
	 * @return object array
	 */
	protected abstract Object[] voToObjectArray(T vo);

	/**
	 * Both the properties and the type maps must be setup
	 * 
	 * @return
	 */
	protected abstract LinkedHashMap<String, Integer> getPropertyTypeMap();

	/**
	 * Returns a map which maps a virtual property to a real one used for
	 * sorting/filtering from the database e.g. dateFormatted -> timestamp
	 * 
	 * @return map of properties
	 */
	protected abstract Map<String, IntStringPair> getPropertiesMap();

	/**
	 * Gets the row mapper for converting the retrieved database values into a
	 * VO object
	 * 
	 * @return row mapper
	 */
	public abstract RowMapper<T> getRowMapper();

	/**
	 * Get a map of the Joined tables with table prefix --> Join SQL To be
	 * overridden as necessary
	 * 
	 * @return
	 */
	protected List<JoinClause> getJoins() {
		return new ArrayList<JoinClause>();
	}

	/**
	 * Optionally return a list of Indexes for this table for Query optimization
	 * 
	 * @return
	 */
	protected List<Index> getIndexes() {
		return new ArrayList<Index>();
	}

	public void delete(int id) {
		T vo = get(id);
		if (vo != null) {
			ejt.update(DELETE, vo.getId());
		}
		handler.notify("delete", vo, null);
	}

	public void delete(int id, String initiatorId) {
		T vo = get(id);
		if (vo != null) {
			ejt.update(DELETE, vo.getId());
		}
		handler.notify("delete", vo, initiatorId);
	}

	public void delete(T vo) {
		if (vo != null) {
			ejt.update(DELETE, vo.getId());
		}
		handler.notify("delete", vo, null);
	}

	public void delete(T vo, String initiatorId) {
		if (vo != null) {
			ejt.update(DELETE, vo.getId());
		}
		handler.notify("delete", vo, initiatorId);
	}

	/**
	 * Persist the vo or if it already exists update it
	 * 
	 * @param vo
	 *            to save
	 */
	public void save(T vo) {
		if (vo.getId() == Common.NEW_ID) {
			insert(vo);
		} else {
			update(vo);
		}
	}

	/**
	 * 
	 * @param vo
	 * @param initiatorId
	 */
	public void save(T vo, String initiatorId) {
		if (vo.getId() == Common.NEW_ID) {
			insert(vo, initiatorId);
		} else {
			update(vo, initiatorId);
		}
	}

	/**
	 * Insert a new vo and assign the ID
	 * 
	 * @param vo
	 *            to insert
	 */
	protected void insert(T vo) {
		int id = -1;
		if (insertStatementPropertyTypes == null)
			id = ejt.doInsert(INSERT, voToObjectArray(vo));
		else
			id = ejt.doInsert(INSERT, voToObjectArray(vo), insertStatementPropertyTypes);
		vo.setId(id);
		handler.notify("add", vo, null);
	}

	protected void insert(T vo, String initiatorId) {
		int id = -1;
		if (insertStatementPropertyTypes == null)
			id = ejt.doInsert(INSERT, voToObjectArray(vo));
		else
			id = ejt.doInsert(INSERT, voToObjectArray(vo), insertStatementPropertyTypes);
		vo.setId(id);
		handler.notify("add", vo, initiatorId);
	}

	/**
	 * Update a vo
	 *
	 * @param vo
	 *            to update
	 */
	protected void update(T vo) {
		List<Object> list = new ArrayList<>();
		list.addAll(Arrays.asList(voToObjectArray(vo)));
		list.add(vo.getId());

		if (updateStatementPropertyTypes == null)
			ejt.update(UPDATE, list.toArray());
		else
			ejt.update(UPDATE, list.toArray(), updateStatementPropertyTypes);

		handler.notify("update", vo, null);
	}

	/**
	 * 
	 * @param vo
	 * @param initiatorId
	 */
	protected void update(T vo, String initiatorId) {
		List<Object> list = new ArrayList<>();
		list.addAll(Arrays.asList(voToObjectArray(vo)));
		list.add(vo.getId());

		if (updateStatementPropertyTypes == null)
			ejt.update(UPDATE, list.toArray());
		else
			ejt.update(UPDATE, list.toArray(), updateStatementPropertyTypes);

		handler.notify("update", vo, initiatorId);
	}

	/**
	 * Return a VO with FKs populated
	 * 
	 * @param id
	 * @return
	 */
	public T getFull(int id) {
		return get(id);
	}

	/**
	 * Get By ID
	 * 
	 * @param id
	 * @return
	 */
	public T get(int id) {
		return queryForObject(SELECT_BY_ID, new Object[] { id }, getRowMapper(), null);
	}

	/**
	 * Get All from table
	 * 
	 * @return
	 */
	public List<T> getAll() {
		return query(SELECT_ALL_FIXED_SORT, getRowMapper());
	}

	/**
	 * Count all from table
	 * 
	 * @return
	 */
	public int count() {
		return ejt.queryForInt(COUNT, new Object[0], 0);
	}

	/**
	 * Return all VOs with FKs Populated
	 * 
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
	 * @param selectCallback
	 * @param countCallback
	 * @param modelMap
	 * @param modifiers
	 * @param applyLimitToSelectSql
	 * @return
	 */
	public StreamableSqlQuery<T> createQuery(ASTNode root, StreamableRowCallback<T> selectCallback,
			StreamableRowCallback<Long> countCallback, Map<String, String> modelMap,
			Map<String, SQLColumnQueryAppender> modifiers, boolean applyLimitToSelectSql) {

		SQLStatement statement;
		if (useSubQuery) {
			statement = new SQLSubQuery(SELECT_ALL_BASE, COUNT_BASE, joins, getTableName(), TABLE_PREFIX,
					applyLimitToSelectSql, Common.envProps.getBoolean("db.forceUseIndex", false), null, this.indexes, this.databaseType);
		} else {
			statement = new SQLStatement(SELECT_ALL_BASE, COUNT_BASE, joins, getTableName(), TABLE_PREFIX,
					applyLimitToSelectSql, Common.envProps.getBoolean("db.forceUseIndex", false), this.indexes, this.databaseType);
		}
		if (root != null)
			root.accept(new RQLToSQLSelect<T>(this, modelMap, modifiers), statement);

		statement.build();
		return new StreamableSqlQuery<T>(this, Common.envProps.getBoolean("db.stream", false), statement, selectCallback, countCallback);
	}

	/**
	 * 
	 * @param root
	 * @param applyLimitToSelectSql
	 * @return
	 */
	public BaseSqlQuery<T> createQuery(ASTNode root, boolean applyLimitToSelectSql) {

		SQLStatement statement = new SQLStatement(SELECT_ALL_BASE, COUNT_BASE, joins, getTableName(), TABLE_PREFIX,
				applyLimitToSelectSql, Common.envProps.getBoolean("db.forceUseIndex", false), this.indexes, this.databaseType);
		if (root != null)
			root.accept(new RQLToSQLSelect<T>(this), statement);

		statement.build();
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

		Integer sqlType;
		if (propertiesMap.containsKey(prop)) {
			IntStringPair pair = propertiesMap.get(prop);
			dbCol = pair.getValue();
			sqlType = pair.getKey();
			mapped = true;
		} else {
			sqlType = this.propertyTypeMap.get(dbCol);
		}

		if (mapped || properties.contains(dbCol)) {
			if (mapped)
				return new SQLQueryColumn(dbCol, sqlType);
			else
				return new SQLQueryColumn(this.tablePrefix + dbCol, sqlType);
		}

		// No Column matches...
		throw new ShouldNeverHappenException("No column found for: " + prop);
	}

	/**
	 * Helper to prepare a statement
	 * 
	 * @param sql
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	public PreparedStatement createPreparedStatement(String sql, List<Object> args, boolean stream) throws SQLException {
		
		PreparedStatement stmt;
		if(stream){
			if(this.databaseType == DatabaseType.MYSQL){
				stmt = this.dataSource.getConnection().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				stmt.setFetchSize(Integer.MIN_VALUE);
			}else{
				//TODO Choose settings for other types to stream
				stmt = this.dataSource.getConnection().prepareStatement(sql);
			}
		}else{
			stmt = this.dataSource.getConnection().prepareStatement(sql);
			int fetchSize = Common.envProps.getInt("db.fetchSize", -1);
			if(fetchSize > 0)
				stmt.setFetchSize(fetchSize);
		}


		
		int index = 1;
		for (Object o : args) {
			stmt.setObject(index, o);
			index++;
		}

		return stmt;
	}

	public boolean isUseMetrics() {
		return this.useMetrics;
	}
}
