/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jooq.Condition;
import org.jooq.DeleteConditionStep;
import org.jooq.DeleteLimitStep;
import org.jooq.DeleteUsingStep;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.ResultQuery;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectLimitStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.SelectUnionStep;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.infiniteautomation.mango.db.query.QueryCancelledException;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.PointValueAnnotations;
import com.infiniteautomation.mango.db.tables.PointValues;
import com.infiniteautomation.mango.db.tables.records.PointValueAnnotationsRecord;
import com.infiniteautomation.mango.db.tables.records.PointValuesRecord;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedIdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.IAnnotated;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.pair.LongPair;

/**
 * Basic implementation with no insert capabilities, no batch writer functionality.
 */
public class BasicSQLPointValueDao extends BaseDao implements PointValueDao {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final PointValueAnnotations pva = PointValueAnnotations.POINT_VALUE_ANNOTATIONS;
    protected final PointValues pv = PointValues.POINT_VALUES;
    protected final DataPoints dp = DataPoints.DATA_POINTS;

    public BasicSQLPointValueDao(DatabaseProxy databaseProxy) {
        super(databaseProxy);
    }

    @Override
    public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue, SetPointSource source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue, SetPointSource source) {
        throw new UnsupportedOperationException();
    }

    protected PointValueTime getPointValue(long id) {
        return baseQuery().where(pv.id.equal(id))
                .limit(1)
                .fetchOne(this::mapRecord);
    }

    protected PointValueTime getPointValueAt(DataPointVO vo, Field<Long> time) {
        return baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.equal(time))
                .limit(1)
                .fetchOne(this::mapRecord);
    }

    @Override
    public PointValueTime getLatestPointValue(DataPointVO vo) {
        Field<Long> ts = this.create.select(DSL.max(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public PointValueTime getPointValueBefore(DataPointVO vo, long time) {
        Field<Long> ts = this.create.select(DSL.max(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.lessThan(time))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public PointValueTime getPointValueAt(DataPointVO vo, long time) {
        return getPointValueAt(vo, DSL.val(time));
    }

    @Override
    public PointValueTime getPointValueAfter(DataPointVO vo, long time) {
        Field<Long> ts = this.create.select(DSL.min(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(time))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public List<PointValueTime> getPointValues(DataPointVO vo, long since) {
        return baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(since))
                .orderBy(pv.ts)
                .fetch(this::mapRecord);
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to) {
        return baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(from))
                .and(pv.ts.lessThan(to))
                .orderBy(pv.ts)
                .fetch(this::mapRecord);
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to, int limit) {
        return baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(from))
                .and(pv.ts.lessThan(to))
                .orderBy(pv.ts)
                .limit(limit)
                .fetch(this::mapRecord);
    }

    @Override
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit) {
        if (limit == 0) {
            return Collections.emptyList();
        } else if (limit == 1) {
            PointValueTime pvt = getLatestPointValue(vo);
            return pvt == null ? Collections.emptyList() : Collections.singletonList(pvt);
        } else {
            return baseQuery()
                    .where(pv.dataPointId.equal(vo.getSeriesId()))
                    .orderBy(pv.ts.desc())
                    .limit(limit)
                    .fetch(this::mapRecord);
        }
    }

    @Override
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit, long before) {
        if (limit == 0) {
            return Collections.emptyList();
        } else {
            return baseQuery()
                    .where(pv.dataPointId.equal(vo.getSeriesId()))
                    .and(pv.ts.lessThan(before))
                    .orderBy(pv.ts.desc())
                    .limit(limit)
                    .fetch(this::mapRecord);
        }
    }

    public IdPointValueTime mapRecord(Record record) {
        PointValuesRecord pvRecord = record.into(pv);
        PointValueAnnotationsRecord pvaRecord = record.into(pva);

        int dataPointId = pvRecord.get(pv.dataPointId);
        long timestamp = pvRecord.get(pv.ts);
        DataValue value = createDataValue(pvRecord, pvaRecord);

        TranslatableMessage sourceMessage = readTranslatableMessage(pvaRecord.get(pva.sourceMessage));
        if (sourceMessage != null) {
            return new AnnotatedIdPointValueTime(dataPointId, value, timestamp, sourceMessage);
        }
        return new IdPointValueTime(dataPointId, value, timestamp);
    }

    public SelectOnConditionStep<Record> baseQuery() {
        return this.create.select(pv.fields())
                .select(pva.fields())
                .from(pv)
                .leftJoin(pva)
                .on(pv.id.equal(pva.pointValueId));
    }

    @Override
    public void getLatestPointValues(List<DataPointVO> vos, long before, boolean orderById, Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
        if (vos.size() == 0) return;

        Integer[] seriesIds = vos.stream().mapToInt(DataPointVO::getSeriesId).sorted().boxed().toArray(Integer[]::new);
        ResultQuery<Record> result;

        if (orderById && limit != null) {
            SelectUnionStep<Record> union = null;
            for (int seriesId : seriesIds) {
                Condition condition = pv.dataPointId.equal(seriesId);
                if (before != Long.MAX_VALUE) {
                    condition = condition.and(pv.ts.lessThan(before));
                }

                SelectLimitPercentStep<Record> r = baseQuery()
                        .where(condition)
                        .orderBy(pv.ts.desc())
                        .limit(limit);

                union = union == null ? r : union.unionAll(r);
            }
            result = union;
        } else {
            Condition condition = pv.dataPointId.in(seriesIds);
            if (before != Long.MAX_VALUE) {
                condition = condition.and(pv.ts.lessThan(before));
            }

            SelectConditionStep<Record> conditionStep = baseQuery()
                    .where(condition);

            SelectLimitStep<Record> limitStep = orderById ?
                    conditionStep.orderBy(pv.dataPointId.asc(), pv.ts.desc()) :
                    conditionStep.orderBy(pv.ts.desc());

            result = limit == null ? limitStep : limitStep.limit(limit);
        }

        AtomicInteger count = new AtomicInteger();
        try (Stream<Record> stream = result.stream()) {
            stream.map(this::mapRecord).forEach(pvt -> callback.row(pvt, count.getAndIncrement()));
        }
    }

    @Override
    public void getPointValuesBetween(DataPointVO vo, long from, long to, Consumer<PointValueTime> callback) {
        ResultQuery<Record> result = baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(from))
                .and(pv.ts.lessThan(to))
                .orderBy(pv.ts);

        try (Stream<Record> stream = result.stream()) {
            stream.map(this::mapRecord).forEach(callback);
        }
    }

    /**
     * Single point value time range logic
     *
     * @author Terry Packer
     */
    class TimeRangeSinglePointValuesPreparedStatementCreator<T extends PVTQueryCallback<IdPointValueTime>> implements PreparedStatementCreator, PreparedStatementCallback<Integer> {

        final AnnotatedIdPointValueRowMapper mapper = new AnnotatedIdPointValueRowMapper();

        final List<DataPointVO> vos;
        final long from;
        final long to;
        final Integer limit;
        final T callback;
        MutableInt counter;

        public TimeRangeSinglePointValuesPreparedStatementCreator(DataPointVO vo, long from, long to, Integer limit, T callback, MutableInt counter) {
            this.vos = new ArrayList<>();
            this.vos.add(vo);
            this.from = from;
            this.to = to;
            this.limit = limit;
            this.callback = callback;
            this.counter = counter;
        }

        public TimeRangeSinglePointValuesPreparedStatementCreator(List<DataPointVO> vos, long from, long to, Integer limit, T callback, MutableInt counter) {
            this.vos = vos;
            this.from = from;
            this.to = to;
            this.limit = limit;
            this.callback = callback;
            this.counter = counter;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {
            if (vos.size() != 1)
                throw new RuntimeException("Wrong base query.");

            List<Object> args = new ArrayList<>();
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId = ? and pv.ts >= ? and pv.ts<? order by pv.ts asc";
            args.add(vos.get(0).getSeriesId());
            args.add(from);
            args.add(to);
            if (limit != null) {
                sql += " limit ?";
                args.add(limit);
            }

            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(args.toArray(new Object[0]));
            setter.setValues(stmt);
            return stmt;
        }

        @Override
        public Integer doInPreparedStatement(PreparedStatement ps)
                throws SQLException, DataAccessException {
            ResultSet rs = null;
            try {
                ps.execute();
                rs = ps.getResultSet();
                while (rs.next()) {
                    IdPointValueTime value = mapper.mapRow(rs, counter.getValue());
                    callback.row(value, counter.getValue());
                    counter.increment();
                }
            } catch (QueryCancelledException e) {
                log.warn("Cancelling Time Range Point Value Query.", e);
                ps.cancel();
                throw e;
            } finally {
                JdbcUtils.closeResultSet(rs);
            }
            return counter.getValue();
        }
    }

    /**
     * Query multiple points over a range
     *
     * @author Terry Packer
     */
    class TimeRangeMultiplePointsValuesPreparedStatementCreator<T extends PVTQueryCallback<IdPointValueTime>> extends TimeRangeSinglePointValuesPreparedStatementCreator<T> {

        public TimeRangeMultiplePointsValuesPreparedStatementCreator(List<DataPointVO> vos, long from, long to,
                                                                     Integer limit, T callback) {
            super(vos, from, to, limit, callback, new MutableInt(0));
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {

            if (vos.size() == 1)
                return super.createPreparedStatement(con);

            List<Object> args = new ArrayList<>();
            List<Integer> ids = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
            String dataPointIds = createDelimitedList(ids, ",", null);
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " WHERE pv.dataPointId in (" + dataPointIds + ") AND pv.ts >= ? AND pv.ts<? ORDER BY pv.ts ASC";
            args.add(from);
            args.add(to);

            if (limit != null) {
                sql += " limit ?";
                args.add(limit);
            }

            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(args.toArray(new Object[0]));
            setter.setValues(stmt);
            return stmt;
        }
    }

    @Override
    public void getPointValuesBetween(List<DataPointVO> vos, long from, long to, boolean orderById, Integer limit, PVTQueryCallback<IdPointValueTime> callback) {
        if (vos.size() == 0)
            return;
        if (orderById) {
            //Limit results of each data point to size limit, i.e. loop over all points and query with limit
            MutableInt counter = new MutableInt(0);
            for (DataPointVO vo : vos) {
                TimeRangeSinglePointValuesPreparedStatementCreator<PVTQueryCallback<IdPointValueTime>> c =
                        new TimeRangeSinglePointValuesPreparedStatementCreator<>(vo, from, to, limit, callback, counter);
                ejt.execute(c, c);
            }
        } else {
            //Limit total results to limit
            TimeRangeMultiplePointsValuesPreparedStatementCreator<PVTQueryCallback<IdPointValueTime>> c =
                    new TimeRangeMultiplePointsValuesPreparedStatementCreator<>(vos, from, to, limit, callback);
            ejt.execute(c, c);
        }
    }

    @Override
    public void wideQuery(DataPointVO vo, long from, long to, WideQueryCallback<PointValueTime> callback) {
        // TODO Improve performance by using one statement and using the exceptions to cancel the results
        PointValueTime pvt = this.getPointValueBefore(vo, from);
        if (pvt != null)
            callback.preQuery(pvt);
        this.getPointValuesBetween(vo, from, to, callback::row);
        pvt = this.getPointValueAfter(vo, to);
        if (pvt != null)
            callback.postQuery(pvt);
    }

    private static final String ANNOTATED_POINT_ID_VALUE_SELECT = "select pv.dataPointId, pv.dataType, pv.pointValue, " //
            + "pva.textPointValueShort, pva.textPointValueLong, pv.ts, pva.sourceMessage "
            + "from pointValues pv "
            + "  left join pointValueAnnotations pva on pv.id = pva.pointValueId";

    /**
     * Process a cancellable bookend time range query
     *
     * @author Terry Packer
     */
    class BookendSinglePointValuesPreparedStatementCreator extends TimeRangeSinglePointValuesPreparedStatementCreator<BookendQueryCallback<IdPointValueTime>> {

        //Single Value Queue for writing values in order
        protected final Map<Integer, IdPointValueTime> values;
        //Create a statement specifically for fetching the first values.
        protected PreparedStatement firstValuesSelect;

        public BookendSinglePointValuesPreparedStatementCreator(DataPointVO vo, long from, long to,
                                                                Integer limit, BookendQueryCallback<IdPointValueTime> callback, MutableInt counter) {
            super(vo, from, to, limit, callback, counter);
            this.values = new HashMap<>(1);
        }

        public BookendSinglePointValuesPreparedStatementCreator(List<DataPointVO> vos, long from, long to,
                                                                Integer limit, BookendQueryCallback<IdPointValueTime> callback, MutableInt counter) {
            super(vos, from, to, limit, callback, counter);
            this.values = new HashMap<>(vos.size());
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            if (vos.size() != 1)
                throw new RuntimeException("Wrong base query.");

            String startValueSql = "";
            List<Object> startValueArgs = new ArrayList<>(2);
            startValueSql = startValueSql + ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId=? AND pv.ts <= ? ORDER BY ts DESC LIMIT 1 ";
            startValueArgs.add(vos.get(0).getSeriesId());
            startValueArgs.add(from);

            firstValuesSelect = con.prepareStatement(startValueSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(startValueArgs.toArray(new Object[0]));
            setter.setValues(firstValuesSelect);

            List<Object> args = new ArrayList<>(3); //pv.ts > ? because firstValueSelect is special
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId = ? and pv.ts > ? and pv.ts<? order by pv.ts asc";
            args.add(vos.get(0).getSeriesId());
            args.add(from);
            args.add(to);
            if (limit != null) {
                sql += " limit ?";
                args.add(limit);
            }

            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            setter = new ArgumentPreparedStatementSetter(args.toArray(new Object[0]));
            setter.setValues(stmt);
            return stmt;
        }

        @Override
        public Integer doInPreparedStatement(PreparedStatement ps)
                throws SQLException, DataAccessException {
            ResultSet rs = null;
            MutableInt realSamples = new MutableInt(0);
            try {
                firstValuesSelect.execute();
                rs = firstValuesSelect.getResultSet();
                while (rs.next()) {
                    IdPointValueTime current = mapper.mapRow(rs, counter.getValue());
                    if (current.getTime() == from) {
                        callback.firstValue(current, counter.getAndIncrement(), false);
                        realSamples.increment();
                    } else {
                        IdPointValueTime fakeValue;
                        if (current instanceof IAnnotated)
                            fakeValue = new AnnotatedIdPointValueTime(current.getSeriesId(), current.getValue(), from,
                                    ((IAnnotated) current).getSourceMessage());
                        else
                            fakeValue = new IdPointValueTime(current.getSeriesId(), current.getValue(), from);
                        callback.firstValue(fakeValue, counter.getAndIncrement(), true);
                    }
                    values.put(current.getSeriesId(), current);
                }

                for (DataPointVO vo : vos)
                    if (!values.containsKey(vo.getSeriesId()))
                        callback.firstValue(new IdPointValueTime(vo.getSeriesId(), null, from), counter.getAndIncrement(), true);
            } catch (QueryCancelledException e) {
                log.warn("Cancelling Time Range Point Value Query.", e);
                firstValuesSelect.cancel();
                ps.cancel();
                throw e;
            } finally {
                JdbcUtils.closeResultSet(rs);
            }
            try {
                ps.execute();
                rs = ps.getResultSet();

                //Process the data in time order, saving the current value for use in the lastValue callback at the end.
                while (rs.next()) {
                    IdPointValueTime current = mapper.mapRow(rs, counter.getValue());
                    values.put(current.getSeriesId(), current);
                    callback.row(current, counter.getAndIncrement());
                    if (limit != null && realSamples.incrementAndGet() == limit)
                        break;
                }

                for (IdPointValueTime current : values.values()) {
                    IdPointValueTime fakeValue;
                    if (current instanceof IAnnotated)
                        fakeValue = new AnnotatedIdPointValueTime(current.getSeriesId(), current.getValue(), to,
                                ((IAnnotated) current).getSourceMessage());
                    else
                        fakeValue = new IdPointValueTime(current.getSeriesId(), current.getValue(), to);
                    callback.lastValue(fakeValue, counter.getAndIncrement(), true);
                }

                for (DataPointVO vo : vos) {
                    if (!values.containsKey(vo.getSeriesId()))
                        callback.lastValue(new IdPointValueTime(vo.getSeriesId(), null, to), counter.getAndIncrement(), true);
                }
            } catch (QueryCancelledException e) {
                log.warn("Cancelling Time Range Point Value Query.", e);
                ps.cancel();
                throw e;
            } finally {
                JdbcUtils.closeResultSet(rs);
            }
            return counter.getValue();
        }
    }

    /**
     * Bookend for mulitple points
     *
     * @author Terry Packer
     */
    class BookendMultiplePointValuesPreparedStatementCreator extends BookendSinglePointValuesPreparedStatementCreator {


        public BookendMultiplePointValuesPreparedStatementCreator(List<DataPointVO> vos, long from, long to,
                                                                  Integer limit, BookendQueryCallback<IdPointValueTime> callback) {
            super(vos, from, to, limit, callback, new MutableInt(0));
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con)
                throws SQLException {

            if (vos.size() == 1)
                return super.createPreparedStatement(con);

            boolean first = true;
            String startValueSql = "";
            List<Object> startValueArgs = new ArrayList<>(vos.size() * 2);
            for (DataPointVO vo : vos) {
                if (first)
                    first = false;
                else
                    startValueSql += " UNION ";
                startValueSql = startValueSql + "(" + ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId=? AND pv.ts <= ? ORDER BY ts DESC LIMIT 1) ";
                startValueArgs.add(vo.getSeriesId());
                startValueArgs.add(from);
            }

            firstValuesSelect = con.prepareStatement(startValueSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ArgumentPreparedStatementSetter setter = new ArgumentPreparedStatementSetter(startValueArgs.toArray(new Object[0]));
            setter.setValues(firstValuesSelect);

            List<Object> args = new ArrayList<>(vos.size() * 2);
            List<Integer> ids = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
            String dataPointIds = createDelimitedList(ids, ",", null);
            String sql = ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId in (" + dataPointIds + ") AND pv.ts >= ? AND pv.ts<? ORDER BY pv.ts ASC";
            args.add(from + 1); //handle from in the startValueSql
            args.add(to);

            if (limit != null) {
                sql += " limit ?";
                args.add(limit);
            }

            PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            setter = new ArgumentPreparedStatementSetter(args.toArray(new Object[0]));
            setter.setValues(stmt);
            return stmt;
        }
    }


    @Override
    public void wideBookendQuery(List<DataPointVO> vos, long from, long to, boolean orderById, Integer limit, BookendQueryCallback<IdPointValueTime> callback) {
        if (vos.size() == 0)
            return;
        if (orderById) {
            //Limit results of each data point to size limit, i.e. loop over all points and query with limit
            MutableInt counter = new MutableInt(0);
            for (DataPointVO vo : vos) {
                BookendSinglePointValuesPreparedStatementCreator c =
                        new BookendSinglePointValuesPreparedStatementCreator(vo, from, to, limit, callback, counter);
                ejt.execute(c, c);
            }
        } else {
            //Limit total results to limit
            BookendMultiplePointValuesPreparedStatementCreator c =
                    new BookendMultiplePointValuesPreparedStatementCreator(vos, from, to, limit, callback);
            ejt.execute(c, c);
        }
    }

    DataValue createDataValue(ResultSet rs, int firstParameter) throws SQLException {
        int dataType = rs.getInt(firstParameter);
        DataValue value;
        switch (dataType) {
            case (DataTypes.NUMERIC):
                value = new NumericValue(rs.getDouble(firstParameter + 1));
                break;
            case (DataTypes.BINARY):
                value = new BinaryValue(rs.getDouble(firstParameter + 1) == 1);
                break;
            case (DataTypes.MULTISTATE):
                value = new MultistateValue(rs.getInt(firstParameter + 1));
                break;
            case (DataTypes.ALPHANUMERIC):
                String s = rs.getString(firstParameter + 2);
                if (s == null)
                    s = rs.getString(firstParameter + 3);
                value = new AlphanumericValue(s);
                break;
            case (DataTypes.IMAGE):
                value = new ImageValue(Long.parseLong(rs.getString(firstParameter + 2)), rs.getInt(firstParameter + 1));
                break;
            default:
                value = null;
        }
        return value;
    }

    DataValue createDataValue(PointValuesRecord pvRecord, PointValueAnnotationsRecord pvaRecord) {
        int dataType = pvRecord.get(pv.dataType);
        switch (dataType) {
            case (DataTypes.NUMERIC): {
                Double doubleValue = pvRecord.get(pv.pointValue);
                return new NumericValue(doubleValue);
            }
            case (DataTypes.BINARY): {
                boolean booleanValue = pvRecord.get(pv.pointValue) == 1;
                return new BinaryValue(booleanValue);
            }
            case (DataTypes.MULTISTATE): {
                int intValue = (int) Math.round(pvRecord.get(pv.pointValue));
                return new MultistateValue(intValue);
            }
            case (DataTypes.ALPHANUMERIC): {
                String shortTextValue = pvaRecord.get(pva.textPointValueShort);
                if (shortTextValue != null) {
                    return new AlphanumericValue(shortTextValue);
                }
                String longTextValue = pvaRecord.get(pva.textPointValueLong);
                return new AlphanumericValue(longTextValue);
            }
            case (DataTypes.IMAGE):
                String shortTextValue = pvaRecord.get(pva.textPointValueShort);
                int intValue = (int) Math.round(pvRecord.get(pv.pointValue));
                return new ImageValue(Long.parseLong(shortTextValue), intValue);
        }
        return null;
    }

    @Override
    public void getPointValuesBetween(List<DataPointVO> vos, long from, long to, Consumer<IdPointValueTime> callback) {
        try (var stream = baseQuery()
                .where(seriesIdCondition(vos))
                .and(pv.ts.greaterOrEqual(from))
                .and(pv.ts.lessThan(to))
                .orderBy(pv.ts).stream()) {
            stream.map(this::mapRecord).forEach(callback);
        }
    }

    /**
     * Bring across source translation
     *
     * @author Terry Packer
     */
    class AnnotatedIdPointValueRowMapper implements RowMapper<IdPointValueTime> {
        @Override
        public IdPointValueTime mapRow(ResultSet rs, int rowNum) throws SQLException {
            int seriesId = rs.getInt(1);
            DataValue value = createDataValue(rs, 2);
            long time = rs.getLong(6);
            TranslatableMessage sourceMessage = BaseDao.readTranslatableMessage(rs, 7);
            if (sourceMessage != null)
                return new AnnotatedIdPointValueTime(seriesId, value, time, sourceMessage);
            else
                return new IdPointValueTime(seriesId, value, time);
        }
    }

    public DeleteUsingStep<PointValuesRecord> baseDelete() {
        return this.create.deleteFrom(pv);
    }

    @Override
    public long deletePointValue(DataPointVO vo, long ts) {
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .and(pv.ts.eq(ts));
        return deletePointValues(delete);
    }

    @Override
    public long deletePointValuesBefore(DataPointVO vo, long time) {
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .and(pv.ts.lessThan(time));
        return deletePointValues(delete);
    }

    @Override
    public long deletePointValuesBetween(DataPointVO vo, long startTime, long endTime) {
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(startTime))
                .and(pv.ts.lessThan(endTime));
        return deletePointValues(delete);
    }

    @Override
    public boolean deletePointValuesBeforeWithoutCount(DataPointVO vo, long time) {
        return deletePointValuesBefore(vo, time) > 0;
    }

    @Override
    public long deletePointValues(DataPointVO vo) {
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.eq(vo.getSeriesId()));
        return deletePointValues(delete);
    }

    @Override
    public boolean deletePointValuesWithoutCount(DataPointVO vo) {
        return deletePointValues(vo) > 0;
    }

    @Override
    public long deleteAllPointData() {
        return deletePointValues(baseDelete());
    }

    @Override
    public void deleteAllPointDataWithoutCount() {
        deleteAllPointData();
    }

    @Override
    public long deleteOrphanedPointValues() {
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.notIn(
                        this.create.select(dp.seriesId).from(dp)
                ));
        return deletePointValues(delete, 5000, 100000L);
    }

    @Override
    public void deleteOrphanedPointValuesWithoutCount() {
        deleteOrphanedPointValues();
    }

    @Override
    public void deleteOrphanedPointValueAnnotations() {
        int limit = databaseProxy.batchSize();
        while (true) {
            List<Long> ids = this.create.select(pva.pointValueId)
                    .from(pva)
                    .leftJoin(pv)
                    .on(pv.id.equal(pva.pointValueId))
                    .where(pv.id.isNull())
                    .limit(limit)
                    .fetch(pva.pointValueId);

            if (ids.isEmpty())
                break;

            int deleted = this.create.deleteFrom(pva)
                    .where(pva.pointValueId.in(ids))
                    .execute();

            if (deleted < limit)
                break;
        }
    }

    protected long deletePointValues(DeleteLimitStep<PointValuesRecord> deleteLimitStep) {
        return deletePointValues(deleteLimitStep, 0, null);
    }

    protected long deletePointValues(DeleteLimitStep<PointValuesRecord> deleteLimitStep, long chunkWait, Long limit) {
        int chunkSize = 1000;
        long total = 0;
        while (true) {
            int count = deleteLimitStep.limit(chunkSize).execute();
            total += count;

            if (count < chunkSize || (limit != null && total >= limit))
                break;

            if (chunkWait > 0) {
                try {
                    Thread.sleep(chunkWait);
                } catch (InterruptedException e) {
                    // no op
                }
            }
        }
        return total;
    }

    @Override
    public long dateRangeCount(DataPointVO vo, long from, long to) {
        return create.select(DSL.count())
                .from(pv)
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(0);
    }

    @Override
    public long getInceptionDate(DataPointVO vo) {
        return create.select(DSL.min(pv.ts))
                .from(pv)
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(-1L);
    }

    private Condition seriesIdCondition(List<DataPointVO> vos) {
        List<Integer> seriesIds = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
        return pv.dataPointId.in(seriesIds);
    }

    @Override
    public long getStartTime(List<DataPointVO> vos) {
        if (vos.isEmpty())
            return -1L;
        return create.select(DSL.min(pv.ts)).from(pv).where(seriesIdCondition(vos))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(-1L);
    }

    @Override
    public long getEndTime(List<DataPointVO> vos) {
        if (vos.isEmpty())
            return -1L;
        return create.select(DSL.max(pv.ts)).from(pv).where(seriesIdCondition(vos))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(-1L);
    }

    @Override
    public LongPair getStartAndEndTime(List<DataPointVO> vos) {
        if (vos.isEmpty())
            return null;
        return create.select(DSL.min(pv.ts), DSL.max(pv.ts)).from(pv).where(seriesIdCondition(vos))
                .fetchOptional()
                .map(record -> new LongPair(record.value1(), record.value2()))
                .orElse(null);
    }

    @Override
    public List<Long> getFiledataIds(DataPointVO vo) {
        return create.select(pv.id).from(pv)
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .and(pv.dataType.eq(DataTypes.IMAGE))
                .fetch(pv.id);
    }
}
