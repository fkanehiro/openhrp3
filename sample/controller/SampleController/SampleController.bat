openhrp-controller-bridge ^
--server-name SampleController ^
--module SampleController ^
--out-port angle:JOINT_VALUE ^
--out-port rhsensor:rhsensor:FORCE_SENSOR ^
--in-port torque:JOINT_TORQUE ^
--connection angle:angle ^
--connection rhsensor:rhsensor ^
--connection torque:torque

