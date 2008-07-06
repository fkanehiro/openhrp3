#!/bin/sh
. ./config.sh

##
# GrxUI usage:
#
#   ./GrxUI.sh "" "" "" on
#   ./GrxUI.sh project/HRP2 OpenHRP3 script/test.py off
##
DEFAULT_PROJECT=$1
DEFAULT_MODE=$2
SCRIPT=$3
DEBUG=$4

## 
# for client/gui/grxuirc.xml
##
BIN_DIR=${OPENHRPHOME}/bin/unix
BIN_SFX=.sh

if [ -f ${OPENHRPHOME}/bin/unix/omninames-*.log ] ; then
/bin/rm ${OPENHRPHOME}/bin/unix/omninames-*.*;
fi

cd ../../client/gui
${JAVAVM} -server -Xmx512m \
	-DPROJECT="${DEFAULT_PROJECT}" \
	-DMODE="${DEFAULT_MODE}" \
	-DSCRIPT="{$SCRIPT}" \
	-DDEBUG=${DEBUG} \
	-DNS_OPT="${NS_OPT}" \
	-DOPENHRPHOME=${OPENHRPHOME} \
	-DBIN_DIR=${BIN_DIR} \
	-DBIN_SFX=${BIN_SFX} \
	-Dpython.home=${JYTHON_HOME} \
	-Dpython.cachedir=$HOME/.jython-cache \
	com.generalrobotix.ui.GrxUI

##
# NOTE:
#   to startup GrxUI from eclipse 
#   set argument -D??? : OPENHRPHOME , BIN_DIR , BIN_SFX
##
