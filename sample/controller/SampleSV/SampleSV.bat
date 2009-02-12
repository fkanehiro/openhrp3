openhrp-controller-bridge ^
--server-name SampleSVController ^
--module SampleSV ^
--out-port steer:JOINT_VALUE ^
--out-port vel:JOINT_VELOCITY ^
--in-port torque:JOINT_TORQUE ^
--connection steer:steer ^
--connection vel:vel ^
--connection torque:torque


