/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Helper class with Fork(0) to test lifecycle and base classes.
 */
@State(Scope.Benchmark)
public class DebugFrameworkBenchmark extends MangoBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkParams extends MangoBenchmarkParameters {
        @Param({"1"})
        public int iterationValues;
    }

    @Setup(Level.Trial)
    public void setupTrial(BenchmarkParams params) {
        super.setupTrial(params);
    }

    @Setup(Level.Iteration)
    public void setupIteration(BenchmarkParams params) {

    }

    @TearDown(Level.Iteration)
    public void tearDownIteration(BenchmarkParams params) {
        super.tearDownIteration(params);
    }

    @TearDown(Level.Trial)
    public void tearDownTrial(BenchmarkParams params) {
        super.tearDownTrial(params);
    }

    @Benchmark
    @Threads(1)
    @Fork(0)
    @BenchmarkMode({Mode.SampleTime})
    @Measurement(iterations = 1, batchSize = 5)
    @Warmup(iterations = 0)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void debug() {
        System.out.println("Running benchmark");
    }
}
