#!/usr/bin/env bash

set -x
set -e

export TZ=Asia/Tokyo; echo "${TZ}" > /etc/timezone # configure tzdata
apt-get update -qq
echo 'debconf debconf/frontend select Noninteractive' | debconf-set-selections
apt-get install -y -qq git wget gnupg lsb-release build-essential
# Define some config vars
export CI_SOURCE_PATH=$(pwd)
export REPOSITORY_NAME=${PWD##*/}
echo "Testing branch $TRAVIS_BRANCH of $REPOSITORY_NAME"
if [[ "$ROS_DISTRO" ==  "hydro" || "$ROS_DISTRO" ==  "jade" || "$ROS_DISTRO" ==  "lunar" ]]; then
    sh -c 'echo "deb http://snapshots.ros.org/$ROS_DISTRO/final/ubuntu $(lsb_release -sc) main" >> /etc/apt/sources.list.d/ros-latest.list'
    apt-key adv --keyserver keyserver.ubuntu.com --recv-key 0xCBF125EA
else
   sh -c 'echo "deb http://packages.ros.org/ros-testing/ubuntu $(lsb_release -sc) main" > /etc/apt/sources.list.d/ros-latest.list'
   wget http://packages.ros.org/ros.key -O - | apt-key add -
fi
apt-get update -qq
apt-get install dpkg -y # for https://github.com/ros/rosdistro/issues/19481
# install: # Use this to install any prerequisites or dependencies necessary to run your build
apt-get install -qq -y f2c libopencv-dev libf2c2 libf2c2-dev doxygen cmake libeigen3-dev libjpeg-dev git jython libatlas-base-dev libboost-all-dev libpng-dev
apt-get install -qq -y collada-dom-dev || apt-get install -qq -y libcollada-dom2.4-dp-dev # libcollada-dom2.4 for melodic
apt-get install -qq -y ros-$ROS_DISTRO-openrtm-aist ros-$ROS_DISTRO-mk ros-$ROS_DISTRO-rosbuild ros-$ROS_DISTRO-rostest ros-$ROS_DISTRO-roslang python-rosdep
apt-get install -qq -y ros-$ROS_DISTRO-openrtm-aist-python || echo "try without openrtm-aist-python"
cd $CI_SOURCE_PATH
# before_script: # Use this to prepare your build for testing e.g. copy database configurations, environment variables, etc.
source /opt/ros/$ROS_DISTRO/setup.bash
# script: # All commands must exit with code 0 on success. Anything else is considered failure.
export ROS_PARALLEL_JOBS="-j2 -l2"
mkdir -p ~/ws/src
ln -sf ${CI_SOURCE_PATH} ~/ws/src/${REPOSITORY_NAME}
git clone http://github.com/fkanehiro/hrpsys-base ~/ws/src/hrpsys
patch -d ~/ws/src/hrpsys -p1 < ${CI_SOURCE_PATH}/.github/workflows/trusty-hrpsys-util.patch
sed -i "s@if(ENABLE_DOXYGEN)@if(0)@" ~/ws/src/hrpsys/CMakeLists.txt # disable doc generation
cd ~/ws
rosdep init
rosdep update --include-eol-distros
rosdep install -r -q -n --from-paths src --ignore-src --rosdistro $ROS_DISTRO -y || echo "use libpng-dev in package.xml"
catkin_make_isolated
source devel_isolated/setup.bash
export ROS_PACKAGE_PATH=`pwd`/devel_isolated:$ROS_PACKAGE_PATH
export EXIT_STATUS=0; [ "`find devel_isolated/openhrp3/share/openhrp3 -iname '*.test'`" == "" ] && echo "[openhrp3] No tests ware found!!!"  || find devel_isolated/openhrp3/share/openhrp3 -iname "*.test" -print0 | xargs -0 -n1 rostest || export EXIT_STATUS=$?; [ $EXIT_STATUS == 0 ]
/etc/init.d/omniorb4-nameserver stop || echo "stop omniserver just in case..."
export EXIT_STATUS=0; [ "`find devel_isolated/hrpsys/share/hrpsys -iname '*.test'`" == "" ] && echo "[hrpsys] No tests ware found!!!"  || find devel_isolated/hrpsys/share/hrpsys -iname "*.test" -print0 | xargs -0 -n1 rostest || export EXIT_STATUS=$?; [ $EXIT_STATUS == 0 ]

