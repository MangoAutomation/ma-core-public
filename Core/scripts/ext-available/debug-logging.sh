#!/bin/bash
case "$1" in
    init)
		# Use DEBUG level configuration file 
		JAVAOPTS="$JAVAOPTS -Dlog4j.configurationFile=file:$MA_HOME/classes/debug-log4j2.xml"
        ;;
esac
