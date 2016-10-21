#!/bin/bash
case "$1" in
    init)
        # Startup with Java Memory setup for Medium size installation
        # The heap is set to non-expanding for increased performance.  If memory use is a factor
        # set the Minimum heap size to a lower number and let the JVM adjust when necessary.
        JAVAOPTS="$JAVAOPTS -Xms5g -Xmx5g"
        ;;
esac