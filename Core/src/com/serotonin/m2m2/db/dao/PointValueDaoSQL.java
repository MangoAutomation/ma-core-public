/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.DeleteLimitStep;
import org.jooq.DeleteUsingStep;
import org.jooq.Field;
import org.jooq.InsertValuesStep4;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectLimitStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.SelectUnionStep;
import org.jooq.impl.DSL;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.infiniteautomation.mango.db.query.QueryCancelledException;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.PointValueAnnotations;
import com.infiniteautomation.mango.db.tables.PointValues;
import com.infiniteautomation.mango.db.tables.records.PointValueAnnotationsRecord;
import com.infiniteautomation.mango.db.tables.records.PointValuesRecord;
import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.ImageSaveException;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedIdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
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
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.pair.LongPair;
import com.serotonin.metrics.EventHistogram;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.util.queue.ObjectQueue;

public class PointValueDaoSQL extends BaseDao implements CachingPointValueDao {

    private final Log log = LogFactory.getLog(PointValueDaoSQL.class);

    private final ConcurrentLinkedQueue<UnsavedPointValue> unsavedPointValues = new ConcurrentLinkedQueue<>();

    private final PointValueAnnotations pva = PointValueAnnotations.POINT_VALUE_ANNOTATIONS;
    private final PointValues pv = PointValues.POINT_VALUES;
    private final DataPoints dp = DataPoints.DATA_POINTS;

    public PointValueDaoSQL() {
        super();
    }

