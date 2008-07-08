SET CONTROLLER_BRIDGE_DIR=..\..\bridge

%CONTROLLER_BRIDGE_DIR%\ControllerBridge ^
--server-name SampleLFController ^
--module SampleLF.dll ^
--out-port angle:JOINT_VALUE ^
--out-port r_torque_out:RARM_WRIST_R:JOINT_TORQUE ^
--out-port l_torque_out:LARM_WRIST_R:JOINT_TORQUE ^
--in-port torque:JOINT_TORQUE ^
--connection angle:angle ^
--connection r_torque_out:r_torque_out ^
--connection l_torque_out:l_torque_out ^
--connection torque:torque


echooff@
REM For debug start
REM %CONTROLLER_BRIDGE_DIR%\ControllerBridged ^
REM --server-name SampleLFController ^
REM --module ./debug/SampleLF.dll ^
REM --out-port angle:JOINT_VALUE ^
REM --out-port r_torque_out:RARM_WRIST_R:JOINT_TORQUE ^
REM --out-port l_torque_out:LARM_WRIST_R:JOINT_TORQUE ^
REM --in-port torque:JOINT_TORQUE ^
REM --connection angle:angle ^
REM --connection r_torque_out:r_torque_out ^
REM --connection l_torque_out:l_torque_out ^
REM --connection torque:torque
echoon@
