#!/bin/bash
#
#    Copyright (C) 2006-2013 Serotonin Software Technologies Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# -----------------------------------------------------------------------------
# The initialization script for starting Mango Automation. This contains all of the 
# things that should happen only once, as opposed to things that happen each time 
# that MA restarts.

if [ -z $MA_HOME ]; then
    echo Do not execute this file directly. Run \'ma.sh start\' instead.
    exit 1
fi

# Delete a pid file if it exists
rm $MA_HOME/bin/ma.pid >/dev/null 2>/dev/null

# Determine the Java home
if [ -z "$JAVA_HOME" ]; then
    EXECJAVA=java
else
    EXECJAVA=$JAVA_HOME/bin/java
fi

# Run enabled init extensions.
for f in $MA_HOME/bin/ext-enabled/*.sh
do
    source $f init
done
