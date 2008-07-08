#!/bin/bash

. config.sh
cd $DYNAMICS_SIMULATOR_DIR
./openhrp-aist-dynamics-simulator $NS_OPT
