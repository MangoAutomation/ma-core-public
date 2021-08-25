/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.infiniteautomation.mango.spring.components.RunAs;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;

@State(Scope.Benchmark)
public class DataPointBenchmarks {

    private MangoBenchmark base;

    public DataPointBenchmarks() {
        this.base = new MangoBenchmark();
    }

    @Test
    public void runBenchmark() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(this.getClass().getName())
                .build();
        new Runner(opt).run();
    }

    @State(Scope.Benchmark)
    public static class BenchmarkParams {
        @Param({ "1000", "100000", "1000000" })
        public int dataPointCount;

    }

    @Benchmark
    @Threads(1)
    @Fork(value = 0, warmups = 0)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void insertSpeed(BenchmarkParams params) {
        RunAs runAs = Common.getBean(RunAs.class);
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            base.getMango().createMockDataPoints(params.dataPointCount);
        });
    }

    @Before
    public void before() {
        System.out.println("Before");
        base.before();
    }

    @BeforeClass
    public static void staticSetup() throws IOException {
        System.out.println("Before class");
        MangoTestBase.staticSetup();
    }

    @After
    public void after() {
        System.out.println("After");
        base.after();
    }

    @AfterClass
    public static void staticTearDown() throws IOException {
        System.out.println("After class");
        MangoTestBase.staticTearDown();
    }
}
