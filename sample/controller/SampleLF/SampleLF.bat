openhrp-controller-bridge ^
--server-name SampleLFController ^
--out-port angle:JOINT_VALUE ^
--out-port r_torque_out:RARM_WRIST_R:JOINT_TORQUE ^
--out-port l_torque_out:LARM_WRIST_R:JOINT_TORQUE ^
--in-port torque:JOINT_TORQUE ^
--connection angle:angle ^
--connection r_torque_out:r_torque_out ^
--connection l_torque_out:l_torque_out ^
--connection torque:torque

