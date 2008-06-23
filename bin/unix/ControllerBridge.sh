#!/bin/bash

. config.sh
if [ $1 ];
then
  export CONTROLLER_BRIDGE_CONFIG=$1
fi
cd $OPENHRPHOME/bin
./ControllerBridge $NS_OPT
