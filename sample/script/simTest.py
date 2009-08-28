import time
import com.generalrobotix.ui.item.GrxWorldStateItem as GrxWorldStateItem
import syncExec
import java.lang.Runnable as Runnable

class StartSim(Runnable):
	def run(self):
		sim.startSimulation(0,None)
		return None

class SetTime(Runnable):
	def run(self):
		item.setDbl("totalTime", 5.0)
		return None

sim   = uimanager.getView("OpenHRP")
item  = uimanager.getSelectedItem(GrxWorldStateItem, None)
syncExec.exec(SetTime())

for i in range(3):
	syncExec.exec(StartSim())
	sim.waitStopSimulation()

