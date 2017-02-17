^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Changelog for package openhrp3
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

3.1.9 (2017-02-17)
------------------

* supports OpenRTM-aist 1.2.0 (`c04e9293 <https://github.com/fkanehiro/openhrp3/commit/c04e92930af318d6566213dd173c34331eb18898>`_)
* supports trunk version of OpenRTM-aist (`#104 <https://github.com/fkanehiro/openhrp3/issues/104>`_)
  * hrplib/{hrpModel/ModelNodeSet.h, TriangleMeshShaper.h} uses signals2 for BOOST_VERSION >= 103900 (`#103 <https://github.com/fkanehiro/openhrp3/issues/103>`_)
* add configuration for openrtm ver-1.1.2 (`#84 <https://github.com/fkanehiro/openhrp3/issues/84>`_)
* SUpport ARM64: aarch64(ARMv8) is also 64bit machine (`#100 <https://github.com/fkanehiro/openhrp3/issues/100>`_)
  * hrplib/hrpPlanner/TimeUtil.cpp : fix get_tick() for arm processors (`#99 <https://github.com/fkanehiro/openhrp3/issues/99>`_ )
* Support clang and Fix some memory leaks (conservative fixes only) (`#113 <https://github.com/fkanehiro/openhrp3/issues/113>`_)
* supports ubuntu 16.04 (`2faa042f0 <https://github.com/fkanehiro/openhrp3/commit/2faa042f0ce5e2b8ac6b03c94feb3e95ab076e1d>`_)
* CMakeLists.txt: fix to work on Boost 1.34.0 (`#89 <https://github.com/fkanehiro/openhrp3/issues/89>`_ )
* .traivs.yml: add to check if this work with current hrpsys (`#70 <https://github.com/fkanehiro/openhrp3/issues/70>`_)
* layout python stub to right place (`#69 <https://github.com/fkanehiro/openhrp3/issues/69>`_ )
  * idl_py_files to lib/python${python_version}/dist-packages
  * module_py_files to lib/python${python_version}/dist-packages/OpenHRP
  * module_poa_py_files to lib/python${python_version}/dist-packages/OpenHRP__POA

* installPackages
  * util/installPackages.sh: Fixes on the script (`#96 <https://github.com/fkanehiro/openhrp3/issues/96>`_)
    * Fix the tests on empty strings
    * Fix shebang for better compatibility
  * util/installPackages.sh: fixes a typo (`#95 <https://github.com/fkanehiro/openhrp3/issues/95>`_)
  * util/packages.list.ubuntu.15.04 : adds a package list for ubuntu15.04 (`ce8dc77f <https://github.com/fkanehiro/openhrp3/commit/ce8dc77f20f2f755f242b0c8ca3c9af7da278bf9>`_)

* Fix many compile warning
  * fixes some of warnings detected by -Wall (`#118 <https://github.com/fkanehiro/openhrp3/issues/118>`_ )
  * fixes warnings detected by -Wsign-compare / restores return type of calcSRInverse() (`#117 <https://github.com/fkanehiro/openhrp3/issues/117>`_)
  * fixes warnings detected by -Wreorder (`#114 <https://github.com/fkanehiro/openhrp3/issues/114>`_)
  * Reduce Warnings (`#102 <https://github.com/fkanehiro/openhrp3/issues/102>`_)
    * Reorder includes for clang
      Clang doesn't allow the overloaded operator <<= used in the template
      function CORBA_Util::typecode::id() to be declared after that point of
      use.  It seems to be a bug in clang.
    * Add missing cases
    * Remove "this != null" checks
      These conditionals are never true in valid C++ programs.
    * Add abort to "impossible" paths
    * Add parens to indicate intentional assignment
    * Fix comparison where it should be assignment
    * Streamline definition of PI and PI_2
      C++ standard (at least prior to C++11) specifies that static const
      double members cannot be initialized within the class definition.  Move
      the initialization of PI and PI_2 outside the class; also, update
        feature test macros to use M_PI and M_PI_2 whenever they're available.
    * Disambiguate if-else
    * Fix friend declarations
      friend declarations can't contain default parameters unless the function
      body is defined at the same site.
  * hrplib/hrpModel/Body.cpp, ModelLoaderUtil.cpp, fixes warnings (false -> NULL) (`#101 <https://github.com/fkanehiro/openhrp3/issues/101>`_)

