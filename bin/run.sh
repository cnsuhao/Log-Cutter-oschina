#!/bin/bash

echo

if [ "$JRE_HOME" != "" ]
then
    _JAVA_HOME="$JRE_HOME"
    echo Using  JRE_HOME : $_JAVA_HOME
else
    _JAVA_HOME="$JAVA_HOME"
    echo Using JAVA_HOME : $_JAVA_HOME
fi

_CLASSPATH="$CLASSPATH"
echo Using CLASSPATH : $_CLASSPATH

_JAVA=$_JAVA_HOME/bin/java

APP_PATH="$(cd "$(dirname "$0")" && pwd)/.."
APP_CLASSPATH=$APP_PATH/classes
APP_LIBPATH=$APP_PATH/lib
APP_MAIN_CLASS=org.jessma.logcutter.main.LogCutter

ARGS=
DAEMON='false'
CMD="$_JAVA -Duser.dir=$APP_PATH -Djava.ext.dirs=$APP_LIBPATH -cp $APP_CLASSPATH:$_CLASSPATH $APP_MAIN_CLASS"

echo

while (( $# > 0 ))
do
    if [ "$1" == '-d' ]
    then
        DAEMON='true'
    else
        ARGS="$ARGS $1"
    fi
    
    shift
done

if [ "$DAEMON" == 'false' ]
then
    $CMD $ARGS
else
    $CMD $ARGS &
fi
