SET CONTROLLER_BRIDGE_DIR=..\..\bridge

%CONTROLLER_BRIDGE_DIR%\ControllerBridge ^
--server-name SampleController ^
--module SampleController.dll ^
--out-port angle:JOINT_VALUE ^
--out-port rhsensor:rhsensor:FORCE_SENSOR ^
--in-port torque:JOINT_TORQUE ^
--connection angle:angle ^
--connection rhsensor:rhsensor ^
--connection torque:torque

echooff@
REM For debug start
REM%CONTROLLER_BRIDGE_DIR%\ControllerBridged ^
REM--server-name SampleController ^
REM--module ./debug/SampleController.dll ^
REM--out-port angle:JOINT_VALUE ^
REM--out-port rhsensor:rhsensor:FORCE_SENSOR ^
REM--in-port torque:JOINT_TORQUE ^
REM--connection rhsensor:rhsensor ^
REM--connection angle:angle ^
REM--connection torque:torque
echoon@
