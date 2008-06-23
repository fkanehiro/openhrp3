import time
import com.generalrobotix.ui.item.GrxWorldStateItem as GrxWorldStateItem

sim   = uimanager.getView("OpenHRP")
item  = uimanager.getSelectedItem(GrxWorldStateItem, None)
item.setDbl("totalTime", 5.0)

for i in range(3):
  sim.startSimulation(0)
  
  sim.waitStopSimulation()

  #time.sleep(5)
  #sim.stopSimulation()
