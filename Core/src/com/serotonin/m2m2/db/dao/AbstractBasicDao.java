/**
 * Copyright (C) 2016  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.SelectSelectStep;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.exception.NoDataFoundException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.monitor.AtomicIntegerMonitor;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.util.RQLUtils;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.log.LogStopWatch;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import net.jazdw.rql.parser.ASTNode;

/**
 * Provides an API to retrieve, update and save VO objects from and to the
 * database.
 *
 * @author Jared Wiltshire
 * @author Terry Packer
 */
public abstract class AbstractBasicDao<T extends AbstractBasicVO, R extends Record, TABLE extends Table<R>> extends BaseDao implements AbstractBasicVOAccess<T> {
    protected Log LOG = LogFactory.getLog(AbstractBasicDao.class);

    //Retry transactions that deadlock
    //TODO Mango 4.0 make the retry criteria more accurate
    protected final int transactionRetries = Common.envProps.getInt("db.transaction.retries", 5);

    protected final TABLE table;
    protected final ObjectMapper mapper;
    protected final ApplicationEventPublisher eventPublisher;

    //Monitor for count of table
    protected final AtomicIntegerMonitor countMonitor;

    protected final Map<String, Field<?>> aliasMap;
    protected final Map<String, RQLSubSelectCondition> subSelectMap;
    protected final Map<String, Function<Object, Object>> valueConverterMap;

    public AbstractBasicDao(TABLE table, ObjectMapper mapper, ApplicationEventPublisher publisher) {
        this(table, null, mapper, publisher);
    }

    /**
     * @param table - table definition
     * @param countMonitorName - If not null create a monitor to track table row count
     * @param mapper
     * @param publisher
     */
    public AbstractBasicDao(TABLE table,
            TranslatableMessage countMonitorName,
            ObjectMapper mapper, ApplicationEventPublisher publisher) {

        this.table = table;
        this.mapper = mapper;
        this.eventPublisher = publisher;

        // Map of potential property names to db field aliases
        this.aliasMap = createAliasMap();

        // Map of potential property names to sub select conditions
        this.subSelectMap = createSubSelectMap();

        // Map of properties to their QueryAttribute
        this.valueConverterMap = createValueConverterMap();

        //Setup Monitors
        if(countMonitorName != null) {
            this.countMonitor = Common.MONITORED_VALUES.create(this.getClass().getCanonicalName() + ".COUNT")
                    .name(countMonitorName)
                    .value(this.count())
                    .uploadToStore(true)
                    .buildAtomic();
        }else{
            this.countMonitor = null;
        }
    }

    /**
     * Converts a VO object into a map of fields for insert/update of DB.
     *
     * @param vo
     *            to convert
     * @return object array
     */
    protected abstract Record voToObjectArray(T vo);

    /**
     * Condition required for user to have read permission.  Override as required, note
     *  that when overriding the user can and will sometimes be null
     * @param user - reading user (can be null)
     * @return
     */
    protected Condition hasReadPermission(PermissionHolder user) {
        return DSL.trueCondition();
    }

