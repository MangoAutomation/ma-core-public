#!/bin/bash
#
#    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# -----------------------------------------------------------------------------
# The start script for Mango Automation. This contains a loop for the purpose of 
# restarting MA as required. 

if [ -z "$MA_HOME" ]; then
    echo Do not execute this file directly. Run \'ma.sh start\' instead.
    exit 1
fi

# This will ensure that the logs are written to the correct directories.
cd "$MA_HOME"

LOOP_EXIT=false
while [ $LOOP_EXIT = false ]; do
    # Check for core upgrade
    if [ -r "$MA_HOME"/m2m2-core-*.zip ]; then
        "$MA_HOME"/bin/upgrade.sh
        exit 0
    fi
    
    # Construct the Java classpath
    MA_CP="$MA_HOME"/overrides/classes
    MA_CP=$MA_CP:"$MA_HOME"/classes
    MA_CP=$MA_CP:"$MA_HOME"/overrides/properties
    
    # Add all of the jar files in the overrides/lib dir to the classpath
    for f in "$MA_HOME"/overrides/lib/*.jar
    do
      MA_CP=$MA_CP:$f
    done
    
    # Add all of the jar files in the lib dir to the classpath
    for f in "$MA_HOME"/lib/*.jar
    do
      MA_CP=$MA_CP:$f
    done
    
    # Run enabled start extensions
    for f in "$MA_HOME"/bin/ext-enabled/*.sh
    do
        source $f start
    done
    
    # Check for output redirection
    if [ ! -z $SYSOUT ] && [ ! -z $SYSERR ]; then
        # Both output redirects are set
        exec >$SYSOUT 2>$SYSERR
    elif [ ! -z $SYSOUT ]; then
        # Just sysout is set
        exec >$SYSOUT
    elif [ ! -z $SYSERR ]; then
        # Just syserr is set
        exec >$SYSERR
    fi
    
    $EXECJAVA $JPDA $JAVAOPTS -server -cp "$MA_CP" \
        "-Dma.home=$MA_HOME" \
        "-Djava.library.path=$MA_HOME/overrides/lib:$MA_HOME/lib:/usr/lib/jni/:$PATH" \
        com.serotonin.m2m2.Main &
    
    PID=$!
    echo "Started Mango with ProcessID: " $PID
    echo $PID > "$MA_HOME"/bin/ma.pid
    until !(ps $PID > /dev/null) do
        sleep 10
    done
    rm "$MA_HOME"/bin/ma.pid
    
    if [ ! -r "$MA_HOME"/RESTART ]; then
        LOOP_EXIT=true
    fi
    
    # Run enabled restart extensions
    if [ -d "$MA_HOME"/bin/ext-enabled ]; then
    for f in "$MA_HOME"/bin/ext-enabled/*.sh
    do
        source $f restart
    done
    fi
    
    # Check if MA was explicitly stopped by the stop script.
    if [ -r "$MA_HOME"/STOP ]; then
        rm "$MA_HOME"/STOP
        LOOP_EXIT=true
    fi
done
