#!/bin/sh

CONTROLLER_BRIDGE_DIR=../../bin

$CONTROLLER_BRIDGE_DIR/ControllerBridge \
--server-name SampleController \
--module SampleController.so \
--out-port angle:JOINT_VALUE \
--out-port rhsensor:rhsensor:FORCE_SENSOR \
--in-port torque:JOINT_TORQUE \
--connection angle:angle \
--connection rhsensor:rhsensor \
--connection torque:torque
