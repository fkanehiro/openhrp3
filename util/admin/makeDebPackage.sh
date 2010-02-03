#!/bin/bash
CURRENT_DIR=${PWD}
cd ${0%/*}
WORK_DIR=${PWD}
PACKAGES_DIR='util/admin/deb'

# Set dsitribution code name
if [ -z $1 ]; then 
  source /etc/lsb-release
else
  DISTRIB_CODENAME=$1
fi

cd ../../

# Generate development deb file
#cmake -D DEBIANPACKAGE_DEVELOP:BOOL=ON -D GENERATE_DEBIANPACKAGE:BOOL=ON .
cmake -D GENERATE_DEBIANPACKAGE:BOOL=ON .
sudo cpack -G DEB
#   Generate runtime deb file
#cmake -D DEBIANPACKAGE_DEVELOP:BOOL=OFF -D GENERATE_DEBIANPACKAGE:BOOL=ON .
#sudo cpack -G DEB

sudo mv -f openhrp-aist*.deb ${PACKAGES_DIR}

DEB_FILES=`ls ${PACKAGES_DIR}/*.deb`

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
