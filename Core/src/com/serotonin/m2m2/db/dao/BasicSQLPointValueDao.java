/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Condition;
import org.jooq.DeleteConditionStep;
import org.jooq.DeleteLimitStep;
import org.jooq.DeleteUsingStep;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.ResultQuery;
import org.jooq.Select;
import org.jooq.SelectOnConditionStep;
import org.jooq.SelectUnionStep;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.db.query.WideCallback;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.PointValueAnnotations;
import com.infiniteautomation.mango.db.tables.PointValues;
import com.infiniteautomation.mango.db.tables.records.PointValueAnnotationsRecord;
import com.infiniteautomation.mango.db.tables.records.PointValuesRecord;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.pointvalue.AggregateDao;
import com.serotonin.m2m2.db.dao.pointvalue.DefaultAggregateDao;
import com.serotonin.m2m2.db.dao.pointvalue.StartAndEndTime;
import com.serotonin.m2m2.db.dao.pointvalue.TimeOrder;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedIdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime.MetaIdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * Basic implementation with no insert capabilities, no batch writer functionality.
 */
public class BasicSQLPointValueDao extends BaseDao implements PointValueDao {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final PointValueAnnotations pva = PointValueAnnotations.POINT_VALUE_ANNOTATIONS;
    protected final PointValues pv = PointValues.POINT_VALUES;
    protected final DataPoints dp = DataPoints.DATA_POINTS;
    protected final DefaultAggregateDao aggregateDao;

    public BasicSQLPointValueDao(DatabaseProxy databaseProxy) {
        super(databaseProxy);
        this.aggregateDao = new DefaultAggregateDao(this);
    }

    @Override
    public AggregateDao getAggregateDao() {
        return aggregateDao;
    }

