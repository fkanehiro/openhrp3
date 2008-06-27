#
# Prepare Scripting Environment Parameters
#
# This module can do with a little tidying up.
# ... perhaps with a Meta-class
#

import time, string

####################################################################
#
# Functions for Jython
#
def doNarrow4Jython(corbaClass, corbaObject):
    return corbaClass.narrow(corbaObject)


def getORBJython(argv):
    props = System.getProperties()
    s = System.getenv("NS_OPT")
    if(not s):
        s = System.getenv("NS_OPT")
        
    if(s == ''):
        args = argv
    else:
        args = string.split(s)
    #
    return CORBA.ORB.init(args, props)


def getPluginObject2NarrowJython(name):
    #print 'getPluginObject2NarrowJython'
    try:
        exec('import Simulator.'+name+'Helper as localImport')
        return localImport
    except ImportError:
        return None

#def importJavaPlugins():
#    pluginList = dir(Simulator)
#    for p in pluginList:
#        if p[-6:] == 'Helper':
#            print 'import Simulator.'+p+' as '+p[:-6]
#            #exec('import Simulator.'+p+' as '+p[:-6])
#
####################################################################


####################################################################
#
# Functions for Python+omniORB
#
def doNarrow4Python(corbaClass, corbaObject):
    #print 'corbaClass = ', corbaClass
    #print 'corbaObject = ', corbaObject
    try:
        tmp = corbaObject._narrow(corbaClass)
        #print 'narrowed result is', tmp
        return tmp
    except:
        print 'narrow exception:', corbaObject, 'to', corbaClass

    return None


def getORBPython(argv):
    return CORBA.ORB_init(argv, CORBA.ORB_ID)


def getPluginObject2NarrowPython(name):
    print name
    print 'getPluginObject2NarrowPython('+name+')'
    try:
        exec('from _GlobalIDL import '+name+' as localImport')
        return localImport
    except ImportError:
        return None
#
####################################################################

global doNarrow
doNarrow = None

try: # platform is Jython
    import java.lang.System as System
    print 'platform is probably Jython'
    import org.omg.CORBA as CORBA
    
    from org.omg.CosNaming import NameComponent
    from org.omg.CosNaming.NamingContextPackage import NotFound
    from jp.go.aist.hrp import simulator as IDLBase
    
    Platform = 'J'
    
    # Naming Context
    from org.omg.CosNaming import NamingContextHelper as NamingContext
    # Plugin Manager (fails here...)
    #from IDLBase import PluginManagerHelper as PluginManager
    #import IDLBase.PluginManagerHelper as PluginManager
    PluginManager = IDLBase.PluginManagerHelper
    # Command Receiver
    #from IDLBase import CommandReceiverHelper as CommandReceiver
    CommandReceiver = IDLBase.CommandReceiverHelper
    # Plugin
    #from IDLBase import PluginHelper as Plugin
    Plugin = IDLBase.PluginHelper
    # Logger Plugin
    #from IDLBase import LoggerPluginHelper as LoggerPlugin
    LoggerPlugin = IDLBase.LoggerPluginHelper
    # Sequence Player
    #from IDLBase import SequencePlayerHelper as SequencePlayer
    SequencePlayer = IDLBase.SequencePlayerHelper

    stateProvider = IDLBase.stateProviderHelper
    IoControPlugin = IDLBase.IoControlPluginHelper
    # set specialised function
    doNarrow = doNarrow4Jython
    getORB = getORBJython
    getPluginObject2Narrow = getPluginObject2NarrowJython

except ImportError: # platform is not Jython
    print 'platform is probably CPython'
    import omniORB.CORBA as CORBA
    import CosNaming
    import _GlobalIDL as IDLBase
    
    NameComponent = CosNaming.NameComponent
    NotFound = CosNaming.NamingContext.NotFound
    
    Platform = 'C'

    # Naming Context
    NamingContext = CosNaming.NamingContext
    # Plugin Manager
    #from IDLBase import PluginManager
    PluginManager = IDLBase.PluginManager
    # Command Receiver
    #from IDLBase import CommandReceiver
    CommandReceiver = IDLBase.CommandReceiver
    # Plugin
    #from IDLBase import Plugin
    Plugin = IDLBase.Plugin
    # Logger Plugin
    #from IDLBase import LoggerPlugin
    LoggerPlugin = IDLBase.LoggerPlugin
    # Sequence Player
    #from IDLBase import SequencePlayer
    SequencePlayer = IDLBase.SequencePlayer
    # set specialised functions
    stateProvider = IDLBase.stateProvider
    IoControPlugin = IDLBase.IoControlPlugin

    doNarrow = doNarrow4Python
    getORB = getORBPython
    getPluginObject2Narrow = getPluginObject2NarrowPython

#---------------------------------------------------#

def getCORBAObjects():
    return NotFound, NamingContext, NameComponent, \
           IDLBase, \
           PluginManager, \
           Plugin, \
           CommandReceiver, \
           stateProvider, \
	   IoControPlugin, \
           LoggerPlugin, \
           SequencePlayer
