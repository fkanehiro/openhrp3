openhrp-controller-bridge ^
--server-name SamplePDController ^
--out-port angle:JOINT_VALUE ^
--in-port torque:JOINT_TORQUE ^
--connection angle:SamplePD0:angle ^
--connection torque:SamplePD0:torque 

