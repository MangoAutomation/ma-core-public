/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks;

import org.junit.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {
    @Test
    public void runBenchmark() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(getClass().getName())
                .build();
        new Runner(opts).run();
    }
}
