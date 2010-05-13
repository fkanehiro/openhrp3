import time
import com.generalrobotix.ui.item.GrxSimulationItem as GrxSimulationItem
import syncExec
import java.lang.Runnable as Runnable

class StartSim(Runnable):
	def run(self):
		sim.startSimulation(0)
		return None

class SetTime(Runnable):
	def run(self):
		sim.setDbl("totalTime", 5.0)
		return None

sim   = uimanager.getSelectedItem(GrxSimulationItem, None)
syncExec.exec(SetTime())

for i in range(3):
	syncExec.exec(StartSim())
	sim.waitStopSimulation()

