/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.tsdb;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import com.infiniteautomation.mango.benchmarks.MockMango;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.DatabaseProxyFactory;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.DefaultDatabaseProxyFactory;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.serotonin.m2m2.db.NoSQLProxy;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class Insert {
    public static final String THREADS_PARAM = "threads";
    public static final String POINTS_PARAM = "points";
    public static final Collection<String> DEFAULT_THREADS = List.of("1", "1C");
    public static final Collection<String> DEFAULT_POINTS = List.of("100", "1000");

    public static void main(String[] args) throws RunnerException, CommandLineOptionException {
        CommandLineOptions cmdOptions = new CommandLineOptions(args);
        new Insert().runBenchmark(cmdOptions);
    }

    @Test
    public void runBenchmark() throws RunnerException {
        runBenchmark(new OptionsBuilder().build());
    }

    /**
     * We cannot parameterize the Threads and OperationsPerInvocation annotations, so we must do it ourselves
     * programmatically, combine the results, then output them to stdout
     */
    private void runBenchmark(Options options) throws RunnerException {
        int processors = Runtime.getRuntime().availableProcessors();
        List<RunResult> results = new ArrayList<>();

        int[] threadsParams = options.getParameter(THREADS_PARAM)
                .orElse(DEFAULT_THREADS)
                .stream()
                .mapToInt(s -> {
                    if (s.endsWith("C")) {
                        float multiplier = Float.parseFloat(s.substring(0, s.length() - 1));
                        return (int) multiplier * processors;
                    }
                    return Integer.parseInt(s);
                }).toArray();

        int[] pointsParams = options.getParameter(POINTS_PARAM)
                .orElse(DEFAULT_POINTS)
                .stream()
                .mapToInt(Integer::parseInt).toArray();

        for (int threads : threadsParams) {
            for (int points : pointsParams) {
                Options opts = new OptionsBuilder()
                        .parent(options)
                        .include(getClass().getName())
                        .threads(threads)
                        .operationsPerInvocation(points / threads)
                        .param(THREADS_PARAM, Integer.toString(threads))
                        .param(POINTS_PARAM, Integer.toString(points))
                        //.verbosity(VerboseMode.SILENT)
                        .build();

                results.addAll(new Runner(opts).run());
            }
        }

        // sort the results for more legible output
        results.sort(RunResult.DEFAULT_SORT_COMPARATOR);

        OutputFormat outputFormat = OutputFormatFactory.createFormatInstance(System.out, VerboseMode.NORMAL);
        outputFormat.endRun(results);
    }

    public static class TsdbMockMango extends MockMango {
        //@Param({"h2:memory", "h2"})
        @Param({"h2"})
        String databaseType;

        @Param({"PointValueDaoSQL", "MangoNoSqlPointValueDao"})
        String pointValueDaoClass;

        @Param({"1"})
        int threads;

        @Param({"1"})
        int points;

        PointValueDao pvDao;

        @Override
        protected void preInitialize() {
            MockMangoProperties props = (MockMangoProperties) Common.envProps;
            props.setProperty("db.nosql.maxOpenFiles", Integer.toString(points * 2));

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
                        // no-op, this is the default PointValueDao
                        break;
                    }
                    default: throw new UnsupportedOperationException();
                }

                return dbProxy;
            });
        }

        @Setup
        public void setup() {
            this.pvDao = Common.databaseProxy.newPointValueDao();
        }
    }

    @State(Scope.Thread)
    public static class PerThreadState {

        final Random random = new Random();
        List<DataPointVO> points;
        long timestamp = 0;

        @Setup
        public void setup(TsdbMockMango mango) throws ExecutionException, InterruptedException {
            this.points = mango.createDataPoints(mango.points / mango.threads, Collections.emptyMap());
        }

        public PointValueTime newValue() {
            return new PointValueTime(random.nextDouble(), timestamp);
        }
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 2, time = 60)
    @Measurement(iterations = 5, time = 60)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void insert(TsdbMockMango mango, PerThreadState perThreadState, Blackhole blackhole) {
        for (DataPointVO point : perThreadState.points) {
            PointValueTime v = mango.pvDao.savePointValueSync(point, perThreadState.newValue(), null);
            blackhole.consume(v);
        }
        perThreadState.timestamp += 1000;
    }

}
