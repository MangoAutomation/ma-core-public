/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import com.codahale.metrics.Meter;
import com.infiniteautomation.mango.db.iterators.ChunkingSpliterator;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.DataSources;
import com.infiniteautomation.mango.db.tables.PointValues;
import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.IAnnotated;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;
import com.serotonin.metrics.EventHistogram;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.util.queue.ObjectQueue;

public class PointValueDaoSQL extends BasicSQLPointValueDao {

    private final ConcurrentLinkedQueue<UnsavedPointValue> unsavedPointValues = new ConcurrentLinkedQueue<>();

    public static final String SYNC_INSERTS_SPEED_COUNTER_ID = "com.serotonin.m2m2.db.dao.PointValueDaoSQL.SYNC_INSERTS_SPEED_COUNTER";
    public static final String ASYNC_INSERTS_SPEED_COUNTER_ID = "com.serotonin.m2m2.db.dao.PointValueDaoSQL.ASYNC_INSERTS_SPEED_COUNTER";

    private static final int SPAWN_THRESHOLD = 10000;
    private static final int MAX_INSTANCES = 5;

    private static final List<Class<? extends RuntimeException>> RETRIED_EXCEPTIONS = List.of(
            RecoverableDataAccessException.class,
            TransientDataAccessException.class,
            TransientDataAccessResourceException.class,
            CannotGetJdbcConnectionException.class
    );

    private final EventHistogram syncCallsCounter = new EventHistogram(5000, 2);
    private final EventHistogram asyncCallsCounter = new EventHistogram(5000, 2);
    private final Meter writeMeter = new Meter();

    private final ObjectQueue<BatchWriteEntry> entries = new ObjectQueue<>();
    private final CopyOnWriteArrayList<BatchWriteTask> instances = new CopyOnWriteArrayList<>();

    private final ValueMonitor<Integer> syncInsertsSpeedCounter;
    private final ValueMonitor<Integer> asyncInsertsSpeedCounter;
    private volatile long queueSize = 0;
    private volatile int threadCount = 0;
    private final int batchInsertSize;

    private final SystemSettingsDao systemSettingsDao;
    private final DataPointDao dataPointDao;

    public PointValueDaoSQL(DatabaseProxy databaseProxy, MonitoredValues monitoredValues,
                            SystemSettingsDao systemSettingsDao, DataPointDao dataPointDao) {
        super(databaseProxy);
        this.systemSettingsDao = systemSettingsDao;

        this.syncInsertsSpeedCounter = monitoredValues.<Integer>create(SYNC_INSERTS_SPEED_COUNTER_ID)
                .name(new TranslatableMessage("internal.monitor.SYNC_INSERTS_SPEED_COUNTER_ID"))
                .value(0)
                .build();
        this.asyncInsertsSpeedCounter = monitoredValues.<Integer>create(ASYNC_INSERTS_SPEED_COUNTER_ID)
                .name(new TranslatableMessage("internal.monitor.ASYNC_INSERTS_SPEED_COUNTER_ID"))
                .value(0)
                .build();

        this.batchInsertSize = databaseProxy.batchSize();
        this.dataPointDao = dataPointDao;
    }

    @Override
    public void savePointValues(Stream<? extends BatchPointValue<? extends PointValueTime>> pointValues) {
        PointValueDao.validateNotNull(pointValues);
        var stream = pointValues.map(v -> {
            var point = v.getPoint();
            var pointValue = v.getValue();
            var dataType = pointValue.getValue().getDataType();

            // can't batch save annotations or ALPHANUMERIC point values
            if (pointValue instanceof IAnnotated || dataType == DataType.ALPHANUMERIC) {
                savePointValueImpl(point, pointValue, false);
                syncCallsCounter.hit();
                syncInsertsSpeedCounter.setValue(syncCallsCounter.getEventCounts()[0] / 5);
                return null;
            }

            return v;
        }).filter(Objects::nonNull).map(v -> {
            var point = v.getPoint();
            var pointValue = v.getValue();
            var dataType = pointValue.getValue().getDataType();
            double boundedValue = databaseProxy.applyBounds(pointValue.getDoubleValue());
            return new BatchWriteEntry(point.getSeriesId(), dataType, boundedValue, pointValue.getTime());
        });

        ChunkingSpliterator.chunkStream(stream, chunkSize()).forEach(chunk -> {
            int count = writeMultiple(chunk.stream());
            syncCallsCounter.hitMultiple(count);
            syncInsertsSpeedCounter.setValue(syncCallsCounter.getEventCounts()[0] / 5);
        });
    }

