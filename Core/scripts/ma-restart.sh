#!/bin/bash
#
#    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# -----------------------------------------------------------------------------
# The restart script for Mango Automation. 

if [ -z "$MA_HOME" ]; then
    echo Do not execute this file directly. Run \'ma.sh restart\' instead.
    exit 1
fi

# If there is a pid file, use it to kill the Java process.
if [ -f "$MA_HOME"/bin/ma.pid ]; then
    echo `date` 'ma-restart: restart initiated...' >> "$MA_HOME"/logs/ma-script.log
    echo pleaseRestart > "$MA_HOME"/RESTART
    kill `cat "$MA_HOME"/bin/ma.pid`
else
    echo No PID file found. Java process not restarted  
fi
