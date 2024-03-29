#!/bin/sh

#
# Copyright (C) 2021 Radix IoT LLC. All rights reserved.
# @author Jared Wiltshire
#

set -e
umask 077

mango_script_dir="$(cd "$(dirname "$0")" && pwd -P)"
. "$mango_script_dir"/getenv.sh

mango_keystore_properties

if [ ! -x "$(command -v openssl)" ]; then
  err "openssl not found on path"
fi

openssl pkcs12 -export \
  -in "$RENEWED_LINEAGE/fullchain.pem" -inkey "$RENEWED_LINEAGE/privkey.pem" \
  -out "$RENEWED_LINEAGE/keystore.p12" -name "$MA_KEY_ALIAS" -passout pass:"$MA_KEYSTORE_PASSWORD"

# copy keystore to final destination, dont change destination ownership
cp "$RENEWED_LINEAGE/keystore.p12" "$MA_KEYSTORE"

# ensure user read only permission
chmod 400 "$MA_KEYSTORE"

# remove interim files
rm -f "$RENEWED_LINEAGE/keystore.p12"
