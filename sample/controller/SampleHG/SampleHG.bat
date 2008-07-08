SET CONTROLLER_BRIDGE_DIR=..\..\bridge

%CONTROLLER_BRIDGE_DIR%\ControllerBridge ^
--server-name SampleHGController ^
--module SampleHG.dll ^
--in-port angle:JOINT_VALUE ^
--in-port vel:JOINT_VELOCITY ^
--in-port acc:JOINT_ACCELERATION ^
--connection angle:angle ^
--connection vel:vel ^
--connection acc:acc ^


echooff@
REM For debug start
REM %CONTROLLER_BRIDGE_DIR%\ControllerBridged ^
REM --server-name SampleHGController ^
REM --module ./debug/SampleHG.dll ^
REM --in-port angle:JOINT_VALUE ^
REM --in-port vel:JOINT_VELOCITY ^
REM --in-port acc:JOINT_ACCELERATION ^
REM --connection angle:angle ^
REM --connection vel:vel ^
REM --connection acc:acc ^
echoon@
