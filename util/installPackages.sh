#!/bin/sh
arg=$1
if [ "$arg" = "" ]; then
  echo
  echo "USAGE   : $0 PACKAGES.LIST" 
  echo "EXAMPLE : $0 packages.list.ubt"
  echo
else
  sudo aptitude update
  sudo aptitude install `sed -e '/^#/D' $1 | xargs`
  sudo ${0%/*}/pkg_install_ubuntu.sh
  sudo update-java-alternatives -s java-6-sun

  if [ "`dpkg -l | grep tvmet`" = "" ] ; then
    echo
    echo "#############################################################################"
    echo
    echo " After complete packge installation."
    echo " Please download and Install tvmet manually."
    echo ""
    echo " tvmet  : http://www.openrtp.jp/openhrp3/download/tvmet_1.7.1-1_i386.deb"
    echo 
    echo "#############################################################################"
    echo
  fi

fi
