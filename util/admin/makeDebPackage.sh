#!/bin/bash
CURRENT_DIR=${PWD}
cd ${0%/*}
WORK_DIR=${PWD}

# Set dsitribution code name
if [ -z $1 ]; then 
  source /etc/lsb-release
else
  DISTRIB_CODENAME=$1
fi

cd ../../
# Get latest *.deb file
sudo cpack -G DEB
DEB_FILES=`ls -t *.deb`
for name in ${DEB_FILES}
do
  DEB_FILE=${name}
  break
done

cd ${WORK_DIR}
DEST_DIR="ubuntu/dists/${DISTRIB_CODENAME}/main/binary-i386/"

if [ ! -d ${DEST_DIR} ]; then
  mkdir -p ${DEST_DIR}
fi

sudo cp ../../${DEB_FILE} ${DEST_DIR}
./makeDebPackageInfo.sh ubuntu dists/${DISTRIB_CODENAME}/main/binary-i386/

cd ${CURRENT_DIR}
