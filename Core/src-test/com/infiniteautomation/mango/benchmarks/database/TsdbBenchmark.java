/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import java.io.IOException;
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

public class TsdbBenchmark extends BenchmarkRunner {

    @State(Scope.Benchmark)
    public static class InsertParams {
        @Param({"1000", "100000", "1000000"})
        public int count;

        public MockMango mockMango;

        @Setup
        public void setup(MockMango mockMango) {
            this.mockMango = mockMango;
        }
    }

    @Benchmark
    @Threads(Threads.MAX)
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 1, time = 10)
    @Measurement(iterations = 1, time = 10)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void insert(InsertParams insertParams, Blackhole blackhole) throws IOException {
        blackhole.consume(insertParams);
    }

}
