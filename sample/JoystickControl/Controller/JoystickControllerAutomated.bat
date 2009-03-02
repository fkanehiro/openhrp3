start "JoystickController" "JoystickController.exe"
cd ..\TkJoyStick
start "TkJoyStickComp" "TkJoyStickComp.py"
cd ..\Controller

..\..\..\bin\openhrp-controller-bridge ^
--server-name JoystickController ^
--out-port angle:JOINT_VALUE ^
--out-port velocity:JOINT_VELOCITY ^
--in-port torque:JOINT_TORQUE ^
--connection angle:JoystickController0:angle ^
--connection velocity:JoystickController0:velocity ^
--connection torque:JoystickController0:torque
