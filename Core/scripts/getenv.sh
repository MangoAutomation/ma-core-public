#!/bin/sh

#
# Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
# @author Jared Wiltshire
#

set -e

if [ -z "$SCRIPT_DIR" ]; then
	SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
fi

# function for getting values from env.properties file
# does not support multiline properties or properties containing = sign
get_prop() {
	awk -v name="$1" -v dval="$2" -F = '$1==name{print $2; f=1; exit} END{if(f!=1){print dval}}' "$MA_ENV_PROPERTIES"
}

is_relative() {
	case "$1" in /*) return 1;; esac
}

resolve_path() {
	if is_relative "$2"; then
		printf %s/ "$1"
	fi
	printf %s "$2"
}

if [ -z "$MA_HOME" ]; then
	possible_ma_home="$(dirname -- "$SCRIPT_DIR")"
	if [ -e "$possible_ma_home/release.signed" ] || [ -e "$possible_ma_home/release.properties" ]; then
		MA_HOME="$possible_ma_home"
	else
		MA_HOME=/opt/mango
	fi
fi

if [ -z "$MA_ENV_PROPERTIES" ]; then
	MA_ENV_PROPERTIES="$MA_HOME/overrides/properties/env.properties"
fi

if [ ! -e "$MA_ENV_PROPERTIES" ]; then
	echo "Can't find env.properties file"
	exit 1
fi

if [ -z "$MA_KEYSTORE" ]; then
	keystore="$(get_prop "ssl.keystore.location" "$MA_HOME/overrides/keystore.p12")"
	MA_KEYSTORE="$(resolve_path "$MA_HOME" "$keystore")"
fi

if [ -z "$MA_KEYSTORE_PASSWORD" ]; then
	MA_KEYSTORE_PASSWORD="$(get_prop 'ssl.keystore.password' 'freetextpassword')"
fi

if [ -z "$MA_KEY_PASSWORD" ]; then
	MA_KEY_PASSWORD="$(get_prop 'ssl.key.password' "$MA_KEYSTORE_PASSWORD")"
fi

if [ -z "$MA_KEY_ALIAS" ]; then
	MA_KEY_ALIAS=mango
fi

keytool_cmd="$JAVA_HOME/bin/keytool"
if [ -z "$JAVA_HOME" ] || [ ! -x "$keytool_cmd" ]; then
	# use keytool from PATH
	keytool_cmd=keytool
fi