    public PointValueDaoSQL(DataSource dataSource, PlatformTransactionManager transactionManager, DatabaseType databaseType, Configuration configuration) {
        super(dataSource, transactionManager, databaseType, configuration);
    }

    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    @Override
    public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue, SetPointSource source) {
        long id = savePointValueImpl(vo, pointValue, source, false);

        PointValueTime savedPointValue;
        int retries = 5;
        while (true) {
            try {
                savedPointValue = getPointValue(id);
                break;
            } catch (ConcurrencyFailureException e) {
                if (retries <= 0)
                    throw e;
                retries--;
            }
        }

        return savedPointValue;
    }

    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    @Override
    public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue, SetPointSource source) {
        savePointValueImpl(vo, pointValue, source, true);
    }

    long savePointValueImpl(final DataPointVO vo, final PointValueTime pointValue, final SetPointSource source,
                            boolean async) {
        DataValue value = pointValue.getValue();
        final int dataType = DataTypes.getDataType(value);
        double dvalue = 0;
        String svalue = null;

        if (dataType == DataTypes.IMAGE) {
            ImageValue imageValue = (ImageValue) value;
            dvalue = imageValue.getType();
            if (imageValue.isSaved())
                svalue = Long.toString(imageValue.getId());
        } else if (value.hasDoubleRepresentation())
            dvalue = value.getDoubleValue();
        else
            svalue = value.getStringValue();

        // Check if we need to create an annotation.
        long id;
        try {
            if (svalue != null || source != null || dataType == DataTypes.IMAGE)
                async = false;
            id = savePointValue(vo, dataType, dvalue, pointValue.getTime(), svalue, source, async);
        } catch (ConcurrencyFailureException e) {
            // Still failed to insert after all of the retries. Store the data
            unsavedPointValues.add(new UnsavedPointValue(vo, pointValue, source));
            return -1;
        }

        // Check if we need to save an image
        if (dataType == DataTypes.IMAGE) {
            ImageValue imageValue = (ImageValue) value;
            if (!imageValue.isSaved()) {
                imageValue.setId(id);

                Path filePath = Common.getFiledataPath().resolve(imageValue.getFilename());
                try (InputStream is = new ByteArrayInputStream(imageValue.getData())) {
                    Files.copy(is, filePath);
                } catch (IOException e) {
                    // Rethrow as an RTE
                    throw new ImageSaveException(e);
                }

                // Allow the data to be GC'ed
                imageValue.setData(null);
            }
        }

        clearUnsavedPointValues();

        return id;
    }

    private void clearUnsavedPointValues() {
        UnsavedPointValue data;
        while ((data = unsavedPointValues.poll()) != null) {
            savePointValueImpl(data.getVO(), data.getPointValue(), data.getSource(), false);
        }
    }

    long savePointValue(final DataPointVO vo, final int dataType, double dvalue, final long time, final String svalue,
                        final SetPointSource source, boolean async) {
        // Apply database specific bounds on double values.
        dvalue = Common.databaseProxy.applyBounds(dvalue);

        if (async) {
            BatchWriteBehind.add(new BatchWriteBehindEntry(vo, dataType, dvalue, time), create);
            return -1;
        }

        int retries = 5;
        while (true) {
            try {
                return savePointValueImpl(vo, dataType, dvalue, time, svalue, source);
            } catch (ConcurrencyFailureException e) {
                if (retries <= 0)
                    throw e;
                retries--;
            } catch (RuntimeException e) {
                throw new RuntimeException("Error saving point value: dataType=" + dataType + ", dvalue=" + dvalue, e);
            }
        }
    }

    private long savePointValueImpl(DataPointVO vo, int dataType, double dvalue, long time, String svalue,
                                    SetPointSource source) {

        long id = this.create.insertInto(pv)
                .set(pv.dataPointId, vo.getSeriesId())
                .set(pv.dataType, dataType)
                .set(pv.pointValue, dvalue)
                .set(pv.ts, time)
                .returningResult(pv.id)
                .fetchOne()
                .value1();

        if (svalue == null && dataType == DataTypes.IMAGE)
            svalue = Long.toString(id);

        // Check if we need to create an annotation.
        TranslatableMessage sourceMessage = null;
        if (source != null)
            sourceMessage = source.getSetPointSourceMessage();

        if (svalue != null || sourceMessage != null) {
            String shortString = null;
            String longString = null;
            if (svalue != null) {
                if (svalue.length() > 128)
                    longString = svalue;
                else
                    shortString = svalue;
            }

            this.create.insertInto(pva)
                    .set(pva.pointValueId, id)
                    .set(pva.textPointValueShort, shortString)
                    .set(pva.textPointValueLong, longString)
                    .set(pva.sourceMessage, writeTranslatableMessage(sourceMessage))
                    .execute();
        }

        return id;
    }

    //
    //
    // Single point
    //

    private PointValueTime getPointValueAt(DataPointVO vo, Field<Long> time) {
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
        ;
        return getPointValueAt(vo, ts);
    }

    private PointValueTime getPointValue(long id) {
        return baseQuery().where(pv.id.equal(id))
                .limit(1)
                .fetchOne(this::mapRecord);
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

    //
    //
    // Values lists
    //

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

    //
    //
    // Query with callback
    //
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
        this.getPointValuesBetween(vo, from, to, (value) -> callback.row(value));
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

    class PointValueRowMapper implements RowMapper<PointValueTime> {
        @Override
        public PointValueTime mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataValue value = createDataValue(rs, 1);
            long time = rs.getLong(5);

            TranslatableMessage sourceMessage = BaseDao.readTranslatableMessage(rs, 6);
            if (sourceMessage == null)
                // No annotations, just return a point value.
                return new PointValueTime(value, time);

            // There was a source for the point value, so return an annotated version.
            return new AnnotatedPointValueTime(value, time, sourceMessage);
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


    //
    //
    // Multiple-point callback for point history replays
    //
    @Override
    public void getPointValuesBetween(List<DataPointVO> vos, long from, long to,
                                      Consumer<IdPointValueTime> callback) {
        List<Integer> dataPointIds = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
        String ids = createDelimitedList(dataPointIds, ",", null);
        query(ANNOTATED_POINT_ID_VALUE_SELECT + " where pv.dataPointId in (" + ids + ") and pv.ts >= ? and pv.ts<? order by ts",
                new Object[]{from, to}, new AnnotatedIdPointValueRowMapper(), callback);
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

    //
    //
    // Point value deletions
    //

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
        int limit = 1000;
        while (true) {
            List<Long> ids = this.create.select(pva.pointValueId)
                    .from(pva)
                    .leftJoin(pv)
                    .on(pv.id.equal(pva.pointValueId))
                    .where(pv.id.isNull())
                    .fetch(pva.pointValueId);

            if (ids.isEmpty())
                break;

            this.create.deleteFrom(pva)
                    .where(pva.pointValueId.in(ids))
                    .execute();

            if (ids.size() < limit)
                break;
        }
    }

    private long deletePointValues(DeleteLimitStep<PointValuesRecord> deleteLimitStep) {
        return deletePointValues(deleteLimitStep, 0, null);
    }

    private long deletePointValues(DeleteLimitStep<PointValuesRecord> deleteLimitStep, long chunkWait, Long limit) {
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
        clearUnsavedPointValues();
        return total;
    }

    /**
     * There WAS a bug here where the end date should be exclusive! The TCP Persistent publisher expects it to be
     * exclusive,
     * but as for what ramifications it will have to other modules who knows.
     * <p>
     * For example if one uses this method to count a range and then a select point values between, the results can be
     * different!
     * <p>
     * This has been changed to be exclusive of End time as the NoSQL DB uses exclusive queries and this needs to
     * match for the Persistent TCP Module to work across various Data stores.
     */
    @Override
    public long dateRangeCount(DataPointVO vo, long from, long to) {
        return ejt.queryForLong("select count(*) from pointValues where dataPointId=? and ts>=? and ts<?",
                new Object[]{vo.getSeriesId(), from, to}, 0L);
    }

    @Override
    public long getInceptionDate(DataPointVO vo) {
        return ejt
                .queryForLong("select min(ts) from pointValues where dataPointId=?", new Object[]{vo.getSeriesId()}, -1);
    }

    @Override
    public long getStartTime(List<DataPointVO> vos) {
        if (vos.isEmpty())
            return -1;
        List<Integer> ids = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
        return ejt.queryForLong("select min(ts) from pointValues where dataPointId in ("
                + createDelimitedList(ids, ",", null) + ")", null, 0L);
    }

    @Override
    public long getEndTime(List<DataPointVO> vos) {
        if (vos.isEmpty())
            return -1;
        List<Integer> ids = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
        return ejt.queryForLong("select max(ts) from pointValues where dataPointId in ("
                + createDelimitedList(ids, ",", null) + ")", null, -1L);
    }

    @Override
    public LongPair getStartAndEndTime(List<DataPointVO> vos) {
        if (vos.isEmpty())
            return null;
        List<Integer> ids = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
        return queryForObject(
                "select min(ts),max(ts) from pointValues where dataPointId in ("
                        + createDelimitedList(ids, ",", null) + ")", null, (rs, index) -> {
                    long l = rs.getLong(1);
                    if (rs.wasNull())
                        return null;
                    return new LongPair(l, rs.getLong(2));
                }, null);
    }

    @Override
    public List<Long> getFiledataIds(DataPointVO vo) {
        return queryForList("select id from pointValues where dataPointId=? and dataType=? ", new Object[]{
                vo.getSeriesId(), DataTypes.IMAGE}, Long.class);
    }

    /**
     * Class that stored point value data when it could not be saved to the database due to concurrency errors.
     *
     * @author Matthew Lohbihler
     */
    static class UnsavedPointValue {
        private final DataPointVO vo;
        private final PointValueTime pointValue;
        private final SetPointSource source;

        public UnsavedPointValue(DataPointVO vo, PointValueTime pointValue, SetPointSource source) {
            this.vo = vo;
            this.pointValue = pointValue;
            this.source = source;
        }

        public DataPointVO getVO() {
            return vo;
        }

        public PointValueTime getPointValue() {
            return pointValue;
        }

        public SetPointSource getSource() {
            return source;
        }
    }

    static class BatchWriteBehindEntry {
        private final DataPointVO vo;
        private final int dataType;
        private final double dvalue;
        private final long time;

        public BatchWriteBehindEntry(DataPointVO vo, int dataType, double dvalue, long time) {
            this.vo = vo;
            this.dataType = dataType;
            this.dvalue = dvalue;
            this.time = time;
        }
    }

    public static final String ENTRIES_MONITOR_ID = "com.serotonin.m2m2.db.dao.PointValueDao$BatchWriteBehind.ENTRIES_MONITOR";
    public static final String INSTANCES_MONITOR_ID = "com.serotonin.m2m2.db.dao.PointValueDao$BatchWriteBehind.INSTANCES_MONITOR";
    public static final String BATCH_WRITE_SPEED_MONITOR_ID = "com.serotonin.m2m2.db.dao.PointValueDao$BatchWriteBehind.BATCH_WRITE_SPEED_MONITOR";
    final static EventHistogram writesPerSecond = new EventHistogram(5000, 2);

    static class BatchWriteBehind implements WorkItem {
        private static final ObjectQueue<BatchWriteBehindEntry> ENTRIES = new ObjectQueue<>();
        private static final CopyOnWriteArrayList<BatchWriteBehind> instances = new CopyOnWriteArrayList<>();
        private static final Log LOG = LogFactory.getLog(BatchWriteBehind.class);
        private static final int SPAWN_THRESHOLD = 10000;
        private static final int MAX_INSTANCES = 5;
        private static final int MAX_ROWS;
        private static final ValueMonitor<Integer> ENTRIES_MONITOR = Common.MONITORED_VALUES.<Integer>create(ENTRIES_MONITOR_ID)
                .name(new TranslatableMessage("internal.monitor.BATCH_ENTRIES"))
                .value(0)
                .build();

        private static final ValueMonitor<Integer> INSTANCES_MONITOR = Common.MONITORED_VALUES.<Integer>create(INSTANCES_MONITOR_ID)
                .name(new TranslatableMessage("internal.monitor.BATCH_INSTANCES"))
                .value(0)
                .build();

        //TODO Create ValueMonitor<Double> but will need to upgrade the Internal data source to do this
        private static final ValueMonitor<Integer> BATCH_WRITE_SPEED_MONITOR = Common.MONITORED_VALUES.<Integer>create(BATCH_WRITE_SPEED_MONITOR_ID)
                .name(new TranslatableMessage("internal.monitor.BATCH_WRITE_SPEED_MONITOR"))
                .value(0)
                .build();

        private static final List<Class<? extends RuntimeException>> retriedExceptions = new ArrayList<>();

        static {
            if (Common.databaseProxy.getType() == DatabaseType.DERBY)
                // This has not been tested to be optimal
                MAX_ROWS = 1000;
            else if (Common.databaseProxy.getType() == DatabaseType.H2)
                // This has not been tested to be optimal
                MAX_ROWS = 1000;
            else if (Common.databaseProxy.getType() == DatabaseType.MSSQL)
                // MSSQL has max rows of 1000, and max parameters of 2100. In this case that works out to...
                MAX_ROWS = 524;
            else if (Common.databaseProxy.getType() == DatabaseType.MYSQL)
                // This appears to be an optimal value
                MAX_ROWS = 2000;
            else if (Common.databaseProxy.getType() == DatabaseType.POSTGRES)
                // This appears to be an optimal value
                MAX_ROWS = 2000;
            else
                throw new ShouldNeverHappenException("Unknown database type: " + Common.databaseProxy.getType());

            retriedExceptions.add(RecoverableDataAccessException.class);
            retriedExceptions.add(TransientDataAccessException.class);
            retriedExceptions.add(TransientDataAccessResourceException.class);
            retriedExceptions.add(CannotGetJdbcConnectionException.class);
        }

        static void add(BatchWriteBehindEntry e, DSLContext create) {
            synchronized (ENTRIES) {
                ENTRIES.push(e);
                ENTRIES_MONITOR.setValue(ENTRIES.size());
                if (ENTRIES.size() > instances.size() * SPAWN_THRESHOLD) {
                    if (instances.size() < MAX_INSTANCES) {
                        BatchWriteBehind bwb = new BatchWriteBehind(create);
                        instances.add(bwb);
                        INSTANCES_MONITOR.setValue(instances.size());
                        try {
                            Common.backgroundProcessing.addWorkItem(bwb);
                        } catch (RejectedExecutionException ree) {
                            instances.remove(bwb);
                            INSTANCES_MONITOR.setValue(instances.size());
                            throw ree;
                        }
                    }
                }
            }
        }

        private final DSLContext create;

        public BatchWriteBehind(DSLContext create) {
            this.create = create;
        }

        @Override
        public void execute() {
            try {
                BatchWriteBehindEntry[] inserts;
                while (true) {
                    synchronized (ENTRIES) {
                        if (ENTRIES.size() == 0)
                            break;

                        inserts = new BatchWriteBehindEntry[Math.min(ENTRIES.size(), MAX_ROWS)];
                        ENTRIES.pop(inserts);
                        ENTRIES_MONITOR.setValue(ENTRIES.size());
                    }

                    // Create the sql and parameters

                    PointValues pv = PointValues.POINT_VALUES;
                    InsertValuesStep4<PointValuesRecord, Integer, Integer, Double, Long> insert = this.create.insertInto(pv)
                            .columns(pv.dataPointId, pv.dataType, pv.pointValue, pv.ts);

                    for (BatchWriteBehindEntry entry : inserts) {
                        insert.values(entry.vo.getSeriesId(), entry.dataType, entry.dvalue, entry.time);
                    }

                    // Insert the data
                    int retries = 10;
                    while (true) {
                        try {
                            insert.execute();
                            writesPerSecond.hitMultiple(inserts.length);
                            BATCH_WRITE_SPEED_MONITOR.setValue(writesPerSecond.getEventCounts()[0] / 5);
                            break;
                        } catch (RuntimeException e) {
                            if (retriedExceptions.contains(e.getClass())) {
                                if (retries <= 0) {
                                    LOG.error("Concurrency failure saving " + inserts.length
                                            + " batch inserts after 10 tries. Data lost.");
                                    break;
                                }

                                int wait = (10 - retries) * 100;
                                try {
                                    if (wait > 0) {
                                        synchronized (this) {
                                            wait(wait);
                                        }
                                    }
                                } catch (InterruptedException ie) {
                                    // no op
                                }

                                retries--;
                            } else {
                                LOG.error("Error saving " + inserts.length + " batch inserts. Data lost.", e);
                                break;
                            }
                        }
                    }
                }
            } finally {
                instances.remove(this);
                INSTANCES_MONITOR.setValue(instances.size());
            }
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_HIGH;
        }

        @Override
        public String getDescription() {
            return "Batch Writing from batch of size: " + ENTRIES.size();
        }

        @Override
        public String getTaskId() {
            return "BWB";
        }

        @Override
        public int getQueueSize() {
            return 0;
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            instances.remove(this);
            INSTANCES_MONITOR.setValue(instances.size());
        }
    }

}
