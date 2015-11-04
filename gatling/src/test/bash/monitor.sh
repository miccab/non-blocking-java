#!/bin/bash

operation=$1
name=$2

function start_monitoring()
{
    if [ -f pids ]; then
        echo "Pids file already exists. Cannot start monitoring."
        exit 1
    fi
    name=$1
    suffix=$2

    echo "Monitoring cpu utilization ..."
    sar -u 2 > monitor_cpuutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid > pids

    echo "Monitoring cpu switches ..."
    sar -w 2 > monitor_cpuswitch_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> pids
 
    echo "Monitoring number of db connections ..."
    monitor_dbconn > monitor_dbconn_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> pids
}
function start_monitoring_java()
{
    name=$1
    suffix=`date +%Y%m%d%H%M%S`
    start_monitoring $name $suffix

    java_server_pid=`ps -ef | grep java | grep jar | grep non-blocking-1.0-SNAPSHOT | awk '{print $2}'`
    echo "Java pid: $java_server_pid"
    echo "Monitoring process mem utilization ..."
    top -d 2 -bp $java_server_pid > monitor_procmemutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> pids

    echo "Monitoring jvm mem utilization ..."
    jstat -gc $java_server_pid 2s > monitor_jvmmemutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> pids

    echo "Monitoring number of threads ..."
    monitor_threads $java_server_pid > monitor_threadnum_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> pids
}
function start_monitoring_nodejs()
{
    name=$1
    suffix=`date +%Y%m%d%H%M%S`
    start_monitoring $name $suffix

    java_server_pid=`ps -ef | grep 'node server.js' | grep -v grep | awk '{print $2}'`
    echo "Monitoring process mem utilization ..."
    top -d 2 -bp $java_server_pid > monitor_procmemutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> pids
}

function monitor_threads() {
    pid=$1
    while true; do
        date
        jcmd $pid PerfCounter.print | grep "java.threads"
        sleep 2
    done
}

function monitor_dbconn() {
    while true; do
        date
        netstat -an | grep :5432 | netstat -an | grep :5432 | grep -c ESTABLISHED
        sleep 2
    done
}


function stop_monitoring() {
    if [ -f pids ]; then
        while read pid; do
            echo "Killing $pid ..."
            kill -9 $pid
        done < pids
        rm -rf pids
    else
        echo "File with pids is missing. Nothing to stop."
    fi
}

case $operation in
    start_java)
        start_monitoring_java $name
        ;;
    start_nodejs)
        start_monitoring_nodejs $name
        ;;
    stop)
        stop_monitoring
        ;;
    *)
        echo "usage $0 start_java|start_nodejs|stop [name]"
        exit 1
esac




