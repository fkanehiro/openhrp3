SET CONTROLLER_BRIDGE_DIR=..\..\bridge

%CONTROLLER_BRIDGE_DIR%\ControllerBridge ^
--server-name SampleSVController ^
--module SampleSV.dll ^
--out-port steer:JOINT_VALUE ^
--out-port vel:JOINT_VELOCITY ^
--in-port torque:JOINT_TORQUE ^
--connection steer:steer ^
--connection vel:vel ^
--connection torque:torque


echooff@
REM For debug start
REM %CONTROLLER_BRIDGE_DIR%\ControllerBridged ^
REM --server-name SampleSVController ^
REM --module ./debug/SampleSV.dll ^
REM --out-port steer:JOINT_VALUE ^
REM --out-port vel:JOINT_VELOCITY ^
REM --in-port torque:JOINT_TORQUE ^
REM --connection steer:steer ^
REM --connection vel:vel ^
REM --connection torque:torque
echoon@
