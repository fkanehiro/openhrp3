#!/bin/sh

openhrp-controller-bridge \
--server-name SampleController \
--out-port angle:JOINT_VALUE \
--out-port rhsensor:rhsensor:FORCE_SENSOR \
--in-port torque:JOINT_TORQUE \
--connection angle:SampleController0:angle \
--connection rhsensor:SampleController0:rhsensor \
--connection torque:SampleController0:torque
