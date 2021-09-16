/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.infiniteautomation.mango.benchmarks.BenchmarkRunner;
import com.infiniteautomation.mango.benchmarks.MockMango;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.DatabaseProxyFactory;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.DefaultDatabaseProxyFactory;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.serotonin.m2m2.db.NoSQLProxy;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class TsdbBenchmark extends BenchmarkRunner {

    public static class TsdbMockMango extends MockMango {
        @Param({"h2:memory", "h2"})
        String databaseType;

        @Param({"PointValueDaoSQL", "MangoNoSqlPointValueDao"})
        String pointValueDaoClass;

        PointValueDao pvDao;

        @Override
        public void setupTrial() throws Exception {
            DatabaseProxyFactory delegate = new DefaultDatabaseProxyFactory();
            lifecycle.setDatabaseProxyFactory((type) -> {
                // ignore type from properties

                DatabaseProxy dbProxy;
                if ("h2:memory".equals(databaseType)) {
                    dbProxy = new H2InMemoryDatabaseProxy();
                } else {
                    dbProxy = delegate.createDatabaseProxy(DatabaseType.valueOf(databaseType.toUpperCase()));
                }

                switch(pointValueDaoClass) {
                    case "MangoNoSqlPointValueDao": {
                        try {
                            Class<?> proxyClass = getClass().getClassLoader().loadClass("com.infiniteautomation.nosql.MangoNoSqlProxy");
                            Constructor<?> constructor = proxyClass.getConstructor();
                            NoSQLProxy tsdbProxy = (NoSQLProxy) constructor.newInstance();
                            dbProxy.setNoSQLProxy(tsdbProxy);
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    }
                    case "PointValueDaoSQL": {
                        // no-op
                        break;
                    }
                    default: throw new UnsupportedOperationException();
                }

                return dbProxy;
            });

            super.setupTrial();
        }

        @Setup
        public void setup() {
            this.pvDao = Common.databaseProxy.newPointValueDao();
        }
    }

    @State(Scope.Thread)
    public static class PerThreadState {

        final Random random = new Random();
        DataPointVO point;
        long timestamp = 0;

        @Setup
        public void setup(TsdbMockMango mango) throws ExecutionException, InterruptedException {
            point = mango.createDataPoints(1, Collections.emptyMap()).get(0);
        }

        public PointValueTime newValue() {
            return new PointValueTime(random.nextDouble(), timestamp++);
        }
    }

    @Benchmark
    @Threads(1)
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1, time = 10)
    @Measurement(iterations = 1, time = 10)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void singleThreadInsert(TsdbMockMango mango, PerThreadState perThreadState, Blackhole blackhole) {
        PointValueTime result = mango.pvDao.savePointValueSync(perThreadState.point, perThreadState.newValue(), null);
        blackhole.consume(result);
    }

    @Benchmark
    @Threads(Threads.MAX)
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1, time = 10)
    @Measurement(iterations = 1, time = 10)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void multiThreadInsert(TsdbMockMango mango, PerThreadState perThreadState, Blackhole blackhole) {
        PointValueTime result = mango.pvDao.savePointValueSync(perThreadState.point, perThreadState.newValue(), null);
        blackhole.consume(result);
    }
}
