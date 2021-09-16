/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import java.io.IOException;
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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.infiniteautomation.mango.benchmarks.BenchmarkRunner;
import com.infiniteautomation.mango.benchmarks.MockMango;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public class TsdbBenchmark extends BenchmarkRunner {

    @State(Scope.Benchmark)
    public static class TsdbBenchmarkState {

        MockMango mockMango;
        PointValueDao pvDao;

        @Setup
        public void setup(MockMango mockMango) {
            this.mockMango = mockMango;
            this.pvDao = Common.databaseProxy.newPointValueDao();
        }
    }

    @State(Scope.Thread)
    public static class PerThreadState {

        final Random random = new Random();
        DataPointVO point;
        long timestamp = 0;

        @Setup
        public void setup(MockMango mockMango) throws ExecutionException, InterruptedException {
            point = mockMango.createDataPoints(1, Collections.emptyMap()).get(0);
        }

        public PointValueTime newValue() {
            return new PointValueTime(random.nextDouble(), timestamp++);
        }
    }

    @Benchmark
    @Threads(1)
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 1, time = 10)
    @Measurement(iterations = 1, time = 60)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void singleThreadInsert(TsdbBenchmarkState state, PerThreadState perThreadState, Blackhole blackhole) throws IOException {
        PointValueTime result = state.pvDao.savePointValueSync(perThreadState.point, perThreadState.newValue(), null);
        blackhole.consume(result);
    }

    @Benchmark
    @Threads(Threads.MAX)
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 1, time = 10)
    @Measurement(iterations = 1, time = 60)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void multiThreadInsert(TsdbBenchmarkState state, PerThreadState perThreadState, Blackhole blackhole) throws IOException {
        PointValueTime result = state.pvDao.savePointValueSync(perThreadState.point, perThreadState.newValue(), null);
        blackhole.consume(result);
    }
}
