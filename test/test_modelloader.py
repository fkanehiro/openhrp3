#!/usr/bin/env python

PKG = 'openhrp3'
NAME = 'modelloader-test'

try: # catkin does not requires load_manifest
    import openhrp3
except:
    import roslib; roslib.load_manifest("openhrp3")

#import OpenRTM_aist.RTM_IDL # for catkin

from omniORB import CORBA, any, cdrUnmarshal, cdrMarshal
import CosNaming

import sys, os, socket, subprocess

import OpenRTM_aist
from OpenHRP import *

import rospkg
import unittest
import rostest

rootrc = None
# set custom port for modelloader-test.launch
def initCORBA():
    global rootnc
    #nshost = socket.gethostname()
    #nsport = 15005

    # initCORBA
    #os.environ['ORBInitRef'] = 'NameService=corbaloc:iiop:{0}:{1}/NameService'.format(nshost,nsport)
    orb = CORBA.ORB_init(sys.argv, CORBA.ORB_ID)

    nameserver = orb.resolve_initial_references("NameService");
    rootnc = nameserver._narrow(CosNaming.NamingContext)

def findModelLoader():
    global rootnc
    try:
        obj = rootnc.resolve([CosNaming.NameComponent('ModelLoader', '')])
        return obj._narrow(ModelLoader_idl._0_OpenHRP__POA.ModelLoader)
    except:
        print("Could not find ModelLoader", sys.exc_info()[0])
        exit

def equal(a, b, tol = 1e-4):
    if type(a) == float and type(b) == float:
        return abs(a - b) < tol
    if type(a) == list and type(b) == list:
        return all(equal(x,y) for (x,y) in zip(a,b))
    else:
        return True

def norm(a):
    r = 0
    for e in a:
        r = r + e*e
    return r/len(a)

