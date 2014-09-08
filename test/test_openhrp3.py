#!/usr/bin/env python

PKG = 'openhrp3'
import roslib; roslib.load_manifest(PKG)  # This line is not needed with Catkin.

import os
import sys
import unittest

code = """
#include <hrpModel/Body.h>

int main (int argc, char** argv)
{
  hrp::BodyPtr body(new hrp::Body());
  return 0;
}
"""
from subprocess import call, check_output, Popen, PIPE, STDOUT

## A sample python unit test
class TestCompile(unittest.TestCase):
    PKG_CONFIG_PATH = ''

    def setUp(self):
        # if rosbuild environment
        openhrp3_path = check_output(['rospack','find','openhrp3']).rstrip()
        if os.path.exists(os.path.join(openhrp3_path, "bin")) :
            self.PKG_CONFIG_PATH='PKG_CONFIG_PATH=%s/lib/pkgconfig:$PKG_CONFIG_PATH'%(openhrp3_path)

    def pkg_config_variable(self, var):
        return check_output("%s pkg-config openhrp3.1 --variable=%s"%(self.PKG_CONFIG_PATH, var), shell=True).rstrip()

    def check_if_file_exists(self, var, fname):
        pkg_var = var
        pkg_dname = self.pkg_config_variable(pkg_var)
        pkg_path = os.path.join(pkg_dname, fname)
        pkg_ret = os.path.exists(pkg_path)
        self.assertTrue(pkg_ret, "pkg-config openhrp3.1 --variable=%s`/%s (%s) returns %r"%(pkg_var, fname, pkg_path, pkg_ret))

    def test_config_variables(self):
        # self.check_if_file_exists("prefix",             "") # not defined
        # self.check_if_file_exists("exec_prefix",        "") # not defined
        self.check_if_file_exists("idl_dir",            "OpenHRP/OpenHRPCommon.idl")

    def check_if_file_exists_from_rospack(self, fname):
        pkg_dname = check_output(['rospack','find','openhrp3']).rstrip()
        pkg_path = os.path.join(pkg_dname, fname)
        pkg_ret = os.path.exists(pkg_path)
        self.assertTrue(pkg_ret, "`rospack find openhrp3`(%s) returns %r"%(pkg_path, pkg_ret))

    def check_if_file_exites_from_prefix(self, fname):
        self.check_if_file_exists("prefix", fname)

    def test_files_for_hrpsys(self):
        # https://github.com/start-jsk/hrpsys/blob/master/catkin.cmake#L125
        # self.check_if_file_exists_from_rospack("share/OpenHRP-3.1/sample/project")
        self.check_if_file_exites_from_prefix("share/OpenHRP-3.1/sample/project")

        # https://code.google.com/p/hrpsys-base/source/browse/trunk/idl/CMakeLists.txt#118
        self.check_if_file_exists("idl_dir",            "OpenHRP/OpenHRPCommon.idl")
        # https://code.google.com/p/hrpsys-base/source/browse/trunk/sample/PA10/PA10.conf.in#1
        # self.check_if_file_exists_from_rospack("share/OpenHRP-3.1/sample/model/PA10/pa10.main.wrl")

    def test_files_for_hrpsys_ros_bridge(self):
        # https://github.com/start-jsk/rtmros_common/blob/master/hrpsys_ros_bridge/test/test-samplerobot.py#L63
        # self.check_if_file_exists_from_rospack("share/OpenHRP-3.1/sample/controller/SampleController/etc/Sample.pos")
        self.check_if_file_exists_from_prefix("share/OpenHRP-3.1/sample/controller/SampleController/etc/Sample.pos")

        # https://github.com/start-jsk/rtmros_common/blob/master/hrpsys_ros_bridge/catkin.cmake#L141
        self.check_if_file_exists("idl_dir",            "../sample/model/PA10/pa10.main.wrl")
        self.check_if_file_exists("idl_dir",            "../sample/model/sample1.wrl")
        self.check_if_file_exists_from_prefix("share/OpenHRP-3.1/sample/model/PA10/pa10.main.wrl")
        self.check_if_file_exists_from_prefix("share/OpenHRP-3.1/sample/model/sample1.wrl")
        # self.check_if_file_exists_from_rospack("share/OpenHRP-3.1/sample/model/PA10/pa10.main.wrl")
        # self.check_if_file_exists_from_rospack("share/OpenHRP-3.1/sample/model/sample1.wrl")

    ## test 1 == 1
    def test_compile_pkg_config(self):
        global PID
        cmd = "%s pkg-config openhrp3.1 --cflags --libs"%(self.PKG_CONFIG_PATH)
        print "`"+cmd+"` =",check_output(cmd, shell=True, stderr=STDOUT)
        ret = call("g++ -o openhrp3-sample-pkg-config /tmp/%d-openhrp3-sample.cpp `%s`"%(PID,cmd), shell=True)
        self.assertTrue(ret==0)

    def _test_compile_move_ankle(self):
        cmd1 = "pkg-config openhrp3.1 --cflags --libs"
        cmd2 = "pkg-config openhrp3.1 --variable=idl_dir"
        print "`"+cmd1+"` =",check_output(cmd1, shell=True, stderr=STDOUT)
        print "`"+cmd2+"` =",check_output(cmd2, shell=True, stderr=STDOUT)
        ret = call("g++ -o move_ankle `%s`/../sample/example/move_ankle/move_ankle.cpp `%s`"%(cmd2,cmd1), shell=True)
        self.assertTrue(ret==0)

    def test_idl_dir(self):
        cmd = "%s pkg-config openhrp3.1 --variable=idl_dir"%(self.PKG_CONFIG_PATH)
        fname = "OpenHRP/OpenHRPCommon.idl"
        # check if idl file exists
        print "`"+cmd+"`"+fname+" = "+os.path.join(check_output(cmd, shell=True).rstrip(), fname)
        self.assertTrue(os.path.exists(os.path.join(check_output(cmd, shell=True).rstrip(), fname)))

    def test_sample_pa10(self):
        cmd = "%s pkg-config openhrp3.1 --variable=idl_dir"%(self.PKG_CONFIG_PATH)
        fname = "../sample/model/PA10/pa10.main.wrl"
        # check if model file exists
        print "`"+cmd+"`"+fname+" = "+os.path.join(check_output(cmd, shell=True).rstrip(), fname)
        self.assertTrue(os.path.exists(os.path.join(check_output(cmd, shell=True).rstrip(), fname)))

        cmd = "%s pkg-config openhrp3.1 --variable=prefix"%(self.PKG_CONFIG_PATH)
        fname = "share/OpenHRP-3.1/sample/model/PA10/pa10.main.wrl"
        # check if model file exists
        print "`"+cmd+"`"+fname+" = "+os.path.join(check_output(cmd, shell=True).rstrip(), fname)
        self.assertTrue(os.path.exists(os.path.join(check_output(cmd, shell=True).rstrip(), fname)))

    def test_sample_samplerobot(self):
        cmd = "%s pkg-config openhrp3.1 --variable=idl_dir"%(self.PKG_CONFIG_PATH)
        fname = "../sample/model/sample1.wrl"
        # check if model file exists
        print "`"+cmd+"`"+fname+" = "+os.path.join(check_output(cmd, shell=True).rstrip(), fname)
        self.assertTrue(os.path.exists(os.path.join(check_output(cmd, shell=True).rstrip(), fname)), "cmd = %r, fname = %r"%(cmd, fname))
        #
        # check if walk data file exists
        fname = "../sample/controller/SampleController/etc/Sample.pos"
        print "`"+cmd+"`"+fname+" = "+os.path.join(check_output(cmd, shell=True).rstrip(), fname)
        self.assertTrue(os.path.exists(os.path.join(check_output(cmd, shell=True).rstrip(), fname)), "cmd = %r, fname = %r"%(cmd, fname))

        cmd = "%s pkg-config openhrp3.1 --variable=prefix"%(self.PKG_CONFIG_PATH)
        fname = "share/OpenHRP-3.1/sample/model/sample1.wrl"
        # check if model file exists
        print "`"+cmd+"`"+fname+" = "+os.path.join(check_output(cmd, shell=True).rstrip(), fname)
        self.assertTrue(os.path.exists(os.path.join(check_output(cmd, shell=True).rstrip(), fname)), "cmd = %r, fname = %r"%(cmd, fname))
        #
        # check if walk data file exists
        fname = "share/OpenHRP-3.1/sample/controller/SampleController/etc/Sample.pos"
        print "`"+cmd+"`"+fname+" = "+os.path.join(check_output(cmd, shell=True).rstrip(), fname)
        self.assertTrue(os.path.exists(os.path.join(check_output(cmd, shell=True).rstrip(), fname)), "cmd = %r, fname = %r"%(cmd, fname))

#unittest.main()
if __name__ == '__main__':
    import rostest
    global PID
    PID = os.getpid()
    f = open("/tmp/%d-openhrp3-sample.cpp"%(PID),'w')
    f.write(code)
    f.close()
    rostest.rosrun(PKG, 'test_openhrp3', TestCompile) 



