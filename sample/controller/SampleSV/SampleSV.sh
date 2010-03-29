#!/bin/sh

openhrp-controller-bridge \
--server-name SampleSVController \
--out-port steer:JOINT_VALUE \
--out-port vel:JOINT_VELOCITY \
--in-port torque:JOINT_TORQUE \
--connection steer:SampleSV0:steer \
--connection vel:SampleSV0:vel \
--connection torque:SampleSV0:torque
