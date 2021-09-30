/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.SelectUnionStep;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.PointValueAnnotations;
import com.infiniteautomation.mango.db.tables.PointValues;
import com.infiniteautomation.mango.db.tables.records.PointValueAnnotationsRecord;
import com.infiniteautomation.mango.db.tables.records.PointValuesRecord;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedIdPointValueTime;
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

    protected Optional<PointValueTime> getPointValueAt(DataPointVO vo, Field<Long> time) {
        return baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.equal(time))
                .limit(1)
                .fetchOptional(this::mapRecord);
    }

    @Override
    public Optional<PointValueTime> getLatestPointValue(DataPointVO vo) {
        Field<Long> ts = this.create.select(DSL.max(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public Optional<PointValueTime> getPointValueBefore(DataPointVO vo, long time) {
        Field<Long> ts = this.create.select(DSL.max(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.lessThan(time))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public Optional<PointValueTime> getPointValueAt(DataPointVO vo, long time) {
        return getPointValueAt(vo, DSL.val(time));
    }

    @Override
    public Optional<PointValueTime> getPointValueAfter(DataPointVO vo, long time) {
        Field<Long> ts = this.create.select(DSL.min(pv.ts))
                .from(pv)
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(time))
                .asField();
        return getPointValueAt(vo, ts);
    }

    @Override
    public List<PointValueTime> getPointValues(DataPointVO vo, long from) {
        return baseQuery()
                .where(pv.dataPointId.equal(vo.getSeriesId()))
                .and(pv.ts.greaterOrEqual(from))
                .orderBy(pv.ts)
                .fetch(this::mapRecord);
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to) {
        return betweenQuery(from, to, null, vo)
                .fetch(this::mapRecord);
    }

    @Override
    public List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to, Integer limit) {
        return betweenQuery(from, to, limit, vo)
                .fetch(this::mapRecord);
    }

    @Override
    public List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit) {
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
    public void getLatestPointValuesPerPoint(Collection<? extends DataPointVO> vos, Long to, int limit, PVTQueryCallback<? super IdPointValueTime> callback) {
        checkLimit(limit); checkNull(vos); checkNull(callback);
        if (vos.isEmpty() || limit == 0) return;

        Integer[] seriesIds = vos.stream().mapToInt(DataPointVO::getSeriesId).sorted().boxed().toArray(Integer[]::new);

        SelectUnionStep<Record> union = null;
        for (int seriesId : seriesIds) {
            Condition condition = pv.dataPointId.equal(seriesId);
            if (to != null) {
                condition = condition.and(pv.ts.lessThan(to));
            }

            SelectLimitPercentStep<Record> r = baseQuery()
                    .where(condition)
                    .orderBy(pv.ts.desc())
                    .limit(limit);

            union = union == null ? r : union.unionAll(r);
        }

        try (Stream<Record> stream = Objects.requireNonNull(union).stream()) {
            stream.map(this::mapRecord).forEach(callback);
        }
    }

    @Override
    public void getLatestPointValuesCombined(Collection<? extends DataPointVO> vos, Long to, int limit, PVTQueryCallback<? super IdPointValueTime> callback) {
        checkLimit(limit); checkNull(vos); checkNull(callback);
        if (vos.isEmpty() || limit == 0) return;

        Integer[] seriesIds = vos.stream().mapToInt(DataPointVO::getSeriesId).sorted().boxed().toArray(Integer[]::new);
        Condition condition = pv.dataPointId.in(seriesIds);
        if (to != null) {
            condition = condition.and(pv.ts.lessThan(to));
        }

        SelectConditionStep<Record> conditionStep = baseQuery()
                .where(condition);

        try (Stream<Record> stream = conditionStep.orderBy(pv.ts.desc()).limit(limit).stream()) {
            stream.map(this::mapRecord).forEach(callback);
        }
    }

    @Override
    public void getPointValuesBetween(DataPointVO vo, long from, long to, PVTQueryCallback<? super PointValueTime> callback) {
        ResultQuery<Record> result = betweenQuery(from, to, null, vo);
        try (Stream<Record> stream = result.stream()) {
            stream.map(this::mapRecord).forEach(callback);
        }
    }

    private Select<Record> betweenQuery(long from, long to, @Nullable Integer limit, DataPointVO point) {
        return betweenQuery(from, to, limit, pv.dataPointId.eq(point.getSeriesId()));
    }

    private Select<Record> betweenQuery(long from, long to, @Nullable Integer limit, Field<Integer> seriesId) {
        return betweenQuery(from, to, limit, pv.dataPointId.eq(seriesId));
    }

    private Select<Record> betweenQuery(long from, long to, @Nullable Integer limit, Collection<? extends DataPointVO> points) {
        return betweenQuery(from, to, limit, seriesIdCondition(points));
    }

    private Select<Record> betweenQuery(long from, long to, @Nullable Integer limit, Condition seriesIdCondition) {
        var query = baseQuery().where(seriesIdCondition)
                .and(pv.ts.greaterOrEqual(from))
                .and(pv.ts.lessThan(to))
                .orderBy(pv.ts.asc());
        return query.limit(limit);
    }

    private SelectUnionStep<Record> firstValueQuery(long from, Field<Integer> seriesId) {
        return baseQuery().where(pv.dataPointId.eq(seriesId))
                .and(pv.ts.lessOrEqual(from))
                .orderBy(pv.ts.desc())
                .limit(1);
    }

    @Override
    public void getPointValuesBetween(Collection<? extends DataPointVO> vos, long from, long to, boolean orderById, Integer limit, PVTQueryCallback<? super IdPointValueTime> callback) {
        if (vos.isEmpty()) return;

        if (orderById) {
            //Limit results of each data point to size limit, i.e. loop over all points and query with limit
            var query = betweenQuery(from, to, limit, DSL.param("seriesId", Integer.class));
            try (var queryKept = query.keepStatement(true)) {
                for (DataPointVO vo : vos) {
                    try (var cursor = queryKept.bind("seriesId", vo.getSeriesId()).fetchLazy()) {
                        for (var record : cursor) {
                            callback.row(mapRecord(record));
                        }
                    }
                }
            }
        } else {
            var query = betweenQuery(from, to, limit, vos);
            try (var cursor = query.fetchLazy()) {
                for (var record : cursor) {
                    callback.row(mapRecord(record));
                }
            }
        }
    }

    private Map<Integer, IdPointValueTime> initialValues(Collection<? extends DataPointVO> vos, long from) {
        Map<Integer, IdPointValueTime> values = new HashMap<>(vos.size());

        try (var cursor = vos.stream()
                .map(DataPointVO::getSeriesId)
                .map(seriesId -> firstValueQuery(from, DSL.val(seriesId)))
                .reduce(SelectUnionStep::union)
                .orElseThrow()
                .fetchLazy()) {
            for (var record : cursor) {
                var value = mapRecord(record);
                values.put(value.getSeriesId(), value);
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
            values.computeIfAbsent(vo.getSeriesId(), seriesId -> new IdPointValueTime(seriesId, null, from));
        }
        return values;
    }

    @Override
    public void wideBookendQuery(Collection<? extends DataPointVO> vos, long from, long to, boolean orderById, Integer limit, BookendQueryCallback<? super IdPointValueTime> callback) {
        if (vos.isEmpty()) return;

        Map<Integer, IdPointValueTime> values = initialValues(vos, from);
        if (orderById) {
            try (var query = betweenQuery(from, to, limit, DSL.param("seriesId", Integer.class)).keepStatement(true)) {
                for (DataPointVO vo : vos) {
                    var value = values.get(vo.getSeriesId());
                    callback.firstValue(value.withNewTime(from), value.getValue() == null || value.getTime() != from);
                    try (var cursor = query.bind("seriesId", vo.getSeriesId()).fetchLazy()) {
                        for (var record : cursor) {
                            value = mapRecord(record);
                            var previousValue = Objects.requireNonNull(values.put(value.getSeriesId(), value));
                            // so we don't call row() for same value that was passed to firstValue()
                            if (value.getTime() > previousValue.getTime()) {
                                callback.row(value);
                            }
                        }
                    }
                    callback.lastValue(value.withNewTime(to), true);
                }
            }
        } else {
            for (IdPointValueTime value : values.values()) {
                callback.firstValue(value.withNewTime(from), value.getValue() == null || value.getTime() != from);
            }
            var query = betweenQuery(from, to, limit, vos);
            try (var cursor = query.fetchLazy()) {
                for (var record : cursor) {
                    var value = mapRecord(record);
                    var previousValue = Objects.requireNonNull(values.put(value.getSeriesId(), value));
                    // so we don't call row() for same value that was passed to firstValue()
                    if (value.getTime() > previousValue.getTime()) {
                        callback.row(value);
                    }
                }
            }
            for (IdPointValueTime value : values.values()) {
                callback.lastValue(value.withNewTime(to), true);
            }
        }
    }

    public DataValue createDataValue(PointValuesRecord pvRecord, PointValueAnnotationsRecord pvaRecord) {
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
    public void getPointValuesBetween(Collection<? extends DataPointVO> vos, long from, long to, PVTQueryCallback<? super IdPointValueTime> callback) {
        try (var stream = betweenQuery(from, to, null, vos).stream()) {
            stream.map(this::mapRecord).forEach(callback);
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

    private Condition seriesIdCondition(Collection<? extends DataPointVO> vos) {
        List<Integer> seriesIds = vos.stream().map(DataPointVO::getSeriesId).collect(Collectors.toList());
        return pv.dataPointId.in(seriesIds);
    }

    @Override
    public long getStartTime(Collection<? extends DataPointVO> vos) {
        if (vos.isEmpty())
            return -1L;
        return create.select(DSL.min(pv.ts)).from(pv).where(seriesIdCondition(vos))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(-1L);
    }

    @Override
    public long getEndTime(Collection<? extends DataPointVO> vos) {
        if (vos.isEmpty())
            return -1L;
        return create.select(DSL.max(pv.ts)).from(pv).where(seriesIdCondition(vos))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(-1L);
    }

    @Override
    public LongPair getStartAndEndTime(Collection<? extends DataPointVO> vos) {
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
