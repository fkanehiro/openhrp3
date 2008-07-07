#!/bin/sh

CONTROLLER_BRIDGE_DIR=../../bin

$CONTROLLER_BRIDGE_DIR/ControllerBridge \
--server-name SampleSVController \
--module SampleSV.so \
--out-port steer:JOINT_VALUE \
--out-port vel:JOINT_VELOCITY \
--in-port torque:JOINT_TORQUE \
--connection steer:steer \
--connection vel:vel \
--connection torque:torque
