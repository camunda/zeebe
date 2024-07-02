#!/bin/bash

#####################################################################################################################################################################
#                                                                                                                                                                   #
#                                        Optimize Reimport Script                                                                                                   #
#                                                                                                                                                                   #
#   Purges imported engine data from the Optimize database in order to perform a reimport of engine data                                                            #
#   without losing your Optimize data (e.g. report definitions). See:                                                                                               #
#   https://docs.camunda.io/optimize/${docs.version}/self-managed/optimize-deployment/migration-update/instructions/#force-reimport-of-engine-data-in-optimize      #
#                                                                                                                                                                   #
#####################################################################################################################################################################

cd $(dirname "$0")
BASEDIR=$(pwd)
export PLUGIN_DIR="${BASEDIR}/../plugin"

# now set the path to java
if [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  set +e
  JAVA=`which java`
  set -e
fi

# check if there are custom JVM options set.
if [ -z "$OPTIMIZE_JAVA_OPTS" ]; then
  OPTIMIZE_JAVA_OPTS="-Xms128m -Xmx512m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=256m"
fi

# Set up the classpath
OPTIMIZE_CLASSPATH="${BASEDIR}/../config:${BASEDIR}/*:${BASEDIR}/../lib/*:${BASEDIR}/../optimize-backend-${project.version}.jar"

echo
echo "Starting Camunda Optimize Reimport ${project.version}...";
echo

exec $JAVA ${OPTIMIZE_JAVA_OPTS} -cp $OPTIMIZE_CLASSPATH -Dfile.encoding=UTF-8 io.camunda.optimize.reimport.preparation.ReimportPreparation
