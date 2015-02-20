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
    	echo `date` 'ma-start: upgrading core...' >> "$MA_HOME"/logs/ma-script.log
        "$MA_HOME"/bin/upgrade.sh
        exit 0
    fi
    
    # Construct the Java classpath
    MA_CP="$MA_HOME"/overrides/classes
    MA_CP=$MA_CP:"$MA_HOME"/classes
    MA_CP=$MA_CP:"$MA_HOME"/overrides/properties
	MA_CP="$MA_CP:$MA_HOME/overrides/lib/*"
    MA_CP="$MA_CP:$MA_HOME/lib/*"

	# Commented out because Mango dynamically builds the library path of module libs during startup
    #for f in `find "$MA_HOME"/web/modules -name '*.jar' -type f`
    #do
    #    dname=`dirname $f`
    #    if [[ $dname == */lib ]]; then
    #        MA_CP=$MA_CP:$f
    #    fi
    #done

    # Run enabled start extensions
    if [ "$(ls -A $MA_HOME/bin/ext-enabled)" ]; then
        echo `date` 'ma-start: running start extensions...' >> "$MA_HOME"/logs/ma-script.log
        for f in "$MA_HOME/bin/ext-enabled/*.sh"
        do
            source $f start
        done
    fi
    
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
    
    # Make sure there are no explicit stop or termination flag files
    rm -f "$MA_HOME"/STOP
    rm -f "$MA_HOME"/TERMINATED
    
    echo `date` 'ma-start: starting MA' >> $MA_HOME/logs/ma-script.log
    $EXECJAVA $JPDA $JAVAOPTS -server -cp "$MA_CP" \
        "-Dma.home=$MA_HOME" \
        "-Djava.library.path=$MA_HOME/overrides/lib:$MA_HOME/lib:/usr/lib/jni/:$PATH" \
        com.serotonin.m2m2.Main &
    
    PID=$!
    echo `date` ma-start: MA started with PID $PID >> "$MA_HOME"/logs/ma-script.log
    echo "Started Mango with ProcessID: " $PID
    echo $PID > "$MA_HOME"/bin/ma.pid
	until !(ps $PID > /dev/null) do
        # Check for a termination flag file. If found, kill the process.
        if [ -r "$MA_HOME"/TERMINATED ]; then
            sleep 1
            kill -9 $PID
        else
            sleep 3
        fi
    done
    rm "$MA_HOME"/bin/ma.pid
    
    # Commented this section out because MA should always restart unless explicitly stopped.
    #if [ ! -r "$MA_HOME"/RESTART ]; then
    #    echo `date` 'ma-start: no restart flag found. Exiting restart loop' >> "$MA_HOME"/logs/ma-script.log
    #    LOOP_EXIT=true
    #fi
    
    # Run enabled restart extensions
    if [ "$(ls -A $MA_HOME/bin/ext-enabled)" ]; then
    	echo `date` 'ma-start: running restart extentions' >> "$MA_HOME"/logs/ma-script.log
    	for f in "$MA_HOME"/bin/ext-enabled/*.sh
    	do
        	source $f restart
    	done
    fi
    
    # Check if MA was explicitly stopped by the stop script.
    if [ -r "$MA_HOME"/STOP ]; then
    	echo `date` 'ma-start: MA explicitly stopped. Exiting restart loop' >> "$MA_HOME"/logs/ma-script.log
        rm "$MA_HOME"/STOP
        LOOP_EXIT=true
    fi
done

echo `date` ma-start: done >> "$MA_HOME"/logs/ma-script.log