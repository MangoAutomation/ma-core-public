#!/bin/sh
#
#    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# Runs Mango Automation.

# Get standard environment variables
PRGDIR=`dirname "$0"`

#Uncommnet the following to log start up error
#exec >${PRGDIR}/logs/ma.out 2>${PRGDIR}/logs/ma.err

# Only set MA_HOME if not already set
[ -z "$MA_HOME" ] && MA_HOME=`cd "$PRGDIR" >/dev/null; pwd`

cd ${PRGDIR}

if [ ! -r "$MA_HOME"/ma-start.sh ]; then
    echo The MA_HOME environment variable is not defined correctly
    echo This environment variable is needed to run this program
    exit 1
fi

# Uncomment the following line to start with the debugger
# JPDA=-agentlib:jdwp=transport=dt_socket,address=8090,server=y,suspend=y

MA_CP=$MA_HOME/overrides/classes
MA_CP=$MA_CP:$MA_HOME/classes
MA_CP=$MA_CP:$MA_HOME/overrides/properties
for f in $MA_HOME/lib/*.jar
do
  MA_CP=$MA_CP:$f
done

if [ -z "$JAVA_HOME" ]; then
    EXECJAVA=java
else
    EXECJAVA=$JAVA_HOME/bin/java
fi

LOOP_EXIT=false
while [ $LOOP_EXIT = false ]; do
    $EXECJAVA $JPDA -server -cp $MA_CP \
    -Dma.home=$MA_HOME \
    -Djava.library.path=$MA/lib:$PATH \
    com.serotonin.m2m2.Main
    
    if [ ! -r "$MA_HOME"/RESTART ]; then
        LOOP_EXIT=true
    fi
done
