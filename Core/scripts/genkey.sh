#!/bin/sh

#
# Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
# @author Jared Wiltshire
#

set -e
umask 077

SCRIPT="$0"
[ -x "$(command -v greadlink)" ] && alias readlink=greadlink
[ -x "$(command -v readlink)" ] && SCRIPT="$(readlink -f "$SCRIPT")"
SCRIPT_DIR="$(dirname "$SCRIPT")"

. "$SCRIPT_DIR"/getenv.sh

"$keytool_cmd" -genkey -noprompt -keyalg RSA -keysize 2048 -alias "$MA_KEY_ALIAS" -dname "CN=$(hostname)" -keystore "$MA_KEYSTORE" -storepass "$MA_KEYSTORE_PASSWORD" -keypass "$MA_KEY_PASSWORD"

# ensure user read only permission
chmod 400 "$MA_KEYSTORE"
