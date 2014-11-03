#!/bin/sh
HEAP_MEM=-Xmx4096m
LOG4J=-Dlog4j.configuration=file:etc/log4j.properties
LIB=./lib
MAIN_CLASS=$1
for jarf in $LIB/*.jar
do
CLPA=$CLPA:$jarf
done
CLPA=$CLPA:bin/streamcorpus-indexer-0.1-SNAPSHOT.jar
CLPA=$CLPA:$HADOOP_CLASSPATH

# Add Hadoop conf to classpath
CLPA=$CLPA:/opt/cloudera/parcels/CDH/etc/hadoop/conf.pseudo.mr1
echo Starting....

export HADOOP_CLIENT_OPTS="-Xmx2048m $HADOOP_CLIENT_OPTS"
export HADOOP_MAPRED_HOME="/opt/cloudera/parcels/CDH/lib/hadoop-0.20-mapreduce"

#setup TERRIER_HOME
if [ ! -n "$TERRIER_HOME" ]
then
  #find out where this script is running
  TEMPVAR=`dirname $0`
  #terrier folder is folder above
  TERRIER_HOME=`pwd $TEMPVAR`
  echo "Setting TERRIER_HOME to $TERRIER_HOME"
fi
#setup TERRIER_ETC
if [ ! -n "$TERRIER_ETC" ]
then
  TERRIER_ETC=$TERRIER_HOME/etc
fi
export terrier.home=$TERRIER_HOME
java $HEAP_MEM $LOG4J -cp $CLPA -Dterrier.home=$TERRIER_HOME -Dterrier.setup=$TERRIER_HOME $MAIN_CLASS $2 $3 $4 $5 $6 $7 $8 $9 ${10} ${11} ${12} ${13} ${14} ${15} ${16} ${17} ${18} ${19} ${20}
