set PATH=%PATH%;"C:\Program Files\OpenHRP\bin"
openhrp-controller-bridge ^
--server-name JoystickController ^
--out-port angle:JOINT_VALUE ^
--out-port velocity:JOINT_VELOCITY ^
--in-port torque:JOINT_TORQUE
