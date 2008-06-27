#/*
# * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# * All rights reserved. This program is made available under the terms of the
# * Eclipse Public License v1.0 which accompanies this distribution, and is
# * available at http://www.eclipse.org/legal/epl-v10.html
# * Contributors:
# * General Robotix Inc.
# * National Institute of Advanced Industrial Science and Technology (AIST) 
# */
import sys
import __builtin__

rollbackImporter = None

class RollbackImporter:
    def __init__(self):
	self.previousModules = []
	for k in sys.modules.keys():
	    self.previousModules.append(k)
	self.realImport = __builtin__.__import__
	__builtin__.__import__ = self._import
	self.newModules = {}

    def _import(self, *args):
	n = args[0]
	f = args[3]
	try:
	    g = args[1]
	except:
	    g = {}
	try:
	    l = args[2]
	except:
	    l = {}
	result = apply(self.realImport, (n, g, l, f))
	self.newModules[n] = 1
	return result
    
    def uninstall(self):
	for m in self.newModules.keys():
	    if m not in self.previousModules:
		del(sys.modules[m])
	__builtin__.__import__ = self.realImport

def refresh():
    global rollbackImporter
    if rollbackImporter:
        rollbackImporter.uninstall()
    rollbackImporter = RollbackImporter()

refresh()
