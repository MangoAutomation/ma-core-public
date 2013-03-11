#!/bin/bash
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
done

chmod +x $MA_HOME/bin/*.sh
chmod +x $MA_HOME/bin/ext-available/*.sh

# Start MA
$MA_HOME/bin/ma.sh start &
