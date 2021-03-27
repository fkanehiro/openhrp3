## projectGenerator

generate a template project file

ProjectGenerator [input files] [options]

input files shold be VRML or COLLADA files.

#### option

* ``--output [output file]``  
   Specify output file path (Required)  
   For example, when output file is test.xml, ProjectGenerator generates test.xml, test.conf, and test.RobotHardware.conf.

* ``--integrate [true or false]``  
   Use forward dynamics mode or kinematics mode (by default, true).

* ``--dt [dt]``  
   dt is controllers' time step[s] (by default, 0.005[s]).

* ``--timestep [timestep]``  
   timestep is simulator time step[s] (by default, 0.005[s]).

* ``--conf-file-option [conf file option]``  
   conf file option is added to controller's config file such as test.conf.

* ``--robothardware-conf-file-option [robothardware conf file option]``  
   robothardware conf file option is added to robothardware's config file such as test.Robothardware.conf.

* ``--joint-properties [joint properties]``  
   joint properties are properties for each joint. Specify property name and property value.
   For example, --joint-properties RLEG_JOINT0.angle,0,RLEG_JOINT1.mode,Torque

* ``--use-highgain-mode [true or false]``  
   Use HighGain mode for robot's joints or Torque mode (by default, use true, use HighGain mode).

* ``--method [EULER or RUNGE_KUTTA]``  
   Integration method (EULER by default).

#### example

```bash
openhrp-project-generator `rospack find openhrp3`/share/OpenHRP-3.2/sample/model/sample1.wrl `rospack find openhrp3`/share/OpenHRP-3.2/sample/model/longfloor.wrl --use-highgain-mode false --output /tmp/SampleRobot_for_torquecontrol.xml --timeStep 0.001 --dt 0.002
```

## NOTE

**projectGenerator** will overwrite your original configuration files (xxx.xml / xxx.conf / xxx.RobotHardware.conf) when there already exists original ones in your output direcrotry because this program generates xxx.xml, xxx.conf and xxx.RobotHardware.conf at the same time in the same directory.
