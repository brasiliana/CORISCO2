#!/bin/sh

. ./env.sh

java -classpath ${CLASSPATH} ${JAVA_OPTS} gov.lanl.adore.djatoka.DjatokaCompress $* 

exit 0
