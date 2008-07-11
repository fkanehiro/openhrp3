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
  sudo update-java-alternatives -s java-6-sun

  if [ "`dpkg -l | grep tvmet`" = "" ] ; then
    echo
    echo "#############################################################################"
    echo
    echo " After complete packge installation."
    echo " Please download and Install JMF and tvmet manually."
    echo ""
    echo " tvmet : http://www.is.aist.go.jp/humanoid/OpenHRP/download/tvmet_1.7.1-1_i386.deb"
    echo 
    echo " JMF   : http://java.sun.com/products/java-media/jmf/2.1.1/download.html"
    echo " To build with default settings, install JMF at /usr/share/java."
    echo "#############################################################################"
    echo
  fi

fi
