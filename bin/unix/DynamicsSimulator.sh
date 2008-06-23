#!/bin/bash

. config.sh
cd $DYNAMICS_SIMULATOR_DIR
./DynamicsSimulator $NS_OPT
