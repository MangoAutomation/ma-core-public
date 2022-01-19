/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.migration.progress;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.iterators.ChunkingSpliterator;
import com.infiniteautomation.mango.db.tables.TimeSeriesMigrationProgress;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.BaseDao;
import com.serotonin.m2m2.db.dao.migration.MigrationStatus;

@Repository
public class DefaultMigrationProgressDao extends BaseDao implements MigrationProgressDao {

    private final TimeSeriesMigrationProgress table = TimeSeriesMigrationProgress.TIME_SERIES_MIGRATION_PROGRESS;

    public DefaultMigrationProgressDao(DatabaseProxy databaseProxy) {
        super(databaseProxy);
    }

    @Override
    public void insert(MigrationProgress progress) {
        create.insertInto(table)
                .set(table.seriesId, progress.seriesId)
                .set(table.status, progress.status.name())
                .set(table.timestamp, progress.timestamp)
                .execute();
    }

    @Override
    public void bulkInsert(Stream<MigrationProgress> stream) {
        ChunkingSpliterator.chunkStream(stream, databaseProxy.batchSize()).forEach(chunk -> {
            // TODO keep prepared statement open, see https://stackoverflow.com/questions/62959401/how-to-clear-a-batch-in-jooq
            var insert = create.insertInto(table)
                    .columns(table.seriesId, table.status, table.timestamp)
                    .values((Integer) null, null, null);
            var batch = create.batch(insert);
            chunk.forEach(progress -> batch.bind(progress.seriesId, progress.status, progress.timestamp));
            batch.execute();
        });
    }

    @Override
    public void update(MigrationProgress progress) {
        create.update(table)
                .set(table.status, progress.status.name())
                .set(table.timestamp, progress.timestamp)
                .where(table.seriesId.eq(progress.seriesId))
                .execute();
    }

    @Override
    public void deleteAll() {
        create.deleteFrom(table).execute();
    }

    @Override
    public Optional<MigrationProgress> get(int seriesId) {
        return create.select(table.fields())
                .where(table.seriesId.eq(seriesId))
                .fetchOptional()
                .map(this::mapRecord);
    }

    @Override
    public int count() {
        var count = DSL.count();
        return Objects.requireNonNull(create.select(count)
                .from(table)
                .fetchSingle(count));
    }

    @Override
    public Stream<MigrationProgress> stream() {
        return create.select(table.fields())
                .from(table)
                .stream()
                .map(this::mapRecord);
    }

    private MigrationProgress mapRecord(Record r) {
        return new MigrationProgress(
                r.get(table.seriesId),
                MigrationStatus.valueOf(r.get(table.status)),
                r.get(table.timestamp));
    }

}