* hrplib/hrpModel
  * hrplib/hrpModel/World.h: changes return type of World::numBodies() from int to unsinged int (`#116 <https://github.com/fkanehiro/openhrp3/issues/116>`_ )
  * hrplib/hrpModel/{Body.h,JointPath.h,LinkTraverse.h} : changes return types of numXXX() (`#115 <https://github.com/fkanehiro/openhrp3/issues/115>`_ )
  * hrplib/hrpModel/Body.cpp: supports slide joints (Link:SLIDE_JOINT) in calcCMJacobian() (`7b674f88 <https://github.com/fkanehiro/openhrp3/commit/7b674f88af1100ae0d85bdc6c45cb1f18ae648ea>`_)
  * hrplib/hrpModel/Body.cpp: fixes a bug in calcInverseKinematics (`1ce8d36d7 <https://github.com/fkanehiro/openhrp3/commit/1ce8d36d72685e4bfe92912ec13cced754c0240a>`_)
  * hrplib/hrpModel/ModelNodeSet.cpp: PROTO Surfaceのあるモデルが読み込めないバグの修正 (`#66 <https://github.com/fkanehiro/openhrp3/issues/66>`_)

* hrplib/hrpPlanner
  * Extend planner (`#112 <https://github.com/fkanehiro/openhrp3/issues/112>`_, `#111 <https://github.com/fkanehiro/openhrp3/issues/111>`_)
    * removes redundant way point in a path /
    * changes type of extraConnectionCheckFunc
    * enables to add an extra connection check between trees
    * adds == and != operators
  * hrplib/hrpPlanner/Algorithm.cpp: adds Algorithm::ignoreCollisionAtStart() and Algorithm::ignoreCollisionAtGoal() (`#110 <https://github.com/fkanehiro/openhrp3/issues/110>`_)
  * hrplib/hrpPlanner/PathPlanner.cpp : Fix bugs, uses attitude() instead of R ( `#109 <https://github.com/fkanehiro/openhrp3/issues/109>`_)

  * hrplib/hrpPlanner/PathPlanner.cpp: outputs debug messages to stderr not to stdout (`#108 <https://github.com/fkanehiro/openhrp3/issues/108>`_)
  * hrplib/hrpPlanner/Algorithm.cpp: makes error messages more informative (`#107 <https://github.com/fkanehiro/openhrp3/issues/107>`_)

* hrplib/hrpCollision
  * hrplib/hrpCollision/Opcode/OPC_Common.h: modifies CreateSSV() to prevent Zero Div.(`#106 <https://github.com/fkanehiro/openhrp3/issues/106>`_)
  * hrplib/hrpCollision/ColdetModel.cpp: 隣接する三角形の判断を修正 (`#75 <https://github.com/fkanehiro/openhrp3/issues/75>`_)

* hrplib/hrpUtil
  * hrplib/hrpUtil/TriangleMeshShaper.cpp: checks values to prevent NaN (`#105 <https://github.com/fkanehiro/openhrp3/issues/105>`_)

  * {hrplib/hrpModel/ModelNodeSet.h, server/ModelLoader/BodyInfo_impl.cpp} uses aligned allocator (`b6b03af8 <https://github.com/fkanehiro/openhrp3/commit/b6b03af8c9d122f891d94387a5cbb8c8f00f9ef6>`_)
  * hrplib/hrpModel: Add angular momentum jacobian (`#98 <https://github.com/fkanehiro/openhrp3/issues/98>`_)
    * [hrplib/hrpModel/Body.cpp,Body.h] Add calcTotalMomentumFromJacobian and calcAngularMomentumJacobian
    * [hrplib/hrpModel/Link.cpp,Link.h] Add subIw (inertia tensor)
  * hrplib/hrpUtil/{Eigen3d.cpp,testEigen3d.cpp}: add the correction of floating point error (`#85 <https://github.com/fkanehiro/openhrp3/issues/85>`_)
    * display input matrix
    * add the correction of floating point error
  * hrplib/hrpUtil/testEigen3d.cpp : add google test for Eigen3d.cpp (`#64 <https://github.com/fkanehiro/openhrp3/issues/64>`_)

