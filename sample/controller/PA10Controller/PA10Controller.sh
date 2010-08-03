#!/bin/sh

openhrp-controller-bridge \
--server-name PA10Controller \
--out-port angle:JOINT_VALUE \
--out-port force_out:fsensor:FORCE_SENSOR \
--in-port torque:JOINT_TORQUE \
--connection angle:angle \
--connection torque:torque \
--connection force_out:force_out

