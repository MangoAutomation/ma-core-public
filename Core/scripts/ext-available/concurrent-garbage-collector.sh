#!/bin/bash
case "$1" in
    init)
        # Concurrent Garbage Collection
        # Medium to large size data sets and multi cpu systems where response time is more important
        # than overall throughput.  The techniques used to imimize pauses can reduce application
        # performance.
        JAVAOPTS=$JAVAOPTS' -XX:+UseConcMarkSweepGC'
        # Print the details from the collector's runs
        #Comment this line if the output is not desired
        JAVAOPTS=$JAVAOPTS' -verbose:gc -XX:+PrintGCDetails'
        ;;
esac