* server/ModelLoader
  * server/ModelLoader/ColladaWriter.h: check that a base link and an effector links exist, Fix `#93 <https://github.com/fkanehiro/openhrp3/issues/93>`_ (`#94 <https://github.com/fkanehiro/openhrp3/issues/94>`_)
  * server/ModelLoader/exportCollada.cpp: fix help message for adding information of manipulator to collada file, Fix `#91 <https://github.com/fkanehiro/openhrp3/issues/91>`_  (`#92 <https://github.com/fkanehiro/openhrp3/issues/92>`_ )
  * server/ModelLoader/BodyInfo_impl.cpp: set default mass properties (`#90 <https://github.com/fkanehiro/openhrp3/issues/90>`_)
  * server/ModelLoader/projectGenerator.cpp: Add outport for root link actual pos and rot. (`#81 <https://github.com/fkanehiro/openhrp3/issues/81>`_)
  * [server/ModelLoader/projectGenerator.cpp, REAME.md] Add integration method (EULER, RUNGE_KUTTA...) argument and update readme (`#79 <https://github.com/fkanehiro/openhrp3/issues/79>`_ )
  * server/ModelLoader/projectGenerator.cpp: generating default outport:dq in project file by projectGenerator (`#74 <https://github.com/fkanehiro/openhrp3/issues/74>`_)
  * server/ModelLoader/ModelLoader_impl.cpp: fix ModelLoader to enable the compile without collada (`#73 <https://github.com/fkanehiro/openhrp3/issues/73>`_)
  * server/ModelLoader/ModelLoader_impl.cpp: support PROJECT_DIR in ModelLoader, Fix `#55 <https://github.com/fkanehiro/openhrp3/issues/55>`_ (`#68 <https://github.com/fkanehiro/openhrp3/issues/68>`_)
  * server/ModelLoader/ColladaWriter.h: fix for reducing CORBA communication on 32bit machine on models with many shapes (`#63 <https://github.com/fkanehiro/openhrp3/issues/63>`_)
  * server/ModelLoader/README.md: add README.md with options and an example for projectGenerator (`#62 <https://github.com/fkanehiro/openhrp3/issues/62>`_, `#60 <https://github.com/fkanehiro/openhrp3/issues/60>`_)

* sample
  * [sample/example/customizer/sample1_bush_customizer_param.conf, sample/model/sample1_bush.wrl] Add hand bush for sample1_bush.wrl. Currently do not fix indent to check diff. Update bush parameters. (`#82 <https://github.com/fkanehiro/openhrp3/issues/82>`_)
  * [sample/model/sample_special_joint_robot.wrl] Add sample robot to check special joints (`#80 <https://github.com/fkanehiro/openhrp3/issues/80>`_ )

  * Fix sample4legrobot conf robot name (`#78 <https://github.com/fkanehiro/openhrp3/issues/78>`_)
    * [sample/model/sample_4leg_robot*.wrl] Fix leg origin pos left/right
    * [sample/example/customizer/sample_4leg_robot_bush_customizer_param.conf] Fix sample4legrobot conf robot name
* Add 4leg robot (`#77 <https://github.com/fkanehiro/openhrp3/issues/77>`_ )
    * [sample/example/customizer/CMakeLists.txt] Install bush customizer file for sample_4leg_robot_bush
    * [sample/model/sample_4leg_robot*, sample/example/customizer/sample_4leg_robot_bush_customizer_param.conf] Add 4legged robot and bush setting
  * [sample/model/sample1_bush.wrl,sample1.wrl] Add vlimit for sample1 and sample1_bush (`#72 <https://github.com/fkanehiro/openhrp3/issues/72>`_)
  * Add bush customizer (`#71 <https://github.com/fkanehiro/openhrp3/issues/71>`_)
    * [sample/example/customizer/CMakeLists.txt] Install BUSH_CUSTOMIZER_CONFIG file
    * [sample/example/customizer/sample1*.conf] Add example config file for sample1_bush.wrl param
    * [sample/example/customizer/CMakeLists.txt,sample/example/customizer/bush_customizer.cpp] Add customizer for rubber bush.
    * [sample/model/sample1_bush.wrl] Add sample1 model with rubber bush.

* Contributors: Eisoku Kuroiwa, Fumio Kanehiro, Shizuko Hattori, Jun Inoue, Kei Okada, Mehdi Benallegue, Shin'ichiro Nakaoka, Shunichi Nozawa, Takasugi Noriaki, Yohei Kakiuchi, Yosuke Matsusaka

3.1.8 (2015-04-21)
------------------

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
