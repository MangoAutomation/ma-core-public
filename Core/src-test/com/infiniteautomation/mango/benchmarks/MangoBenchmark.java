/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.benchmarks;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.serotonin.m2m2.MangoTestBase;

/**
 * Base helper class for Mango Benchmarks using JMH.
 *
 * The mock Mango lifecycle must be managed by these annotations
 *  in the subclass:
 *
 *  @Setup(Level.Trial) - Initialize Mango using the setupTrial method, customize as required.
 *  @Setup(Level.Iteration) - Noop for now
 *  @TearDown(Level.Iteration) - Clean database between trials
 *  @TearDown(Level.Trial) - Terminate lifecycle and shutdown thread pools etc.
 *
 *  NOTE: There is a known bug when running with Fork = 0 that only the first trial will succeed.
 *    The remaining will fail due to the Global classes that exist in Mango
 */
public abstract class MangoBenchmark {

    @Test
    public void runBenchmark() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(this.getClass().getName())
                .build();
        new Runner(opt).run();
    }

    /**
     * Common logic to initialize Mango before the trial.  Note this
     * will only work with Fork > 0
     * @param params
     */
    public void setupTrial(MangoBenchmarkParameters params) {
        try {
            MangoTestBase.staticSetup();
        } catch (IOException e) {
            fail(e.getMessage());
        }
        params.mango.before();
    }

    /**
     * No-op for now
     * @param params
     */
    public void setupIteration(MangoBenchmarkParameters params) {

    }

    /**
     * Reset the database after every iteration
     * @param params
     */
    public void tearDownIteration(MangoBenchmarkParameters params) {
        params.mango.resetDatabase();
    }

    /**
     * Common logic to shutdown Mango after a trial. Note this
     * will only work with Fork > 0
     *
     * @param params
     */
    public void tearDownTrial(MangoBenchmarkParameters params) {
        params.mango.after();
        try {
            MangoTestBase.staticTearDown();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
