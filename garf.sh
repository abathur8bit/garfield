#!/bin/bash
# $@ - all command line params
# $1 - first param
# $# - number of command line params
java -cp out/production/nconsole -Djava.library.path=./cmake-build-debug com.axorion.NConsole $@