    @Override
    public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue) {
        syncCallsCounter.hit();
        syncInsertsSpeedCounter.setValue(syncCallsCounter.getEventCounts()[0] / 5);
        long id = savePointValueImpl(vo, pointValue, false);

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

    @Override
    public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue) {
        savePointValueImpl(vo, pointValue, true);
        asyncCallsCounter.hit();
        asyncInsertsSpeedCounter.setValue(asyncCallsCounter.getEventCounts()[0] / 5);
    }

    @Override
    public double writeSpeed() {
        return writeMeter.getOneMinuteRate();
    }

    @Override
    public long queueSize() {
        return queueSize;
    }

    @Override
    public int threadCount() {
        return threadCount;
    }

    @Override
    public boolean enablePerPointPurge() {
        return systemSettingsDao.getBooleanValue(SystemSettingsDao.ENABLE_POINT_DATA_PURGE_PER_POINT);
    }

    @Override
    public List<PointHistoryCount> topPointHistoryCounts(int limit) {
        PointValueDao.validateLimit(limit);

        DataPoints points = DataPoints.DATA_POINTS;
        DataSources dataSources = DataSources.DATA_SOURCES;
        Field<Integer> count = DSL.count().as("count");

        return create.select(count)
                .select(dataPointDao.getSelectFields())
                .from(pv)
                .innerJoin(points).on(points.id.eq(pv.dataPointId))
                .leftJoin(dataSources).on(dataSources.id.eq(points.dataSourceId))
                .groupBy(points.id, dataSources.name, dataSources.xid, dataSources.dataSourceType)
                .orderBy(count.desc())
                .limit(limit)
                .fetch(record -> {
                    DataPointVO point = dataPointDao.mapRecord(record);
                    dataPointDao.loadRelationalData(point);
                    return new PointHistoryCount(point, record.get(count));
                });
    }

    private @Nullable TranslatableMessage getAnnotation(PointValueTime pointValue) {
        return pointValue instanceof IAnnotated ? ((IAnnotated) pointValue).getSourceMessage() : null;
    }

    private long savePointValueImpl(final DataPointVO vo, final PointValueTime pointValue, boolean async) {
        DataValue value = pointValue.getValue();
        final DataType dataType = value.getDataType();
        double dvalue = 0;
        String svalue = null;

        if (value.hasDoubleRepresentation())
            dvalue = value.getDoubleValue();
        else
            svalue = value.getStringValue();

        // Check if we need to create an annotation.
        long id;
        try {
            id = savePointValue(vo, dataType, dvalue, pointValue.getTime(), svalue, getAnnotation(pointValue), async);
        } catch (ConcurrencyFailureException e) {
            // Still failed to insert after all of the retries. Store the data
            unsavedPointValues.add(new UnsavedPointValue(vo, pointValue));
            return -1;
        }

        writeUnsavedPointValues();
        return id;
    }

    private void writeUnsavedPointValues() {
        UnsavedPointValue data;
        while ((data = unsavedPointValues.poll()) != null) {
            savePointValueImpl(data.vo, data.pointValue, false);
        }
    }

    private long savePointValue(DataPointVO vo, DataType dataType, double dvalue, long time, String svalue, TranslatableMessage sourceMessage, boolean async) {
        // Apply database specific bounds on double values.
        dvalue = databaseProxy.applyBounds(dvalue);

        if (async && svalue == null && sourceMessage == null) {
            addBatchWriteEntry(new BatchWriteEntry(vo.getSeriesId(), dataType, dvalue, time));
            return -1;
        }

        int retries = 5;
        while (true) {
            try {
                return savePointValueImpl(vo, dataType, dvalue, time, svalue, sourceMessage);
            } catch (ConcurrencyFailureException e) {
                if (retries <= 0)
                    throw e;
                retries--;
            } catch (RuntimeException e) {
                throw new RuntimeException("Error saving point value: dataType=" + dataType + ", dvalue=" + dvalue, e);
            }
        }
    }

    private long savePointValueImpl(DataPointVO vo, DataType dataType, double dvalue, long time, String svalue, TranslatableMessage sourceMessage) {
        long id = this.create.insertInto(pv)
                .set(pv.dataPointId, vo.getSeriesId())
                .set(pv.dataType, dataType.getId())
                .set(pv.pointValue, dvalue)
                .set(pv.ts, time)
                .returningResult(pv.id)
                .fetchOptional()
                .orElseThrow()
                .value1();

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

    /**
     * Holds point value data that could not be saved to the database due to concurrency errors.
     *
     * @author Matthew Lohbihler
     */
    private static class UnsavedPointValue {
        private final DataPointVO vo;
        private final PointValueTime pointValue;

        public UnsavedPointValue(DataPointVO vo, PointValueTime pointValue) {
            this.vo = vo;
            this.pointValue = pointValue;
        }
    }

    private static class BatchWriteEntry {
        private final int seriesId;
        private final DataType dataType;
        private final double dvalue;
        private final long time;

        public BatchWriteEntry(int seriesId, DataType dataType, double dvalue, long time) {
            this.seriesId = seriesId;
            this.dataType = dataType;
            this.dvalue = dvalue;
            this.time = time;
        }
    }

    private void addBatchWriteEntry(BatchWriteEntry e) {
        synchronized (entries) {
            entries.push(e);
            queueSize = entries.size();

            try {
                if (entries.size() > instances.size() * SPAWN_THRESHOLD) {
                    if (instances.size() < MAX_INSTANCES) {
                        BatchWriteTask bwb = new BatchWriteTask();
                        instances.add(bwb);
                        try {
                            Common.backgroundProcessing.addWorkItem(bwb);
                        } catch (RejectedExecutionException ree) {
                            instances.remove(bwb);
                            throw ree;
                        }
                    }
                }
            } finally {
                threadCount = instances.size();
            }
        }
    }

    private class BatchWriteTask implements WorkItem {

        @Override
        public void execute() {
            try {
                BatchWriteEntry[] inserts;
                while (true) {
                    synchronized (entries) {
                        if (entries.size() == 0)
                            break;

                        inserts = new BatchWriteEntry[Math.min(entries.size(), batchInsertSize)];
                        entries.pop(inserts);
                        queueSize = entries.size();
                    }
                    // Create the sql and parameters
                    int count = writeMultiple(Arrays.stream(inserts));
                    writeMeter.mark(count);
                }
            } finally {
                instances.remove(this);
                threadCount = instances.size();
            }
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_HIGH;
        }

        @Override
        public String getDescription() {
            return "Batch Writing from batch of size: " + entries.size();
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
            threadCount = instances.size();
        }
    }

    private int writeMultiple(Stream<BatchWriteEntry> entryStream) {
        PointValues pv = PointValues.POINT_VALUES;
        var insert = create.insertInto(pv)
                .columns(pv.dataPointId, pv.dataType, pv.pointValue, pv.ts);

        MutableInt count = new MutableInt();
        entryStream.forEach(entry -> {
            insert.values(entry.seriesId, entry.dataType.getId(), entry.dvalue, entry.time);
            count.incrementAndGet();
        });
        int written = 0;

        // Insert the data
        int retries = 10;
        while (true) {
            try {
                insert.execute();
                written = count.intValue();
                break;
            } catch (RuntimeException e) {
                if (RETRIED_EXCEPTIONS.contains(e.getClass())) {
                    if (retries <= 0) {
                        log.error("Concurrency failure saving {} point values after 10 tries. Data lost.", count.intValue());
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
                    log.error("Error saving {} point values. Data lost.", count.intValue(), e);
                    break;
                }
            }
        }
        return written;
    }
}
