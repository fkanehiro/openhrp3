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
# Generate deb files
sudo rm -f openhrp-aist*.deb
#   Generate development deb file
cmake -D DEBIANPACKAGE_DEVELOP:BOOL=ON .
sudo cpack -G DEB
#   Generate runtime deb file
cmake -D DEBIANPACKAGE_DEVELOP:BOOL=OFF .
sudo cpack -G DEB
DEB_FILES=`ls openhrp-aist*.deb`

cd ${WORK_DIR}
DEST_DIR="ubuntu/dists/${DISTRIB_CODENAME}/main/binary-i386/"

if [ ! -d ${DEST_DIR} ]; then
  mkdir -p ${DEST_DIR}
fi

for name in ${DEB_FILES}
do
  sudo cp ../../${name} ${DEST_DIR}
done

./makeDebPackageInfo.sh ubuntu dists/${DISTRIB_CODENAME}/main/binary-i386/

cd ${CURRENT_DIR}
