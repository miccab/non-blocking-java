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
    suffix=`date +%Y%m%d%H%M%S`

    echo "Monitoring cpu utilization ..."
    sar -u 2 > monitor_cpuutil_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid > pids

    echo "Monitoring cpu switches ..."
    sar -w 2 > monitor_cpuswitch_${name}_${suffix} 2>&1 &
    last_pid=$!
    echo $last_pid >> pids

    java_server_pid=`ps -ef | grep java | grep jar | grep non-blocking | awk '{print $2}'`
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
#http://www.ibm.com/developerworks/library/j-nativememory-linux/

function monitor_threads() {
    pid=$1
    while true; do
        date
        jcmd $pid PerfCounter.print | grep "java.threads"
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
    start)
        start_monitoring $name
        ;;
    stop)
        stop_monitoring
        ;;
    *)
        echo "usage $0 start|stop"
        exit 1
esac




