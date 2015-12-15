#!/bin/bash
case "$1" in
    init)
        # Logs start up message from sysout and syserr to ma.out and ma.err respectively
        SYSOUT="$MA_HOME"/logs/ma.out
        SYSERR="$MA_HOME"/logs/ma.err
        ;;
esac
