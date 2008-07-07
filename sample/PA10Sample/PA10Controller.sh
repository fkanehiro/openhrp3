#!/bin/sh

CONTROLLER_BRIDGE_DIR=../../bin

$CONTROLLER_BRIDGE_DIR/ControllerBridge \
--server-name PA10Controller \
--module PA10Controller.so \
--out-port angle:JOINT_VALUE \
--in-port torque:JOINT_TORQUE \
--connection angle:angle \
--connection torque:torque

