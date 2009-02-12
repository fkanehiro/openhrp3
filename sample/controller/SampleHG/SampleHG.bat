openhrp-controller-bridge ^
--server-name SampleHGController ^
--module SampleHG ^
--in-port angle:JOINT_VALUE ^
--in-port vel:JOINT_VELOCITY ^
--in-port acc:JOINT_ACCELERATION ^
--connection angle:angle ^
--connection vel:vel ^
--connection acc:acc ^

