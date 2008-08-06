#
# Inherit and use this for individual robots.
#
import sys

import hrp

class HRPCommon:

    def __init__(self, h, p='2809'):

        self.MotionSys = None

        self.Hostname = h
        self.PortNumber = p

        self.Initialise = 0


    def initialisePlugins(self):
        count = 0
        for pn,vn,pc in plist:
            if self.Initialise and 1<<count:
                print 'initialise ', pn, ' as ', vn

    #
    # pCORBA: plugin corba name
    # pFile:  plugin shared object file name(without suffix)
    # pClass: plugin class name
    #
    def loadAndCreatePlugin(self, pCORBA, pFile, pClass):
        p = hrp.findPlugin(pCORBA)

        if p == None:
            print 'load and create'
            self.MotionSys.load(pFile)
            p = self.MotionSys.create(pFile, pCORBA, '')

        if p != None:
            if pClass != None:
                narrowedP = hrp.doNarrow(pClass, p)
                
            else:
                narrowedP = hrp.doNarrow(Plugin, p)
	    #
            return narrowedP

        else:
            print 'plugin '+pCORBA+' does not exist'
            return None
