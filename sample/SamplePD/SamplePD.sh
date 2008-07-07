#!/bin/sh

CONTROLLER_BRIDGE_DIR=../../bin

$CONTROLLER_BRIDGE_DIR/ControllerBridge \
--server-name SamplePDController \
--module SamplePD.so \
--out-port angle:JOINT_VALUE \
--in-port torque:JOINT_TORQUE \
--connection angle:angle \
--connection torque:torque
