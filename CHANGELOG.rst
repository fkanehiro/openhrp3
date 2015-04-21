^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Changelog for package openhrp3
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Forthcoming
-----------

* IMU

  * [sample/model/sample1.wrl] rotate imu mount coordinate for debug
  * [hrplib/hrpModel/ForwardDynamics.cpp] Fix accel sensor frame discussed in https://github.com/fkanehiro/hrpsys-base/issues/472

* modelloader / projectGenerator

  * [server/modelLoader] rename export-collada to openhrp-export-collada
  * [server/modelLoader] fix ProjectGenerator to load BodyInfo and create ProjectFiles
  * [server/modelLoader] copy ProjectGenerator from hrpsys-base/util/ProjectGenerator

* export collada

  * [export-vrml] add --use-inline-shape option to output separate mesh files

* Solvers

  * [Eigen3d.h] use 1.0e-12 instaed of 1.0e-6 for error check
  * [hrplib/hrpUtil/MatrixSolvers] add calcSRInverse
  * [BodyInfoCollada_impl.cpp] fix for wrong collada interpretation,
    joint axis is in child frame

* misc

  * [sample/CMakeLists.txt] need to change command name from export-collada to openhrp-export-collada
  * super ugry hack for catkin build
  * Update .travis.yml
  * adds ppa repository without confirmation
  * create symlink for share directory for backword compatibility
  * changes openrtm-aist to openrtm-aist-dev and adds collada-dom-dev
  * changes PPA repository
  * fix problem when environment variable "_" not set
  * add dependency for ubuntu trusty
  * Fix test to match change python stub install location (fixes #36)
  * Change python stub install location (fixes #36)



3.1.7-0 (2014-10-10)
--------------------
・add package.xml and CMakeLists.txt for catkin compile
・disable java IDL compile by defualt
・enable java python compile by defualt
・convert wrl file into COLLADA file during compile
・add sample3dof robot model
・add .travis.yml file
・fix servo gain in PD controller 
・add CollisionDetector::colldingPairs()
・support OpenRTM 1.1.1
・fix bug in COLLADA loading

3.1.6-0 (2014-06-21)
--------------------
* fix inertia matrix conversion of ModelLoader
* fix segment's name of ModelLoader
* fix link's rotation of ModelLoader

3.1.5-6 (2014-04-15)
--------------------
* remove installed file if openhrp3_FOUND is not found
* Give installed libraries execute permissions
  All shared object libraries should have execute permissions. Using install will default the permissions to be like a normal file, which typically doesn't have execute permissions.
* Fix python syntax errors
  You cannot define a function called exec. This patch renames it to Exec.
* Handle non-existent lsb-release file
  This file is not present on Fedora systems.
* test_openhrp3.py: add test for samplerobot walking pattern data file
* test_openhpr3.py: add test code to check hrpsys-base
* add test code to check if file exists
* add start_omninames.sh start starts omniNames for test code, use port 2809 for test
* add test sample1.wrl location
* Add rostest for rosbuid, also improve .travis.yml to check rosbulid/deb environment
* (Makefile.openhrp3) touch patched_no_makefile to avoid compile twice
* add PKG_CONFIG_PATH for rosbuild environment
* (.travis.yml) add rosbuild/deb test
* (`#32 <https://github.com/start-jsk/openhrp3/issues/32>`_) add roslang for manifest.xml and package.xml
* (`#24 <https://github.com/start-jsk/openhrp3/issues/24>`_) add rosbuild, see https://github.com/ros/ros/issues/47
* check rosdep until it succeeded
* Fix cblas on Linux.
* Fix Boost linker error (remove -mt suffix).
* add link to issues for each patchs
* update travis to check rosbuild/catkin, use_deb/use_source
* Contributors: Benjamin Chrétien, Kei Okada, Scott Logan, Isaac Isao Saito

3.1.5-5 (2014-03-04)
--------------------
* Fix to an issue that caused https://github.com/start-jsk/hrpsys/issues/25
* Initial commit of CHANGELOG.rst
* Contributors: Kei Okada, chen.jsk, Ryohei Ueda, Isaac Isao Saito, Hiroyuki Mikita, Iori Kumagai, Takuya Nakaoka, Shunichi Nozawa, Rosen Diankov, Yohei Kakiuchi
