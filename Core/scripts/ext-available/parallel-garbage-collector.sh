#!/bin/bash
case "$1" in
    init)
        # Parallel Garbage Collection aka Throughput collector
        # performs minor collections in parallel.  Medium to large size
        # data sets and multi cpu systems.
        JAVAOPTS=$JAVAOPTS' -XX:+UseParallelGC'
        #Use parallel compaction, perform major collections in parallel
        JAVAOPTS=$JAVAOPTS' -XX:+UseParallelOldGC'
        #Optionally tune the number of threads used
        #JAVAOPTS=$JAVAOPTS' XX:ParallelGCThreads=4'
        # Print the details from the collector's runs
        #Comment this line if the output is not desired
        JAVAOPTS=$JAVAOPTS' -verbose:gc -XX:+PrintGCDetails'
        ;;
esac