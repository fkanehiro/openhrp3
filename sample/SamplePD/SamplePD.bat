SET CONTROLLER_BRIDGE_DIR=..\..\bridge

%CONTROLLER_BRIDGE_DIR%\ControllerBridge ^
--server-name SamplePDController ^
--module SamplePD.dll ^
--out-port angle:JOINT_VALUE ^
--in-port torque:JOINT_TORQUE ^
--connection angle:angle ^
--connection torque:torque

echooff@
REM For debug start
REM%CONTROLLER_BRIDGE_DIR%\ControllerBridged ^
REM--server-name SamplePDController ^
REM--module ./debug/SamplePD.dll ^
REM--out-port angle:JOINT_VALUE ^
REM--in-port torque:JOINT_TORQUE ^
REM--connection angle:angle ^
REM--connection torque:torque
echoon@
