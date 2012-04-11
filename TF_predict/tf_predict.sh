#! /bin/bash

# create basedir
OUT_FILE=$5
BASE_DIR=${OUT_FILE%.dat}"_files/"
if [ ! -d "$BASE_DIR" ]; then
    mkdir "$BASE_DIR"
fi

default="-i data/ipr.model -s data/super.model -r data/ipr.iprs -sr data/super.iprs -g data/DNA.go -t data/transHMan"

#replace __CR____CN__
#echo "$*"
args=$(echo $* | sed 's/__cr____cn__//g')
args=$(echo $args | sed 's/-sequence//g')
#echo $args

GALAXY_DIR=`pwd`"/../../../..";
cd $GALAXY_DIR"/tools/ra_tools/tf_predict/"
#echo "/usr/java/latest/bin/java -jar tf_predict.jar -basedir $BASE_DIR -p /opt/iprscan/bin/iprscan $default $args"
/usr/java/latest/bin/java -jar tf_predict.jar -basedir $BASE_DIR -p /opt/iprscan/bin/iprscan $default $args
