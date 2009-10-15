#!/bin/bash

WORK_DIR="$1"
DIR_VALUE="$2"

function errorMessage() {
    if [ -z "$1" ]; then
      echo "Working and target directory do not exist."
    else
      echo "$1 dose not exist."
    fi
    echo "usage:  ./${0##*/} <work dir> <target dir>"
    echo 'work dir: Working directory path.'
    echo 'target dir: Target directory is relative <work dir> path.'
    exit -1
}

if [ -z ${WORK_DIR} -a -z ${WORK_DIR}/${DIR_VALUE}  ]; then
  errorMessage
else
  cd ${WORK_DIR}
  apt-ftparchive packages ${DIR_VALUE} | gzip -9c > ${DIR_VALUE}/Packages.gz
# apt-ftparchive sources ${DIR_VALUE} | gzip -9c > ${DIR_VALUE}/Sources.gz
  apt-ftparchive contents ${DIR_VALUE} | gzip -9c > ${DIR_VALUE}/Contents.gz
  apt-ftparchive release ${DIR_VALUE} > ${DIR_VALUE}/Release
  cd ${OLDPWD}
fi
