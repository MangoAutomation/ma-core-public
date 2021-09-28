/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.db.tables.PointValues;
import com.infiniteautomation.mango.db.tables.records.PointValuesRecord;
import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.ImageSaveException;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.metrics.EventHistogram;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.util.queue.ObjectQueue;

public class PointValueDaoSQL extends BasicSQLPointValueDao {

    private final ConcurrentLinkedQueue<UnsavedPointValue> unsavedPointValues = new ConcurrentLinkedQueue<>();

    public static final String SYNC_INSERTS_SPEED_COUNTER_ID = "com.serotonin.m2m2.db.dao.PointValueDaoSQL.SYNC_INSERTS_SPEED_COUNTER";
    private final ValueMonitor<Integer> syncInsertsSpeedCounter = Common.MONITORED_VALUES.<Integer>create(SYNC_INSERTS_SPEED_COUNTER_ID)
                    .name(new TranslatableMessage("internal.monitor.SYNC_INSERTS_SPEED_COUNTER_ID"))
                    .value(0)
                    .build();

    public static final String ASYNC_INSERTS_SPEED_COUNTER_ID = "com.serotonin.m2m2.db.dao.PointValueDaoSQL.ASYNC_INSERTS_SPEED_COUNTER";
    private final ValueMonitor<Integer> asyncInsertsSpeedCounter = Common.MONITORED_VALUES.<Integer>create(ASYNC_INSERTS_SPEED_COUNTER_ID)
            .name(new TranslatableMessage("internal.monitor.ASYNC_INSERTS_SPEED_COUNTER_ID"))
            .value(0)
            .build();

    private final EventHistogram syncCallsCounter = new EventHistogram(5000, 2);
    private final EventHistogram asyncCallsCounter = new EventHistogram(5000, 2);

    public PointValueDaoSQL(DatabaseProxy databaseProxy) {
        super(databaseProxy);
    }

    public PointValueDaoSQL(DataSource dataSource, PlatformTransactionManager transactionManager, DatabaseType databaseType, DSLContext context) {
        super(dataSource, transactionManager, databaseType, context);
    }

    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    @Override
    public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue, SetPointSource source) {
        syncCallsCounter.hit();
        syncInsertsSpeedCounter.setValue(syncCallsCounter.getEventCounts()[0] / 5);
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
        asyncCallsCounter.hit();
        asyncInsertsSpeedCounter.setValue(asyncCallsCounter.getEventCounts()[0] / 5);

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
        dvalue = databaseProxy.applyBounds(dvalue);

        if (async) {
            BatchWriteBehind.add(new BatchWriteBehindEntry(vo, dataType, dvalue, time), create, databaseType);
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
        private static final Logger LOG = LoggerFactory.getLogger(BatchWriteBehind.class);
        private static final int SPAWN_THRESHOLD = 10000;
        private static final int MAX_INSTANCES = 5;
        private final int maxRows;
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
            retriedExceptions.add(RecoverableDataAccessException.class);
            retriedExceptions.add(TransientDataAccessException.class);
            retriedExceptions.add(TransientDataAccessResourceException.class);
            retriedExceptions.add(CannotGetJdbcConnectionException.class);
        }

        static void add(BatchWriteBehindEntry e, DSLContext create, DatabaseType databaseType) {
            synchronized (ENTRIES) {
                ENTRIES.push(e);
                ENTRIES_MONITOR.setValue(ENTRIES.size());
                if (ENTRIES.size() > instances.size() * SPAWN_THRESHOLD) {
                    if (instances.size() < MAX_INSTANCES) {
                        BatchWriteBehind bwb = new BatchWriteBehind(create, databaseType);
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

        public BatchWriteBehind(DSLContext create, DatabaseType databaseType) {
            this.create = create;

            if (databaseType == DatabaseType.DERBY)
                // This has not been tested to be optimal
                maxRows = 1000;
            else if (databaseType == DatabaseType.H2)
                // This has not been tested to be optimal
                maxRows = 1000;
            else if (databaseType == DatabaseType.MSSQL)
                // MSSQL has max rows of 1000, and max parameters of 2100. In this case that works out to...
                maxRows = 524;
            else if (databaseType == DatabaseType.MYSQL)
                // This appears to be an optimal value
                maxRows = 2000;
            else if (databaseType == DatabaseType.POSTGRES)
                // This appears to be an optimal value
                maxRows = 2000;
            else
                throw new ShouldNeverHappenException("Unknown database type: " + databaseType);
        }

        @Override
        public void execute() {
            try {
                BatchWriteBehindEntry[] inserts;
                while (true) {
                    synchronized (ENTRIES) {
                        if (ENTRIES.size() == 0)
                            break;

                        inserts = new BatchWriteBehindEntry[Math.min(ENTRIES.size(), maxRows)];
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
