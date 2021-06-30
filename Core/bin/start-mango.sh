#!/bin/sh

#
# Copyright (C) 2021 Radix IoT LLC. All rights reserved.
# @author Jared Wiltshire
# @author Matthew Lohbihler
#

set -e

# set options from arguments
for arg in "$@"; do
  case "$arg" in
  wait) mango_wait=true ;;
  esac
done

mango_script_dir="$(cd "$(dirname "$0")" && pwd -P)"
. "$mango_script_dir"/getenv.sh

mango_start

if [ "$mango_wait" = true ]; then
  # trap the SIGINT signal (Ctrl-C) and stop mango
  trap mango_stop INT TERM
  # needed for trap to work
  set +e
  # wait for Mango to exit
  wait $mango_pid
fi

exit 0
