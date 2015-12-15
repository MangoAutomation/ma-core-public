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
	MA_CP=$MA_CP:"$MA_HOME"/overrides/lib/*
    MA_CP=$MA_CP:"$MA_HOME"/lib/*

	# Commented out because Mango dynamically builds the library path of module libs during startup
    #for f in `find "$MA_HOME"/web/modules -name '*.jar' -type f`
    #do
    #    dname=`dirname $f`
    #    if [[ $dname == */lib ]]; then
    #        MA_CP=$MA_CP:$f
    #    fi
    #done

    # Run enabled start extensions
    if [ "$(ls -A "$MA_HOME"/bin/ext-enabled)" ]; then
        echo `date` 'ma-start: running start extensions...' >> "$MA_HOME"/logs/ma-script.log
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
    
    # Make sure there are no explicit stop or termination flag files
    rm -f "$MA_HOME"/STOP
    rm -f "$MA_HOME"/TERMINATED

	#Check to see if we have a pid and are running on it...
    if [ -r "$MA_HOME"/bin/ma.pid ]; then
        PID=$(cat "$MA_HOME"/bin/ma.pid)      
        if ps -p $PID > /dev/null
         then
         	# final check to see that main is running on this pid
            if  [ $(pgrep -f com.serotonin.m2m2.Main) == "$PID" ]
             then
		        echo `date` ma-start: MA already running with Process ID: $PID >> "$MA_HOME"/logs/ma-script.log
		        echo "MA already running with Process ID: " $PID
		        break;
             else
		        echo `date` ma-start: MA removing pid file for dead pid: $PID >> "$MA_HOME"/logs/ma-script.log
		        echo "MA removing dead pid file for PID: " $PID
	            rm -f "$MA_HOME"/bin/ma.pid
	        fi
         else
         	echo `date` ma-start: MA removing pid file for dead pid: $PID >> "$MA_HOME"/logs/ma-script.log
	        echo "MA removing dead pid file for PID: " $PID
            rm -f "$MA_HOME"/bin/ma.pid
        fi
    fi

    echo `date` 'ma-start: starting MA' >> "$MA_HOME"/logs/ma-script.log
    $EXECJAVA $JPDA $JAVAOPTS -server -cp "$MA_CP" \
        "-Dma.home=$MA_HOME" \
        "-Djava.library.path=$MA_HOME/overrides/lib:$MA_HOME/lib:/usr/lib/jni/:$PATH" \
        com.serotonin.m2m2.Main &
    
    PID=$!
    echo `date` ma-start: MA started with Process ID $PID >> "$MA_HOME"/logs/ma-script.log
    echo "ma-start: MA started with Process ID: " $PID
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

    # Check if MA was explicitly stopped by the stop script.
    if [ -r "$MA_HOME"/STOP ]; then
    	echo ma-start: MA explicitly stopped.
    	echo `date` 'ma-start: MA explicitly stopped. Exiting restart loop' >> "$MA_HOME"/logs/ma-script.log
        rm "$MA_HOME"/STOP
        LOOP_EXIT=true
        break;
    fi
    
    #Should we restart?
    if [ ! -r "$MA_HOME"/RESTART ]; then
        echo ma-start: no restart flag found, not restarting MA
        echo `date` 'ma-start: no restart flag found, not restarting MA' >> "$MA_HOME"/logs/ma-script.log
        LOOP_EXIT=true
        break;
    fi
    
    # Run enabled restart extensions
    if [ -r "$MA_HOME"/RESTART ]; then
        if [ "$(ls -A "$MA_HOME"/bin/ext-enabled)" ]; then
            echo `date` 'ma-start: running restart extentions' >> "$MA_HOME"/logs/ma-script.log
            for f in "$MA_HOME"/bin/ext-enabled/*.sh
            do
                source "$f" restart
            done
        fi
    fi

done
echo ma-start: MA done
echo `date` ma-start: MA done >> "$MA_HOME"/logs/ma-script.log