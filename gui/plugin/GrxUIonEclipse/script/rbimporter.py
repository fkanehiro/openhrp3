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
