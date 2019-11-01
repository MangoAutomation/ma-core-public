#!/bin/bash
# 
# Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
# @author Jared Wiltshire
# @author Matthew Lohbihler
#

set -e
[ -x "$(command -v greadlink)" ] && alias readlink='greadlink'
script_dir=$(dirname $(readlink -f "$0"))

# Only set MA_HOME if not already set
[ -z "$MA_HOME" ] && MA_HOME=$(dirname "$script_dir")

if [ ! -d "$MA_HOME" ]; then
    echo 'Error: MA_HOME is not set or is not a directory'
    exit 1
fi

echo MA_HOME is "$MA_HOME"
rm -f "$MA_HOME"/bin/ma.pid

# This will ensure that the logs are written to the correct directories.
cd "$MA_HOME"

#Create a logs directory if it doesn't exist
if [ ! -d "$MA_HOME"/logs ]; then
	mkdir "$MA_HOME"/logs
fi

# Determine the Java home
if [ -d "$JAVA_HOME" ]; then
    EXECJAVA=java
else
    EXECJAVA="$JAVA_HOME"/bin/java
fi

export MA_HOME JPDA EXECJAVA JAVAOPTS SYSOUT SYSERR

# Run enabled init extensions.
if [ "$(ls -A "$MA_HOME"/bin/ext-enabled)" ]; then
    echo 'ma-start: running init extensions...'
    for f in "$MA_HOME"/bin/ext-enabled/*.sh
    do
        source "$f" init
    done
fi

# Check for core upgrade
if [ -r "$MA_HOME"/m2m2-core-*.zip ]; then
	echo 'ma-start: upgrading core...'
	
	# Delete jars and work dir
	rm -f "$MA_HOME"/lib/*.jar
	rm -rf "$MA_HOME"/work
	
	# Delete the release properties files
	if [ ! -z "$MA_HOME"/release.properties ]; then
		rm -f "$MA_HOME"/release.properties
	fi
	if [ ! -z "$MA_HOME"/release.signed ]; then
		rm -f "$MA_HOME"/release.signed
	fi
	
	# Unzip core. The exact name is unknown, but there should only be one, so iterate
	for f in "$MA_HOME"/m2m2-core-*.zip
	do
	    unzip -o "$f"
	    rm "$f"
	done
	
	chmod +x "$MA_HOME"/bin/*.sh
	chmod +x "$MA_HOME"/bin/ext-available/*.sh
fi

# Construct the Java classpath
MA_CP="$MA_HOME"/overrides/classes
MA_CP=$MA_CP:"$MA_HOME"/classes
MA_CP=$MA_CP:"$MA_HOME"/overrides/properties
MA_CP=$MA_CP:"$MA_HOME"/overrides/lib/*
MA_CP=$MA_CP:"$MA_HOME"/lib/*

# Run enabled start extensions
if [ "$(ls -A "$MA_HOME"/bin/ext-enabled)" ]; then
    echo 'ma-start: running start extensions...'
    for f in "$MA_HOME"/bin/ext-enabled/*.sh
    do
        source "$f" start
    done
fi

# Check for output redirection
if [ ! -z "$SYSOUT" ] && [ ! -z "$SYSERR" ]; then
    # Both output redirects are set
    exec >"$SYSOUT" 2>"$SYSERR"
elif [ ! -z "$SYSOUT" ]; then
    # Just sysout is set
    exec >"$SYSOUT"
elif [ ! -z "$SYSERR" ]; then
    # Just syserr is set
    exec >"$SYSERR"
fi

#Delete Range.class if it exists
if [ -r "$MA_HOME"/classes/org/jfree/data/Range.class ]; then
	rm -f "$MA_HOME"/classes/org/jfree/data/Range.class
fi

echo 'ma-start: starting MA'
$EXECJAVA $JPDA $JAVAOPTS -server -cp "$MA_CP" \
    "-Dma.home=$MA_HOME" \
    "-Djava.library.path=$MA_HOME/overrides/lib:$MA_HOME/lib:/usr/lib/jni/:$PATH" \
    com.serotonin.m2m2.Main &

PID=$!
echo $PID > "$MA_HOME"/bin/ma.pid
echo "ma-start: MA started with Process ID: " $PID

exit 0
