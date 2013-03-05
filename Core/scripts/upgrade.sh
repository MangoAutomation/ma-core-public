#!/bin/sh
#
#    Copyright (C) 2006-2013 Serotonin Software Technologies Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# Upgrades Mango Automation core.

# Delete jars and work dir
rm $MA_HOME/lib/*.jar
rm -Rf $MA_HOME/work

# Unzip core. The exact name is unknown, but there should only be one, so iterate
for f in $MA_HOME/m2m2-core-*.zip
do
    unzip -o $f
    rm $f
    chmod +x $MA_HOME/ma-start.sh
done

# This is a *nix script, so we don't need the start batch file
rm $MA_HOME/ma-start.bat

# If this instance has a start script in overrides, copy it in.
if [ -r "$MA_HOME"/overrides/ma-start.sh ]; then
    cp $MA_HOME/overrides/ma-start.sh $MA_HOME
fi

# Start MA
sh $MA_HOME/ma-start.sh
