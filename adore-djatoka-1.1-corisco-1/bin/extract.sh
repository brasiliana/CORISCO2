#!/bin/sh

. ./env.sh

java -classpath ${CLASSPATH} ${JAVA_OPTS} gov.lanl.adore.djatoka.DjatokaExtract $*

exit 0
