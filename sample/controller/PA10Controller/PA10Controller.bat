openhrp-controller-bridge ^
--server-name PA10Controller ^
--out-port angle:JOINT_VALUE ^
--in-port torque:JOINT_TORQUE ^
--connection angle:PA10Controller0:angle ^
--connection torque:PA10Controller0:torque

