#!/bin/bash
case "$1" in
    init)
    	# Add required java modules not inherently loaded in Java 9, comma separated
        JAVAOPTS="$JAVAOPTS --add-modules java.xml.bind"
        ;;
esac
