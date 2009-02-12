#!/bin/sh

openhrp-controller-bridge \
--server-name SamplePDController \
--module SamplePD \
--out-port angle:JOINT_VALUE \
--in-port torque:JOINT_TORQUE \
--connection angle:angle \
--connection torque:torque
