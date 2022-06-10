from org.omg.CORBA import *
from org.omg.CosNaming import *
from org.omg.CosNaming.NamingContextPackage import *

from java.lang import System
from jp.go.aist.hrp.simulator import *

import string, math

rootnc = None

def unbindObject(objname):
	nc = NameComponent(objname, "")
	path = [nc]
	rootnc.unbind(path)
	return None

def initCORBA():
	global rootnc, orb
	props = System.getProperties()
	
	args = string.split(System.getProperty("NS_OPT"))
	orb = ORB.init(args, props)

	nameserver = orb.resolve_initial_references("NameService");
	rootnc = NamingContextHelper.narrow(nameserver);
	return None

def getRootNamingContext(corbaloc):
	props = System.getProperties()
	
	args = ["-ORBInitRef", corbaloc]
	orb = ORB.init(args, props)

	nameserver = orb.resolve_initial_references("NameService");
	return NamingContextHelper.narrow(nameserver);

def findObject(objname, rnc=None):
	nc = NameComponent(objname,"")
	path = [nc]
	if not rnc:
		rnc = rootnc
	return rnc.resolve(path)

def findReceiver(objname, rnc=None):
	try:
		obj = findObject(objname, rnc)
		return CommandReceiverHelper.narrow(obj)
	except:
		print("exception in findReceiver("+objname+")")

def findPlugin(name, rnc=None):
	try:
		obj = findObject(name, rnc)
		return PluginHelper.narrow(obj)
	except:
		return None

def findPluginManager(name, rnc=None):
	try:
		obj = findObject(name, rnc)
		return PluginManagerHelper.narrow(obj)
	except:
		print("exception in findPluginManager")

def findWalkPlugin(name, rnc=None):
	try:
		obj = findObject(name, rnc)
		return walkpluginHelper.narrow(obj)
	except:
		print("exception in findWalkPlugin")

def findSeqPlugin(name, rnc=None):
	try:
		obj = findObject(name, rnc)
		return SequencePlayerHelper.narrow(obj)
	except:
		print("exception in findSeqPlugin")

def findLogPlugin(name, rnc=None):
	try:
		return LoggerPluginHelper.narrow(findObject(name, rnc))
	except:
		print("exception in findLogPlugin")

def findDynamicsPlugin(name, rnc=None):
	try:
		return dynamicsPluginHelper.narrow(findObject(name, rnc))
	except:
		return None

def findStateProvider(name, rnc=None):
	try:
		return stateProviderHelper.narrow(findObject(name, rnc))
	except:
		return None
	
def findStereoVision(rnc=None):
	try:
		return StereoVisionHelper.narrow(findObject("StereoVision", rnc))
	except:
		return None

def findStabilizerPlugin(name, rnc=None):
	try:
		return findPlugin(name, rnc)
	except:
		print("exception in findStabilizerPlugin")
		return None

def findIoControlPlugin(name, rnc=None):
	try:
		return IoControlPluginHelper.narrow(findObject(name, rnc))
	except:
		return None

def findModelLoader(rnc=None):
	try:
		return ModelLoaderHelper.narrow(findObject("ModelLoader", rnc))
	except:
		return None
	
def findPositionSensor(rnc=None):
        try:
		return positionSensorHelper.narrow(findObject("positionSensor", rnc))
        except:
		return None

def findSpeakServer(rnc=None):
	try:
		return SpeakServerHelper.narrow(findObject("speakFactory", rnc))
	except:
		return None

def findOnlineViewer(rnc=None):
	try:
		return OnlineViewerHelper.narrow(findObject("OnlineViewer", rnc))
	except:
		return None

def findDynamicsSimulatorFactory(rnc=None):
	try:
		return DynamicsSimulatorFactoryHelper.narrow(findObject("DynamicsSimulatorFactory", rnc))
	except:
		return None

def findControllerFactory(name, rnc=None):
	try:
		return ControllerFactoryHelper.narrow(findObject(name, rnc))
	except:
		return None

def findCollisionDetectorFactory(rnc=None):
	try:
		return CollisionDetectorFactoryHelper.narrow(findObject("CollisionDetectorFactory", rnc))
	except:
		return None
						       

def degrees(x):
	return x*180/math.pi


def radians(x):
	return x*math.pi/180

def loadVRML(url):
	ml = findModelLoader()
	if ml == None:
		return None
	model = ml.loadURL(url)
	co = model.getCharObject()
	model.destroy()
	return co

initCORBA()