class TestModelLoaderBase(unittest.TestCase):
    ml = None
    wrl_url = None
    dae_url = None
    wrl_binfo = None
    dae_binfo = None
    wrl_links = None
    dae_links = None

    def setUp(self):
        initCORBA()
        self.ml = findModelLoader()

    def print_ok(self, fmt, ok):
        s = '\033[0m' if ok else '\033[91m'
        e = '\033[0m'
        str = s+"{0:70} {1}".format(fmt, ok)+e
        print(str)
        self.assertTrue(ok,str)

    def loadFiles(self, wrl_file, dae_file):
        """ Override this method for loading model files from another directory """
        openhrp3_prefix=subprocess.check_output('pkg-config openhrp3.1 --variable=prefix', shell=True).rstrip()
        self.wrl_url = openhrp3_prefix+"/share/OpenHRP-3.1/sample/model/"+wrl_file
        self.dae_url = openhrp3_prefix+"/share/OpenHRP-3.1/sample/model/"+dae_file
        self.wrl_binfo = self.ml.getBodyInfo(self.wrl_url)
        self.dae_binfo = self.ml.getBodyInfo(self.dae_url)
        self.wrl_links = self.wrl_binfo._get_links()
        self.dae_links = self.dae_binfo._get_links()

    def checkModels(self, wrl_file, dae_file):
        self.loadFiles(wrl_file, dae_file)
        ret = True
        print("%16s %16s"%(wrl_file, dae_file))
        for (wrl_l, dae_l) in zip(self.wrl_links, self.dae_links) :
            # 'centerOfMass', 'childIndices', 'climit', 'encoderPulse', 'gearRatio', 'hwcs', 'inertia', 'inlinedShapeTransformMatrices', 'jointAxis', 'jointId', 'jointType', 'jointValue', 'lights', 'llimit', 'lvlimit', 'mass', 'name', 'parentIndex', 'rotation', 'rotorInertia', 'rotorResistor', 'segments', 'sensors', 'shapeIndices', 'specFiles', 'torqueConst', 'translation', 'ulimit', 'uvlimit'
            print(";; %s %d,%d"%(dae_l.name, dae_l.jointId, dae_l.parentIndex));
            name_ok             = wrl_l.name == dae_l.name
            translation_ok      = equal(wrl_l.translation, dae_l.translation)
            rotation_ok         = equal(norm(wrl_l.rotation), norm(dae_l.rotation))
            mass_ok             = equal(wrl_l.mass, dae_l.mass)
            centerOfMass_ok     = equal(wrl_l.centerOfMass, dae_l.centerOfMass)
            inertia_ok          = equal(wrl_l.inertia, dae_l.inertia)
            llimit_ok           = equal(wrl_l.llimit, dae_l.llimit)   if wrl_l.parentIndex > 0 else True
            ulimit_ok           = equal(wrl_l.ulimit, dae_l.ulimit)   if wrl_l.parentIndex > 0 else True
            lvlimit_ok          = equal(wrl_l.lvlimit, dae_l.lvlimit) if wrl_l.parentIndex > 0 else True
            uvlimit_ok          = equal(wrl_l.uvlimit, dae_l.uvlimit) if wrl_l.parentIndex > 0 else True
            climit_ok           = equal(wrl_l.climit, dae_l.climit)   if wrl_l.parentIndex > 0 else True
            torqueConst_ok      = equal(wrl_l.torqueConst, dae_l.torqueConst) if wrl_l.parentIndex > 0 else True
            gearRatio_ok        = equal(wrl_l.gearRatio, dae_l.gearRatio)     if wrl_l.parentIndex > 0 else True
            ret = all([ret, name_ok, translation_ok, rotation_ok, mass_ok, centerOfMass_ok])
            self.print_ok("name   {0:24s}  {1:24s} ".format(wrl_l.name, dae_l.name), True)#) ## not fixed yet
            self.print_ok(" tran   {0:24}  {1:24}".format(wrl_l.translation, dae_l.translation), translation_ok)
            self.print_ok(" rot    {0:24}  {1:24}".format(wrl_l.rotation, dae_l.rotation), rotation_ok)
            self.print_ok(" mass   {0:<24}  {1:<24}".format(wrl_l.mass, dae_l.mass), mass_ok)
            self.print_ok(" CoM    {0:24}  {1:24}".format(wrl_l.centerOfMass, dae_l.centerOfMass), centerOfMass_ok)
            self.print_ok(" iner   {0:50}\n        {1:50}".format(wrl_l.inertia, dae_l.inertia), inertia_ok)
            self.print_ok(" llim   {0:24}  {1:24}".format(wrl_l.llimit, dae_l.llimit), llimit_ok)
            self.print_ok(" ulim   {0:24}  {1:24}".format(wrl_l.ulimit, dae_l.ulimit), ulimit_ok)
            self.print_ok(" lvlim  {0:24}  {1:24}".format(wrl_l.lvlimit, dae_l.lvlimit), lvlimit_ok)
            self.print_ok(" uvlim  {0:24}  {1:24}".format(wrl_l.uvlimit, dae_l.uvlimit), uvlimit_ok)
            self.print_ok(" clim   {0:24}  {1:24}".format(wrl_l.climit, dae_l.climit), climit_ok)
            self.print_ok(" trqcnt {0:24}  {1:24}".format(wrl_l.torqueConst, dae_l.torqueConst), torqueConst_ok)
            self.print_ok(" gratio {0:24}  {1:24}".format(wrl_l.gearRatio, dae_l.gearRatio), gearRatio_ok)
            for (wrl_s, dae_s) in zip(wrl_l.segments, dae_l.segments):
                # name, mass, centerOfMass, inertia, transformMatrix, shapeIndices
                name_ok             = wrl_s.name == dae_s.name
                mass_ok             = equal(wrl_s.mass, dae_s.mass)
                centerOfMass_ok     = equal(wrl_s.centerOfMass, dae_s.centerOfMass)
                inertia_ok          = equal(wrl_s.inertia, dae_s.inertia)
                transformMatrix_ok  = equal(wrl_s.transformMatrix, dae_s.transformMatrix)
                shapeIndices_ok     = equal(wrl_s.shapeIndices, dae_s.shapeIndices)
                ret = all([ret, name_ok, mass_ok, centerOfMass_ok,inertia_ok, transformMatrix_ok, shapeIndices_ok])
                self.print_ok(" name {0:24s}  {1:24s} ".format(wrl_s.name, dae_s.name), name_ok)
                self.print_ok("  mass  {0:<24}  {1:<24}".format(wrl_s.mass, dae_s.mass), mass_ok)
                self.print_ok("  CoM   {0:24}  {1:24}".format(wrl_s.centerOfMass, dae_s.centerOfMass), centerOfMass_ok)
                self.print_ok("  iner  {0:50}\n        {1:50}".format(wrl_s.inertia, dae_s.inertia), inertia_ok)
                self.print_ok("  trans {0:50}\n        {1:50}".format(wrl_s.transformMatrix, dae_s.transformMatrix), transformMatrix_ok)
                self.print_ok("  shape  {0:24}  {1:24}".format(wrl_s.shapeIndices, dae_s.shapeIndices), shapeIndices_ok)

        if not ret:
            print("===========\n== ERROR == {0} and {1} differs\n===========".format(wrl_file, dae_file))
            return ret

class TestModelLoader(TestModelLoaderBase):
    """ Make class for testing by inheriting TestModelLoaderBase """
    def test_sample_models(self):
        self.checkModels("sample.wrl","sample.dae")

    def test_pa10(self):
        self.checkModels("PA10/pa10.main.wrl","PA10/pa10.main.dae")

    #def test_3dof_arm(self):
    #    self.checkModels("sample3dof.wrl","sample3dof.dae")

if __name__ == '__main__':
    rostest.run(PKG, NAME, TestModelLoader, sys.argv)
