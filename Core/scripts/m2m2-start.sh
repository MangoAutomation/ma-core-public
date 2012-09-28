#!/bin/sh
#
#    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# Runs Mango M2M2.

# Get standard environment variables
PRGDIR=`dirname "$0"`

# Only set M2M2_HOME if not already set
[ -z "$M2M2_HOME" ] && M2M2_HOME=`cd "$PRGDIR" >/dev/null; pwd`

if [ ! -r "$M2M2_HOME"/m2m2-start.sh ]; then
    echo The M2M2_HOME environment variable is not defined correctly
    echo This environment variable is needed to run this program
    exit 1
fi

# Uncomment the following line to start with the debugger
# JPDA=-agentlib:jdwp=transport=dt_socket,address=8090,server=y,suspend=y

M2M2_CP=$M2M2_HOME/overrides/classes
M2M2_CP=$M2M2_CP:$M2M2_HOME/classes
M2M2_CP=$M2M2_CP:$M2M2_HOME/overrides/properties
for f in $M2M2_HOME/lib/*.jar
do
  M2M2_CP=$M2M2_CP:$f
done

if [ -z "$JAVA_HOME" ]; then
    EXECJAVA=java
else
    EXECJAVA=$JAVA_HOME/bin/java
fi

LOOP_EXIT=false
while [ $LOOP_EXIT = false ]; do
    $EXECJAVA $JPDA -server -cp $M2M2_CP \
    -Dm2m2.home=$M2M2_HOME \
    -Djava.library.path=$M2M2/lib:$PATH \
    com.serotonin.m2m2.Main
    
    if [ ! -r "$M2M2_HOME"/RESTART ]; then
        LOOP_EXIT=true
    fi
done
