#/*
# * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# * All rights reserved. This program is made available under the terms of the
# * Eclipse Public License v1.0 which accompanies this distribution, and is
# * available at http://www.eclipse.org/legal/epl-v10.html
# * Contributors:
# * General Robotix Inc.
# * National Institute of Advanced Industrial Science and Technology (AIST) 
# */
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
