#!/bin/bash
#
#    Copyright (C) 2006-2013 Serotonin Software Technologies Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# -----------------------------------------------------------------------------
# The stop script for Mango Automation. 

if [ -z $MA_HOME ]; then
    echo Do not execute this file directly. Run \'ma.sh stop\' instead.
    exit 1
fi

# Run enabled stop extensions.
for f in $MA_HOME/bin/ext-enabled/*.sh
do
    source $f stop
done

# If there is a pid file, use it to kill the Java process.
if [ -f $MA_HOME/bin/ma.pid ]; then
    # Write a flag to the start script that the app was explicitly stopped.
    echo pleaseStop > "$MA_HOME"/STOP
    kill `cat "$MA_HOME"/bin/ma.pid`
    # kill -0 `cat "$CATALINA_PID"` >/dev/null 2>&1
else
    echo No PID file found. Java process not stopped  
fi
