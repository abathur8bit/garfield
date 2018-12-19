#!/bin/bash
# $@ - all command line params
# $1 - first param
# $# - number of command line params

# Run normally
# java -cp out/production/garfield:out/production/nconsole -Djava.library.path=cmake-build-debug Garfield $@

# Remote debugging since you can't debug a ncurses app in IntelliJ
java -cp out/production/garfield:out/production/nconsole -Djava.library.path=cmake-build-debug -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 Garfield $@
