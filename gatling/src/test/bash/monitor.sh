#!/bin/bash

operation=$1
name=$2
explicit_pid=$3

function start_monitoring()
{
    name=$1
    suffix=$2
    mypid=$3
    pidfile="pids${mypid}"
    if [ -f $pidfile ]; then
        echo "Pids file already exists. Cannot start monitoring."
        exit 1
    fi

    echo "Monitoring cpu utilization ..."
    sar -u 2 > monitor_cpuutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid > $pidfile

    echo "Monitoring cpu switches ..."
    sar -w 2 > monitor_cpuswitch_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> $pidfile
 
    echo "Monitoring number of db connections ..."
    monitor_dbconn > monitor_dbconn_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> $pidfile

    echo "Monitoring process stats..."
    perf stat -p $mypid > monitor_process_perf_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> $pidfile
}
function start_monitoring_java()
{
    name=$1
    suffix=`date +%Y%m%d%H%M%S`
    java_server_pid="$2"
    pidfile="pids${java_server_pid}"
    if [ -z "$java_server_pid" ]; then
        java_server_pid=`ps -ef | grep java | grep jar | grep non-blocking-1.0-SNAPSHOT | awk '{print $2}'`
    else
        name="${name}_${java_server_pid}"
    fi
    echo "Java pid: $java_server_pid"
    start_monitoring $name $suffix $java_server_pid

    echo "Monitoring process mem utilization ..."
    top -d 2 -bp $java_server_pid > monitor_procmemutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> $pidfile

    echo "Monitoring jvm mem utilization ..."
    jstat -gc $java_server_pid 2s > monitor_jvmmemutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> $pidfile

    echo "Monitoring number of threads ..."
    monitor_threads $java_server_pid > monitor_threadnum_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> $pidfile
}
function start_monitoring_nodejs()
{
    name=$1
    suffix=`date +%Y%m%d%H%M%S`
    java_server_pid="$2"
    pidfile="pids${java_server_pid}"
    if [ -z "$java_server_pid" ]; then
        java_server_pid=`ps -ef | grep 'node server.js' | grep -v grep | awk '{print $2}'`
    else
        name="${name}_${java_server_pid}"
    fi
    start_monitoring $name $suffix $java_server_pid

    echo "Monitoring process mem utilization ..."
    top -d 2 -bp $java_server_pid > monitor_procmemutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> $pidfile
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
    explicit_pid="$1"
    if [ -f "pids${explicit_pid}" ]; then
        while read pid; do
            echo "Killing $pid ..."
            kill -SIGINT $pid
            sleep 1
            kill -9 $pid > /dev/null 2>&1
        done < "pids${explicit_pid}"
        rm -rf "pids${explicit_pid}"
    else
        echo "File with pids is missing. Nothing to stop."
    fi
}

case $operation in
    start_java)
        start_monitoring_java $name $explicit_pid
        ;;
    start_nodejs)
        start_monitoring_nodejs $name $explicit_pid
        ;;
    stop)
        stop_monitoring $explicit_pid
        ;;
    *)
        echo "usage $0 start_java|start_nodejs|stop [name] [pid]"
        exit 1
esac




