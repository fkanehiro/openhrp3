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


