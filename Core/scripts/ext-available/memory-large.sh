#!/bin/bash
case "$1" in
    init)
        # Startup with Java Memory setup for Large installation
        JAVAOPTS="$JAVAOPTS -Xms10g -Xmx10g"
        ;;
esac