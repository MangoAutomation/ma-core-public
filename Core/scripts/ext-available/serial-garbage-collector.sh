#!/bin/bash
case "$1" in
    init)
        # Specifically use the Serial Garbage Collector (Default)
        # Designed for use with small data sets up to 100MB and single processor machines
        # Also print the details from the collector's runs
        JAVAOPTS=$JAVAOPTS' -XX:+UseSerialGC'
        #Comment this line if the output is not desired
        JAVAOPTS=$JAVAOPTS' -verbose:gc -XX:+PrintGCDetails'
        ;;
esac