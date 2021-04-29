/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.benchmarks;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Results
 * <pre>
 * Benchmark                                          (implementation)  (mapSize)  Mode  Cnt      Score      Error  Units
 * MapIteration.iterationSpeed                       java.util.HashMap       1000  avgt    5      6.251 ±    0.632  us/op
 * MapIteration.iterationSpeed                       java.util.HashMap     100000  avgt    5   1665.126 ±  350.889  us/op
 * MapIteration.iterationSpeed                       java.util.HashMap    1000000  avgt    5  24671.571 ± 5248.340  us/op
 * MapIteration.iterationSpeed                 java.util.LinkedHashMap       1000  avgt    5      5.326 ±    0.210  us/op
 * MapIteration.iterationSpeed                 java.util.LinkedHashMap     100000  avgt    5    931.804 ±  847.821  us/op
 * MapIteration.iterationSpeed                 java.util.LinkedHashMap    1000000  avgt    5  10746.102 ± 4410.881  us/op
 * MapIteration.iterationSpeed  java.util.concurrent.ConcurrentHashMap       1000  avgt    5     10.428 ±    0.488  us/op
 * MapIteration.iterationSpeed  java.util.concurrent.ConcurrentHashMap     100000  avgt    5   1407.599 ±   36.135  us/op
 * MapIteration.iterationSpeed  java.util.concurrent.ConcurrentHashMap    1000000  avgt    5  20386.238 ± 1669.141  us/op
 * </pre>
 */
public class MapIteration {

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    @State(Scope.Benchmark)
    public static class MapIterationParams {

        @Param({ "1000", "100000", "1000000" })
        public int mapSize;

        @Param({ "java.util.HashMap", "java.util.LinkedHashMap", "java.util.concurrent.ConcurrentHashMap" })
        String implementation;

        public Map<Integer, Object> map;

        @Setup
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void setup() throws Exception {
            Class<? extends Map> implementationClass = Class.forName(implementation).asSubclass(Map.class);
            Constructor<? extends Map> constructor = implementationClass.getConstructor(int.class);
            map = constructor.newInstance(mapSize);

            for (int i = 0; i < mapSize; i++) {
                map.put(i, new Object());
            }
        }
    }

    @Benchmark
    @Threads(8)
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void iterationSpeed(Blackhole blackhole, MapIterationParams params) {
        for (Object obj : params.map.values()) {
            blackhole.consume(obj);
        }
    }

}
