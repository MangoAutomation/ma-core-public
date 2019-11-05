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

openssl pkcs12 -export \
    -in "$RENEWED_LINEAGE/fullchain.pem" -inkey "$RENEWED_LINEAGE/privkey.pem" \
    -out "$RENEWED_LINEAGE/keystore.p12" -name "$MA_KEY_ALIAS" -passout pass:"$MA_KEYSTORE_PASSWORD"

"$keytool_cmd" -importkeystore -noprompt \
    -srckeystore "$RENEWED_LINEAGE/keystore.p12" -srcstoretype PKCS12 -srcstorepass "$MA_KEYSTORE_PASSWORD" \
    -srcalias "$MA_KEY_ALIAS" \
    -destkeystore "$RENEWED_LINEAGE/keystore.jks" -deststorepass "$MA_KEYSTORE_PASSWORD" \
    -destkeypass "$MA_KEY_PASSWORD" -destalias "$MA_KEY_ALIAS" \
    2> /dev/null

# copy keystore to final destination, dont change destination ownership
cp "$RENEWED_LINEAGE/keystore.jks" "$MA_KEYSTORE"

# ensure user read only permission
chmod 400 "$MA_KEYSTORE"

# remove interim files
rm -f "$RENEWED_LINEAGE/keystore.p12" "$RENEWED_LINEAGE/keystore.jks"
