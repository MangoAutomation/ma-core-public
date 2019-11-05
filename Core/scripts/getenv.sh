#!/bin/sh

#
# Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
# @author Jared Wiltshire
#

set -e

if [ -z "$SCRIPT_DIR" ]; then
	SCRIPT="$0"
	[ -x "$(command -v greadlink)" ] && alias readlink=greadlink
	[ -x "$(command -v readlink)" ] && SCRIPT="$(readlink -f "$SCRIPT")"
	SCRIPT_DIR="$(dirname "$SCRIPT")"
fi

# function for getting values from .properties file
getProp() {
  awk -F "=" "/^$2=/ {print "'$2'"; exit; }" "$1"
}

if [ -z "$MA_HOME" ]
then
    possible_ma_home="$(dirname "$SCRIPT_DIR")"
    if [ -e "$possible_ma_home/release.signed" ] || [ -e "$possible_ma_home/release.properties" ]
    then
        MA_HOME="$possible_ma_home"
    else
        MA_HOME=/opt/mango
    fi
fi

if [ -z "$MA_ENV_PROPERTIES" ]
then
    MA_ENV_PROPERTIES="$MA_HOME/overrides/properties/env.properties"
fi

if [ -z "$MA_KEYSTORE" ]
then
    if [ -e "$MA_ENV_PROPERTIES" ]
    then
        MA_KEYSTORE=$(getProp "$MA_ENV_PROPERTIES" "ssl.keystore.location")
    fi

    if [ -z "$MA_KEYSTORE" ]
	then
	    MA_KEYSTORE="$MA_HOME/overrides/keystore.jks"
	fi
fi

if [ -z "$MA_KEYSTORE_PASSWORD" ]
then
    if [ -e "$MA_ENV_PROPERTIES" ]
    then
        MA_KEYSTORE_PASSWORD=$(getProp "$MA_ENV_PROPERTIES" "ssl.keystore.password")
    fi

    if [ -z "$MA_KEYSTORE_PASSWORD" ]
    then
        MA_KEYSTORE_PASSWORD=freetextpassword
    fi
fi

if [ -z "$MA_KEY_PASSWORD" ]
then
    if [ -e "$MA_ENV_PROPERTIES" ]
    then
        MA_KEY_PASSWORD=$(getProp "$MA_ENV_PROPERTIES" "ssl.key.password")
    fi

    if [ -z "$MA_KEY_PASSWORD" ]
    then
        MA_KEY_PASSWORD="$MA_KEYSTORE_PASSWORD"
    fi
fi

if [ -z "$MA_KEY_ALIAS" ]
then
    MA_KEY_ALIAS=mango
fi

keytool_cmd="$JAVA_HOME/bin/keytool"
if [ -z "$JAVA_HOME" ] || [ ! -x "$keytool_cmd" ]
then
    # use keytool from PATH
    keytool_cmd=keytool
fi