    /**
     * Gets the row mapper for converting the retrieved database values into a
     * VO object
     *
     * @return row mapper
     */
    public RowMapper<T> getRowMapper() {
        return this::mapRow;
    }

    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        Record record = create.fetchLazy(rs).fetchNext();
        return mapRecord(record);
    }

    public abstract T mapRecord(Record record);

    @Override
    public boolean delete(int id) {
        return delete(get(id));
    }

    @Override
    public boolean delete(T vo) {
        if (vo != null) {
            int deleted = 0;
            int tries = transactionRetries;
            while(tries > 0) {
                try {
                    deleted = withLockedRow(vo.getId(), (txStatus) -> {
                        deleteRelationalData(vo);
                        int result = create.deleteFrom(table).where(getIdField().eq(vo.getId())).execute();
                        if(result > 0) {
                            deletePostRelationalData(vo);
                        }
                        return result;
                    });
                    break;
                }catch(org.jooq.exception.DataAccessException | ConcurrencyFailureException e) {
                    if(tries == 1) {
                        throw e;
                    }
                }
                tries--;
            }

            if(this.countMonitor != null) {
                this.countMonitor.addValue(-deleted);
            }

            if(deleted > 0) {
                this.publishEvent(createDaoEvent(DaoEventType.DELETE, vo, null));
            }

            return deleted > 0;
        }
        return false;
    }

    public Field<Integer> getIdField() {
        Identity<? extends Record, ?> identity = table.getIdentity();
        if (identity != null) {
            TableField<? extends Record, ?> field = identity.getField();
            if (field.getDataType().isNumeric()) {
                return field.cast(Integer.class);
            }
        }
        return null;
    }

    @Override
    public void deleteRelationalData(T vo) { }

    @Override
    public void deletePostRelationalData(T vo) { }

    @Override
    public void insert(T vo) {
        int tries = transactionRetries;
        while(tries > 0) {
            try {
                doInTransaction(status -> {
                    savePreRelationalData(null, vo);

                    int id = create.insertInto(table)
                            .set(voToObjectArray(vo))
                            .returningResult(getIdField())
                            .fetchOptional()
                            .orElseThrow(NoDataFoundException::new)
                            .value1();
                    vo.setId(id);

                    saveRelationalData(null, vo);
                });
                break;
            }catch(org.jooq.exception.DataAccessException | ConcurrencyFailureException e) {
                if(tries == 1) {
                    throw e;
                }
            }
            tries--;
        }

        this.publishEvent(createDaoEvent(DaoEventType.CREATE, vo, null));

        if (this.countMonitor != null)
            this.countMonitor.increment();
    }

    @Override
    public void savePreRelationalData(T existing, T vo) { }

    @Override
    public void saveRelationalData(T existing, T vo) { }

    @Override
    public void update(int id, T vo) {
        update(get(id), vo);
    }

    @Override
    public void update(T existing, T vo) {
        int tries = transactionRetries;
        while(tries > 0) {
            try {
                doInTransaction(status -> {
                    savePreRelationalData(existing, vo);

                    create.update(table).set(voToObjectArray(vo))
                            .where(getIdField().eq(vo.getId()))
                            .execute();

                    saveRelationalData(existing, vo);
                });
                break;
            }catch(org.jooq.exception.DataAccessException | ConcurrencyFailureException e) {
                if(tries == 1) {
                    throw e;
                }
            }
            tries--;
        }

        this.publishEvent(createDaoEvent(DaoEventType.UPDATE, vo, existing));
    }

    @Override
    public T get(int id) {
        Select<Record> query = this.getJoinedSelectQuery()
                .where(getIdField().eq(id))
                .limit(1);
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        T item = ejt.query(sql, args.toArray(new Object[0]), getObjectResultSetExtractor());
        if (item != null) {
            loadRelationalData(item);
        }
        return item;
    }

    @Override
    public void getAll(MappedRowCallback<T> callback) {
        Select<Record> query = this.getJoinedSelectQuery();
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        query(sql, args.toArray(), getCallbackResultSetExtractor((item, index) -> {
            loadRelationalData(item);
            callback.row(item, index);
        }));
    }

    @Override
    public List<T> getAll() {
        List<T> items = new ArrayList<>();
        getAll((item, index) -> {
            items.add(item);
        });
        return items;
    }

    @Override
    public SelectJoinStep<Record> getSelectQuery(List<Field<?>> fields) {
        return create.select(fields)
                .from(table);
    }

    @Override
    public SelectJoinStep<Record> getJoinedSelectQuery() {
        SelectJoinStep<Record> query = getSelectQuery(getSelectFields());
        return joinTables(query, null);
    }

    @Override
    public void loadRelationalData(T vo) { }

    @Override
    public int count() {
        return getCountQuery().from(table)
                .fetchSingle()
                .value1();
    }

    /**
     * Get the base Count query
     * @return
     */
    @Override
    public SelectSelectStep<Record1<Integer>> getCountQuery() {
        if (getIdField() == null) {
            return create.selectCount();
        } else {
            return create.select(DSL.count(getIdField()));
        }
    }

    public AtomicIntegerMonitor getCountMonitor(){
        return this.countMonitor;
    }

    /**
     * Get the select columns, override as necessary
     * @return
     */
    public List<Field<?>> getSelectFields() {
        return Arrays.asList(table.fields());
    }

    public TABLE getTable() {
        return table;
    }

    /**
     * Add any joins including those in the conditions
     *  Override as necessary
     * @param select
     * @return
     */
    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        return select;
    }

    /**
     * Join on permission read conditions to limit results to what the user can 'see'
     */
    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions, PermissionHolder user) {
        return select;
    }

    @Override
    public int customizedCount(ConditionSortLimit conditions, PermissionHolder user) {
        SelectSelectStep<Record1<Integer>> count = getCountQuery();

        SelectJoinStep<Record1<Integer>> select = count.from(table);
        select = joinTables(select, conditions);
        select = joinPermissions(select, conditions, user);
        return customizedCount(select, conditions.getCondition());
    }

    @Override
    public int customizedCount(SelectJoinStep<Record1<Integer>> input, Condition condition) {
        Select<Record1<Integer>> select = input;
        if (condition != null) {
            select = input.where(condition);
        }

        String sql = select.getSQL();
        List<Object> arguments = select.getBindValues();
        Object[] argumentsArray = arguments.toArray(new Object[0]);

        LogStopWatch stopWatch = null;
        if (useMetrics) {
            stopWatch = new LogStopWatch();
        }

        int count = this.ejt.queryForInt(sql, argumentsArray, 0);

        if (stopWatch != null) {
            Select<Record1<Integer>> selectOutput = select;
            stopWatch.stop(() -> "customizedCount(): " + create.renderInlined(selectOutput), metricsThreshold);
        }

        return count;
    }

    @Override
    public void customizedQuery(ConditionSortLimit conditions, PermissionHolder user, MappedRowCallback<T> callback) {
        SelectJoinStep<Record> select = getSelectQuery(getSelectFields());
        select = joinTables(select, conditions);
        select = joinPermissions(select, conditions, user);
        customizedQuery(select, conditions.getCondition(), conditions.getSort(), conditions.getLimit(), conditions.getOffset(), callback);
    }

    @Override
    public void customizedQuery(SelectJoinStep<Record> select, Condition condition, List<SortField<?>> sort, Integer limit, Integer offset,
            MappedRowCallback<T> callback) {
        SelectConnectByStep<Record> afterWhere = condition == null ? select : select.where(condition);

        SelectLimitStep<Record> afterSort = sort == null ? afterWhere : afterWhere.orderBy(sort);

        Select<Record> offsetStep = afterSort;
        if (limit != null) {
            if (offset != null) {
                offsetStep = afterSort.limit(offset, limit);
            } else {
                offsetStep = afterSort.limit(limit);
            }
        }

        customizedQuery(offsetStep, getCallbackResultSetExtractor(callback));
    }

    @Override
    public void customizedQuery(Select<Record> select, MappedRowCallback<T> callback) {
        customizedQuery(select, getCallbackResultSetExtractor(callback));
    }

    @Override
    public <TYPE> TYPE customizedQuery(Select<Record> select, ResultSetExtractor<TYPE> callback) {
        String sql = select.getSQL();
        List<Object> arguments = select.getBindValues();
        Object[] argumentsArray = arguments.toArray(new Object[0]);

        LogStopWatch stopWatch = null;
        if (useMetrics) {
            stopWatch = new LogStopWatch();
        }
        try {
            return this.query(sql, argumentsArray, callback);
        }finally {
            if (stopWatch != null) {
                stopWatch.stop(() -> "customizedQuery(): " + create.renderInlined(select), metricsThreshold);
            }
        }
    }

    protected Map<String, Field<?>> createAliasMap() {
        return Collections.emptyMap();
    }

    protected Map<String, RQLSubSelectCondition> createSubSelectMap() {
        return Collections.emptyMap();
    }

    protected Map<String, Function<Object, Object>> createValueConverterMap() {
        return table.fieldStream().filter(f -> f.getDataType().getSQLDataType() == SQLDataType.CHAR)
                .collect(Collectors.toMap(Field::getName, e -> RQLToCondition.BOOLEAN_VALUE_CONVERTER));
    }

    @Override
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, RQLSubSelectCondition> subSelectMapping, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters) {
        RQLToCondition rqlToCondition = createRqlToCondition(combine(this.subSelectMap, subSelectMapping), combine(this.aliasMap, fieldMap), combine(this.valueConverterMap, valueConverters));
        return rqlToCondition.visit(rql);
    }

    protected <X, Y> Map<X,Y> combine(Map<X,Y> a, Map<X,Y> b) {
        if (b == null || b.isEmpty()) {
            return Collections.unmodifiableMap(a);
        }
        HashMap<X,Y> result = new HashMap<>(a);
        result.putAll(b);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Create a stateful rql to condition (Override as necessary)
     * @param subSelectMapping not null
     * @param fieldMap not null
     * @param converterMap not null
     * @return
     */
    protected RQLToCondition createRqlToCondition(Map<String, RQLSubSelectCondition> subSelectMapping, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> converterMap) {
        return new RQLToCondition(subSelectMapping, fieldMap, converterMap);
    }

    protected DaoEvent<T> createDaoEvent(DaoEventType type, T vo, T existing) {
        if (type == DaoEventType.UPDATE) {
            return new DaoEvent<T>(this, type, vo, existing);
        } else {
            return new DaoEvent<T>(this, type, vo);
        }
    }

    protected void publishEvent(DaoEvent<T> event) {
        if (this.eventPublisher != null) {
            this.eventPublisher.publishEvent(event);
        }
    }

    /**
     * Get a writer for serializing JSON
     * @return
     */
    public ObjectWriter getObjectWriter(Class<?> type) {
        return mapper.writerFor(type);
    }

    /**
     * Get a reader for use de-serializing JSON
     * @return
     */
    public ObjectReader getObjectReader(Class<?> type) {
        return mapper.readerFor(type);
    }

    /**
     * Available to overload the result set extractor for list queries.
     *
     * If a module is not installed the exception is caught and logged,
     * which could mess up a query limit/offset scenario.  Resulting in
     * less items than asked for.
     *
     * @return
     */
    protected ResultSetExtractor<T> getObjectResultSetExtractor() {
        return getObjectResultSetExtractor((e,rs) -> {
            if(e.getCause() instanceof ModuleNotLoadedException) {
                //We will log and continue as to not prevent someone from loading module based VOs for
                // which the modules are actually installed.
                LOG.error(e.getCause().getMessage(), e.getCause());
            }else {
                LOG.error(e.getMessage(), e);
                //TODO Mango 4.0 What shall we do here? most likely this caused by a bug in the code and we
                // want to see the 500 error in the API etc.
                throw new ShouldNeverHappenException(e);
            }
        });
    }

    /**
     *
     * @param error
     * @return
     */
    protected ResultSetExtractor<T> getObjectResultSetExtractor(BiConsumer<Exception, ResultSet> error) {
        return new ResultSetExtractor<T>() {

            @Override
            public T extractData(ResultSet rs) throws SQLException, DataAccessException {
                RowMapper<T> rowMapper = getRowMapper();
                List<T> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    try {
                        T row = rowMapper.mapRow(rs, rowNum);
                        loadRelationalData(row);
                        results.add(row);
                    }catch (Exception e) {
                        error.accept(e, rs);
                        //Abort mission
                        break;
                    }finally {
                        rowNum++;
                    }
                    return DataAccessUtils.uniqueResult(results);
                }
                return null;
            }
        };
    }

    /**
     * Available to overload the result set extractor for list queries.
     *
     * If a module is not installed the exception is caught and logged,
     * which could mess up a query limit/offset scenario.  Resulting in
     * less items than asked for.
     *
     * @return
     */
    protected ResultSetExtractor<List<T>> getListResultSetExtractor() {
        return getListResultSetExtractor((e,rs) -> {
            if(e.getCause() instanceof ModuleNotLoadedException) {
                LOG.error(e.getCause().getMessage(), e.getCause());
            }else {
                LOG.error(e.getMessage(), e);
                //TODO Mango 4.0 What shall we do here? most likely this caused by a bug in the code and we
                // want to see the 500 error in the API etc.
                throw new ShouldNeverHappenException(e);
            }
        });
    }

    /**
     *
     * @param error
     * @return
     */
    protected ResultSetExtractor<List<T>> getListResultSetExtractor(BiConsumer<Exception, ResultSet> error) {
        return new ResultSetExtractor<List<T>>() {

            @Override
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                RowMapper<T> rowMapper = getRowMapper();
                List<T> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    try {
                        T row = rowMapper.mapRow(rs, rowNum);
                        loadRelationalData(row);
                        results.add(row);
                    }catch (Exception e) {
                        error.accept(e, rs);
                        //Abort mission
                        break;
                    }finally {
                        rowNum++;
                    }
                }
                return results;
            }
        };
    }

    /**
     * Available to overload the result set extractor for callback queries
     *  to customize error handling
     * @param callback
     * @return
     */
    protected ResultSetExtractor<Void> getCallbackResultSetExtractor(MappedRowCallback<T> callback) {
        return getCallbackResultSetExtractor(callback, (e, rs) -> {
            if(e.getCause() instanceof ModuleNotLoadedException) {
                LOG.error(e.getCause().getMessage(), e.getCause());
            }else {
                LOG.error(e.getMessage(), e);
                //TODO Mango 4.0 What shall we do here? most likely this caused by a bug in the code and we
                // want to see the 500 error in the API etc.
                throw new ShouldNeverHappenException(e);
            }
        });
    }

    /**
     *
     * @param callback
     * @param error
     * @return
     */
    protected ResultSetExtractor<Void> getCallbackResultSetExtractor(MappedRowCallback<T> callback, BiConsumer<Exception, ResultSet> error) {
        return new ResultSetExtractor<Void>() {

            @Override
            public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
                RowMapper<T> rowMapper = getRowMapper();
                int rowNum = 0;
                while (rs.next()) {
                    try {
                        T row = rowMapper.mapRow(rs, rowNum);
                        loadRelationalData(row);
                        callback.row(row, rowNum);
                    }catch (Exception e) {
                        error.accept(e, rs);
                        //Abort mission
                        break;
                    }finally {
                        rowNum++;
                    }
                }
                return null;
            }
        };
    }

    @Override
    public void lockRow(int id) {
        create.select().from(table)
        .where(getIdField().eq(id))
        .forUpdate()
        .fetch();
    }

    /**
     * Helper to convert JSON Node for db
     * @param data
     * @return
     */
    protected String convertData(JsonNode data) {
        try {
            if(data == null) {
                return null;
            }else {
                return getObjectWriter(JsonNode.class).writeValueAsString(data);
            }
        }catch(JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    protected JsonNode extractData(Clob c) throws SQLException {
        if (c != null) {
            try (Reader reader = c.getCharacterStream()) {
                return getObjectReader(JsonNode.class).readValue(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return null;
    }

    protected JsonNode extractData(String c) {
        if (c != null) {
            try {
                return getObjectReader(JsonNode.class).readValue(c);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return null;
    }

    @Override
    public int count(PermissionHolder user) {
        return customizedCount(new ConditionSortLimit(null, null, null, null), user);
    }

    @Override
    public int count(PermissionHolder user, String rql) {
        ConditionSortLimit csl = rqlToCondition(RQLUtils.parseRQLtoAST(rql), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        return customizedCount(csl, user);
    }

    @Override
    public List<T> list(PermissionHolder user) {
        List<T> list = new ArrayList<>();
        list(user, list::add);
        return list;
    }

    @Override
    public void list(PermissionHolder user, Consumer<T> consumer) {
        customizedQuery(new ConditionSortLimit(null, null, null, null), user, consumer);
    }

    @Override
    public List<T> query(PermissionHolder user, String rql) {
        List<T> list = new ArrayList<>();
        query(user, rql, list::add);
        return list;
    }

    @Override
    public void query(PermissionHolder user, String rql, Consumer<T> consumer) {
        ConditionSortLimit csl = rqlToCondition(RQLUtils.parseRQLtoAST(rql), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        customizedQuery(csl, user, consumer);
    }

    @Override
    public QueryBuilder<T> buildQuery(PermissionHolder user) {
        return new QueryBuilder<T>(aliasMap, valueConverterMap, csl -> customizedCount(csl, user), (csl, consumer) -> customizedQuery(csl, user, consumer));
    }

}
