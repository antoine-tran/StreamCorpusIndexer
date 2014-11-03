#!/bin/sh
HEAP_MEM=-Xmx4096m
LOG4J="-D log4j.configuration=file:etc/log4j.properties"
LIB=./lib
LOCAL_LIB=./ivy/local-repo
MAIN_CLASS=$1
for jarf in $LIB/*.jar
do
CLPA=$CLPA:$jarf
HCLPA=$HCLPA,$jarf
done
for jarf in $LOCAL_LIB/*.jar
do
CLPA=$CLPA:$jarf
HCLPA=$HCLPA,$jarf
done
CLPA=${CLPA:1:${#CLPA}-1}
HCLPA=${HCLPA:1:${#HCLPA}-1}
CLPD=$CLPA:bin/streamcorpus-indexer-0.1-SNAPSHOT.jar
HCLPD=$HCLPA,bin/streamcorpus-indexer-0.1-SNAPSHOT.jar
CLPA=$CLPA:$HADOOP_CLASSPATH

# Add Hadoop conf to classpath
CLPD=$CLPA:/opt/cloudera/parcels/CDH/lib/hadoop-0.20-mapreduce/conf
export HADOOP_CLASSPATH="$CLPD:$HADOOP_CLASSPATH"
echo Starting....

export HADOOP_CLIENT_OPTS="-Xmx2048m $HADOOP_CLIENT_OPTS"
export HADOOP_MAPRED_HOME="/opt/cloudera/parcels/CDH/lib/hadoop-0.20-mapreduce"
export HADOOP_HOME="/opt/cloudera/parcels/CDH/lib/hadoop-0.20-mapreduce"
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
hadoop jar bin/streamcorpus-indexer-0.1-SNAPSHOT.jar $MAIN_CLASS -libjars ${HCLPD} $LOG4J -D mapreduce.map.memory.mb=4096 -D mapreduce.map.java.opts=$HEAP_MEM -D mapred.output.compress=true -D mapred.output.compression.type=BLOCK -D mapred.output.compression.codec=org.apache.hadoop.io.compress.BZip2Codec -D mapred.compress.map.output -D mapred.map.output.compression.codec=org.apache.hadoop.io.compress.SnappyCodec -D mapred.task.timeout=1200000 -D terrier.home="$TERRIER_HOME" $2 $3 $4 $5 $6 $7 $8 $9 ${10} ${11} ${12} ${13} ${14} ${15} ${16} ${17} ${18} ${19} ${20}
