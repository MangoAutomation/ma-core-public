#!/bin/bash
case "$1" in
    init)
    	# Enable garbageless configuration for high performance logging 
        JAVAOPTS="$JAVAOPTS --add-modules java.xml.bind"
        ;;
esac
