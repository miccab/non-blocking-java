#!/bin/bash

JVM_SETTINGS="-Xmx256m"
#JVM_SETTINGS="-Xms6g -Xmx6g -XX:NewSize=3g"
#JVM_SETTINGS="-XX:+UseG1GC -Xms6g -Xmx6g -XX:NewSize=3g"

if [[ "$1" = "a" ]]; then
  taskset -c 0 java $JVM_SETTINGS -jar target/non-blocking-1.0-SNAPSHOT.jar server product.yml
else
  java $JVM_SETTINGS -jar target/non-blocking-1.0-SNAPSHOT.jar server product.yml
fi


