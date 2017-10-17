#!/bin/bash
#
#    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# Upgrades Mango Automation core.

# Delete jars and work dir
rm -f "$MA_HOME"/lib/*.jar
rm -Rf "$MA_HOME"/work

# Delete the release properties files
rm -f "$MA_HOME"/release.properties
rm -f "$MA_HOME"/release.signed

# Unzip core. The exact name is unknown, but there should only be one, so iterate
for f in "$MA_HOME"/m2m2-core-*.zip
do
    unzip -o "$f"
    rm "$f"
done

chmod +x "$MA_HOME"/bin/*.sh
chmod +x "$MA_HOME"/bin/ext-available/*.sh

#This feature does not exist but could be added in future releases
# Execute any one-off scripts there may be. Delete when done.
#for f in "$MA_HOME"/bin/upgrade/*.sh
#do
#    $f
#    rm $f
#done


# Start MA
"$MA_HOME"/bin/ma.sh start &
