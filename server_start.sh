#!/bin/bash

JVM_SETTINGS="-Xmx256m"
#JVM_SETTINGS="-Xms6g -Xmx6g -XX:NewSize=3g"
#JVM_SETTINGS="-XX:+UseG1GC -Xms6g -Xmx6g -XX:NewSize=3g"

instance=$1

echo "Starting instance $instance ..."

DW_SETTINGS=" -Ddw.server.applicationConnectors[0].port=808${instance}"

if [[ "$2" = "a" ]]; then
  taskset -c $instance java $JVM_SETTINGS $DW_SETTINGS -jar target/non-blocking-1.0-SNAPSHOT.jar server product.yml
else
  java $JVM_SETTINGS $DW_SETTINGS -jar target/non-blocking-1.0-SNAPSHOT.jar server product.yml
fi


