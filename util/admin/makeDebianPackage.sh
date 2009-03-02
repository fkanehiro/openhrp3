#!/bin/bash
#Set default value
VER_VAL='3.0.2'
if ! [ -z "$1" ]; then
    VER_VAL=$1
fi

cd ${0%/*}
echo ${PWD}
sudo ./layoutDebian.rb ../../ OpenHRP3.0.x-debian-package-list debian-package/openhrp-aist-${VER_VAL}
sudo find debian-package/openhrp-aist-${VER_VAL} -type d -regex '.*\.svn' | sudo xargs rm -rf
sudo cp -r debian-package/DEBIAN debian-package/openhrp-aist-${VER_VAL}/
sudo chown -R root:root debian-package/openhrp-aist-${VER_VAL}
sudo dpkg-deb -b debian-package/openhrp-aist-${VER_VAL} debian-package/openhrp-aist_${VER_VAL}_i386.deb
cd ${OLDPWD}
