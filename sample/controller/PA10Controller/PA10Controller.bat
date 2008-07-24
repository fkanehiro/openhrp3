openhrp-controller-bridge ^
--server-name PA10Controller ^
--module PA10Controller.dll ^
--out-port angle:JOINT_VALUE ^
--in-port torque:JOINT_TORQUE ^
--connection angle:angle ^
--connection torque:torque

