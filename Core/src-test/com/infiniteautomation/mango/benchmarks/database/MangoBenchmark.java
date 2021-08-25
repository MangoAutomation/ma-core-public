/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks.database;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class MangoBenchmark {
    private MockMango mango;

    public MangoBenchmark() {
        this.mango = new MockMango();
    }

    public MockMango getMango() {
        return mango;
    }

    public void before() {
        mango.before();
    }

    public void after() {
        mango.after();
    }
}