    @Override
    public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateNotNull(pointValue);
        throw new UnsupportedOperationException();
    }

    @Override
    public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateNotNull(pointValue);
        throw new UnsupportedOperationException();
    }

    protected PointValueTime getPointValue(long id) {
        return baseQuery().where(pv.id.equal(id))
                .limit(1)
                .fetchOne(this::mapRecord);
    }

    protected Optional<PointValueTime> getPointValueAt(DataPointVO vo, Field<Long> time) {
        return baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.equal(time))
                .limit(1)
                .fetchOptional(this::mapRecord);
    }

    @Override
    public Optional<PointValueTime> getLatestPointValue(DataPointVO vo) {
        PointValueDao.validateNotNull(vo);
        Field<Long> ts = this.create.select(DSL.max(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public Optional<PointValueTime> getPointValueBefore(DataPointVO vo, long time) {
        PointValueDao.validateNotNull(vo);
        Field<Long> ts = this.create.select(DSL.max(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.lessThan(time))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public Optional<PointValueTime> getPointValueAt(DataPointVO vo, long time) {
        PointValueDao.validateNotNull(vo);
        return getPointValueAt(vo, DSL.val(time));
    }

    @Override
    public Optional<PointValueTime> getPointValueAfter(DataPointVO vo, long time) {
        PointValueDao.validateNotNull(vo);

        Field<Long> ts = this.create.select(DSL.min(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(time))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public List<PointValueTime> getPointValues(DataPointVO vo, long from) {
        PointValueDao.validateNotNull(vo);

        return baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(from))
                .orderBy(pv.ts)
                .fetch(this::mapRecord);
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
        return betweenQuery(from, to, null, TimeOrder.ASCENDING, pv.dataPointId.eq(vo.getSeriesId()))
                .fetch(this::mapRecord);
    }

    @Override
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateNotNull(limit);

        if (limit == 0) {
            return Collections.emptyList();
        } else if (limit == 1) {
            Optional<PointValueTime> pvt = getLatestPointValue(vo);
            return pvt.map(Collections::singletonList).orElse(Collections.emptyList());
        } else {
            return baseQuery()
                    .where(pv.dataPointId.equal(vo.getSeriesId()))
                    .orderBy(pv.ts.desc())
                    .limit(limit)
                    .fetch(this::mapRecord);
        }
    }

    @Override
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, long to, int limit) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateLimit(limit);

        if (limit == 0) {
            return Collections.emptyList();
        } else {
            return baseQuery()
                    .where(pv.dataPointId.equal(vo.getSeriesId()))
                    .and(pv.ts.lessThan(to))
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
    public void getPointValuesBetween(DataPointVO vo, long from, long to, Consumer<? super PointValueTime> callback) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateNotNull(callback);

        ResultQuery<Record> result = betweenQuery(from, to, null, TimeOrder.ASCENDING, pv.dataPointId.eq(vo.getSeriesId()));
        try (Stream<Record> stream = result.stream()) {
            stream.map(this::mapRecord).forEach(callback);
        }
    }


    private Select<Record> betweenQuery(@Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder timeOrder, Condition seriesIdCondition) {
        var query = baseQuery().where(seriesIdCondition);
        if (from != null) {
            query = query.and(pv.ts.greaterOrEqual(from));
        }
        if (to != null) {
            query = query.and(pv.ts.lessThan(to));
        }
        return query.orderBy(timeOrder == TimeOrder.ASCENDING ? pv.ts.asc() : pv.ts.desc()).limit(limit);
    }

    private SelectUnionStep<Record> firstValueQuery(long from, Field<Integer> seriesId) {
        return baseQuery().where(pv.dataPointId.eq(seriesId))
                .and(pv.ts.lessOrEqual(from))
                .orderBy(pv.ts.desc())
                .limit(1);
    }

    @Override
    public void getPointValuesPerPoint(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder sortOrder, Consumer<? super IdPointValueTime> callback) {
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(callback);
        PointValueDao.validateNotNull(sortOrder);
        if (vos.isEmpty() || limit != null && limit == 0) return;

        //Limit results of each data point to size limit, i.e. loop over all points and query with limit
        var query = betweenQuery(from, to, limit, sortOrder, pv.dataPointId.eq(DSL.param("seriesId", Integer.class)));
        try (var queryKept = query.keepStatement(true)) {
            for (DataPointVO vo : vos) {
                try (var cursor = queryKept.bind("seriesId", vo.getSeriesId()).fetchLazy()) {
                    for (var record : cursor) {
                        callback.accept(mapRecord(record));
                    }
                }
            }
        }
    }

    @Override
    public void getPointValuesCombined(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder sortOrder, Consumer<? super IdPointValueTime> callback) {
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(callback);
        PointValueDao.validateNotNull(sortOrder);
        if (vos.isEmpty() || limit != null && limit == 0) return;

        var query = betweenQuery(from, to, limit, sortOrder, seriesIdCondition(vos));
        try (var cursor = query.fetchLazy()) {
            for (var record : cursor) {
                callback.accept(mapRecord(record));
            }
        }
    }

    @Override
    public Stream<IdPointValueTime> streamPointValues(DataPointVO vo, @Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder sortOrder, int chunkSize) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateNotNull(sortOrder);

        var query = betweenQuery(from, to, limit, sortOrder, pv.dataPointId.eq(vo.getSeriesId()));
        return query.stream().map(this::mapRecord);
    }

    @Override
    public Stream<IdPointValueTime> streamPointValuesCombined(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder sortOrder) {
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateNotNull(sortOrder);
        if (vos.isEmpty()) return Stream.empty();

        var query = betweenQuery(from, to, limit, sortOrder, seriesIdCondition(vos));
        return query.stream().map(this::mapRecord);
    }

    @Override
    public Map<Integer, IdPointValueTime> initialValues(Collection<? extends DataPointVO> vos, long time) {
        PointValueDao.validateNotNull(vos);
        Map<Integer, IdPointValueTime> values = new LinkedHashMap<>(vos.size());

        try (var cursor = vos.stream()
                .map(DataPointVO::getSeriesId)
                .map(seriesId -> firstValueQuery(time, DSL.val(seriesId)))
                .reduce(SelectUnionStep::union)
                .orElseThrow()
                .fetchLazy()) {
            for (var record : cursor) {
                var value = mapRecord(record);
                values.put(value.getSeriesId(), value.withNewTime(time));
            }
        }
        /* alternative implementation without union
        try (var query = firstValueQuery(from, DSL.param("seriesId", Integer.class)).keepStatement(true)) {
            for (DataPointVO vo : vos) {
                query.bind("seriesId", vo.getSeriesId())
                        .fetchOptional(this::mapRecord)
                        .ifPresent(v -> values.put(v.getSeriesId(), v));
            }
        }
        */
        for (DataPointVO vo : vos) {
            values.computeIfAbsent(vo.getSeriesId(), seriesId -> new MetaIdPointValueTime(seriesId, null, time, true, false));
        }
        return values;
    }

    @Override
    public void wideBookendQueryPerPoint(Collection<? extends DataPointVO> vos, long from, long to, @Nullable Integer limit, WideCallback<? super IdPointValueTime> callback) {
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(callback);
        if (vos.isEmpty()) return;

        Map<Integer, IdPointValueTime> values = initialValues(vos, from);
        try (var query = betweenQuery(from, to, limit, TimeOrder.ASCENDING, pv.dataPointId.eq(DSL.param("seriesId", Integer.class))).keepStatement(true)) {
            for (DataPointVO vo : vos) {
                var value = values.get(vo.getSeriesId());
                callback.firstValue(value, value.isBookend());
                try (var cursor = query.bind("seriesId", vo.getSeriesId()).fetchLazy()) {
                    for (var record : cursor) {
                        value = mapRecord(record);
                        // so we don't call row() for same value that was passed to firstValue()
                        if (value.getTime() > from) {
                            callback.accept(value);
                        }
                    }
                }
                callback.lastValue(value.withNewTime(to), true);
            }
        }
    }

    @Override
    public void wideBookendQueryCombined(Collection<? extends DataPointVO> vos, long from, long to, @Nullable Integer limit, WideCallback<? super IdPointValueTime> callback) {
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(callback);
        if (vos.isEmpty()) return;

        Map<Integer, IdPointValueTime> values = initialValues(vos, from);
        for (IdPointValueTime value : values.values()) {
            callback.firstValue(value, value.isBookend());
        }
        var query = betweenQuery(from, to, limit, TimeOrder.ASCENDING, seriesIdCondition(vos));
        try (var cursor = query.fetchLazy()) {
            for (var record : cursor) {
                var value = mapRecord(record);
                values.put(value.getSeriesId(), value);
                // so we don't call row() for same value that was passed to firstValue()
                if (value.getTime() > from) {
                    callback.accept(value);
                }
            }
        }
        for (IdPointValueTime value : values.values()) {
            callback.lastValue(value.withNewTime(to), true);
        }
    }

    public DataValue createDataValue(PointValuesRecord pvRecord, PointValueAnnotationsRecord pvaRecord) {
        DataType dataType = DataType.fromId(pvRecord.get(pv.dataType));
        if (dataType == null) return null;

        switch (dataType) {
            case NUMERIC: {
                Double doubleValue = pvRecord.get(pv.pointValue);
                return new NumericValue(doubleValue);
            }
            case BINARY: {
                boolean booleanValue = pvRecord.get(pv.pointValue) == 1;
                return new BinaryValue(booleanValue);
            }
            case MULTISTATE: {
                int intValue = (int) Math.round(pvRecord.get(pv.pointValue));
                return new MultistateValue(intValue);
            }
            case ALPHANUMERIC: {
                String shortTextValue = pvaRecord.get(pva.textPointValueShort);
                if (shortTextValue != null) {
                    return new AlphanumericValue(shortTextValue);
                }
                String longTextValue = pvaRecord.get(pva.textPointValueLong);
                return new AlphanumericValue(longTextValue);
            }
        }
        return null;
    }

    @Override
    public void getPointValuesBetween(Collection<? extends DataPointVO> vos, long from, long to, Consumer<? super IdPointValueTime> callback) {
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateNotNull(callback);
        try (var stream = betweenQuery(from, to, null, TimeOrder.ASCENDING, seriesIdCondition(vos)).stream()) {
            stream.map(this::mapRecord).forEach(callback);
        }
    }

    public DeleteUsingStep<PointValuesRecord> baseDelete() {
        return this.create.deleteFrom(pv);
    }

    @Override
    public Optional<Long> deletePointValue(DataPointVO vo, long ts) {
        PointValueDao.validateNotNull(vo);
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .and(pv.ts.eq(ts));
        return Optional.of(deletePointValues(delete));
    }

    @Override
    public Optional<Long> deletePointValuesBefore(DataPointVO vo, long endTime) {
        PointValueDao.validateNotNull(vo);
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .and(pv.ts.lessThan(endTime));
        return Optional.of(deletePointValues(delete));
    }

    @Override
    public Optional<Long> deletePointValuesBetween(DataPointVO vo, @Nullable Long startTime, @Nullable Long endTime) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(startTime, endTime);
        var delete = baseDelete()
                .where(pv.dataPointId.eq(vo.getSeriesId()));
        if (startTime != null) {
            delete = delete.and(pv.ts.greaterOrEqual(startTime));
        }
        if (endTime != null) {
            delete = delete.and(pv.ts.lessThan(endTime));
        }
        return Optional.of(deletePointValues(delete));
    }

    @Override
    public Optional<Long> deletePointValues(DataPointVO vo) {
        PointValueDao.validateNotNull(vo);
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.eq(vo.getSeriesId()));
        return Optional.of(deletePointValues(delete));
    }

    @Override
    public Optional<Long> deleteAllPointData() {
        return Optional.of(deletePointValues(baseDelete()));
    }

    @Override
    public Optional<Long> deleteOrphanedPointValues() {
        DeleteConditionStep<PointValuesRecord> delete = baseDelete()
                .where(pv.dataPointId.notIn(
                        this.create.select(dp.seriesId).from(dp)
                ));
        Optional<Long> result = Optional.of(deletePointValues(delete, 5000L, 100000L));
        deleteOrphanedPointValueAnnotations();
        return result;
    }

    private void deleteOrphanedPointValueAnnotations() {
        int limit = databaseProxy.batchDeleteSize();
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
        int batchSize = databaseProxy.batchDeleteSize();
        if (batchSize <= 0) {
            return deleteLimitStep.execute();
        }

        long total = 0;
        // H2: this is really slow on H2, about 1 second to delete 1000 point values
        // H2: jOOQ creates a sub-query on row-id for delete with a limit
        try (var statement = deleteLimitStep.limit(DSL.param("limit", Integer.class)).keepStatement(true)) {
            while (true) {
                int count = statement.bind("limit", batchSize).execute();
                total += count;

                if (count < batchSize || (limit != null && total >= limit))
                    break;

                // park the thread for the specified amount of time
                if (chunkWait > 0) {
                    LockSupport.parkNanos(chunkWait * 1_000_000);
                }
            }
        }
        return total;
    }

    @Override
    public long dateRangeCount(DataPointVO vo, @Nullable Long from, @Nullable Long to) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
        var select = create.select(DSL.count())
                .from(pv)
                .where(pv.dataPointId.eq(vo.getSeriesId()));
        if (from != null) {
            select = select.and(pv.ts.greaterOrEqual(from));
        }
        if (to != null) {
            select = select.and(pv.ts.lessThan(to));
        }
        return select.fetchOptional()
                .map(Record1::value1)
                .orElse(0);
    }

    @Override
    public Optional<Long> getInceptionDate(DataPointVO vo) {
        PointValueDao.validateNotNull(vo);
        return create.select(DSL.min(pv.ts))
                .from(pv)
                .where(pv.dataPointId.eq(vo.getSeriesId()))
                .fetchOptional()
                .map(Record1::value1);
    }

    private Condition seriesIdCondition(Collection<? extends DataPointVO> vos) {
        List<Integer> seriesIds = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
        return pv.dataPointId.in(seriesIds);
    }

    @Override
    public Optional<Long> getStartTime(Collection<? extends DataPointVO> vos) {
        PointValueDao.validateNotNull(vos);
        if (vos.isEmpty()) return Optional.empty();
        return create.select(DSL.min(pv.ts)).from(pv).where(seriesIdCondition(vos))
                .fetchOptional()
                .map(Record1::value1);
    }

    @Override
    public Optional<Long> getEndTime(Collection<? extends DataPointVO> vos) {
        PointValueDao.validateNotNull(vos);
        if (vos.isEmpty()) return Optional.empty();
        return create.select(DSL.max(pv.ts)).from(pv).where(seriesIdCondition(vos))
                .fetchOptional()
                .map(Record1::value1);
    }

    @Override
    public Optional<StartAndEndTime> getStartAndEndTime(Collection<? extends DataPointVO> vos) {
        PointValueDao.validateNotNull(vos);
        if (vos.isEmpty()) return Optional.empty();
        return create.select(DSL.min(pv.ts), DSL.max(pv.ts)).from(pv).where(seriesIdCondition(vos))
                .fetchOptional()
                .map(record -> new StartAndEndTime(record.value1(), record.value2()));
    }

    @Override
    public double writeSpeed() {
        // this implementation does not support writes
        return 0D;
    }

    @Override
    public long queueSize() {
        // this implementation does not support writes
        return 0L;
    }

    @Override
    public int threadCount() {
        // this implementation does not support writes
        return 0;
    }
}
