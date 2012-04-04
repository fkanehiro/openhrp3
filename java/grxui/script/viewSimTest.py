#/*
# * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# * All rights reserved. This program is made available under the terms of the
# * Eclipse Public License v1.0 which accompanies this distribution, and is
# * available at http://www.eclipse.org/legal/epl-v10.html
# * Contributors:
# * General Robotix Inc.
# * National Institute of Advanced Industrial Science and Technology (AIST) 
# */
import hrp

import jp.go.aist.hrp.simulator.ViewSimulatorHelper as ViewSimulatorHelper
import jp.go.aist.hrp.simulator.CameraSequenceHolder as CameraSequenceHolder
import jp.go.aist.hrp.simulator.ImageData as ImageData
obj  = hrp.findObject("ViewSimulator")
view = ViewSimulatorHelper.narrow(obj)
csh  = CameraSequenceHolder()
view.getCameraSequence(csh)

data = csh.value[0].getImageData()
print "camera0"
print len(data.longData)
print len(data.octetData)
print len(data.floatData)
for i in range(0, 240):
  print data.floatData[i*320]

data = csh.value[1].getImageData()
print "camera1"
print len(data.longData)
print len(data.octetData)
print len(data.floatData)
#for i in range(0, 240):
#  print data.floatData[i*320]

print "finished"


