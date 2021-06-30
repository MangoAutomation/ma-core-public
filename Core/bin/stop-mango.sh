#!/bin/sh

#
# Copyright (C) 2021 Radix IoT LLC. All rights reserved.
# @author Jared Wiltshire
#
set -e

mango_script_dir="$(cd "$(dirname "$0")" && pwd -P)"
. "$mango_script_dir"/getenv.sh

mango_stop "$1"
exit 0
