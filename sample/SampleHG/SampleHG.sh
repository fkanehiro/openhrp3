#!/bin/sh

CONTROLLER_BRIDGE_DIR=../../bin

$CONTROLLER_BRIDGE_DIR/ControllerBridge \
--server-name SampleHGController \
--module SampleHG.so \
--in-port angle:JOINT_VALUE \
--in-port vel:JOINT_VELOCITY \
--in-port acc:JOINT_ACCELERATION \
--connection angle:angle \
--connection vel:vel \
--connection acc:acc
