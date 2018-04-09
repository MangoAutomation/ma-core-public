#!/bin/bash
case "$1" in
    init)
    	# Enable garbageless configuration for high performance logging 
        JAVAOPTS="$JAVAOPTS -Dlog4j2.enableThreadlocals=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
		JAVAOPTS="$JAVAOPTS -Dlog4j.configurationFile=file:$MA_HOME/classes/high-performance-log4j2.xml"
        ;;
esac
