#!/bin/sh
arg=$1
if [ "$arg" = "" ]; then
  echo
  echo "USAGE   : $0 PACKAGES.LIST" 
  echo "EXAMPLE : $0 packages.list.ubt"
  echo
else
  sudo ${0%/*}/pkg_install_ubuntu.sh
  sudo apt-get --force-yes install `sed -e '/^#/D' $1 | xargs`
  sudo update-java-alternatives -s java-6-sun
fi
