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

#
# utils
#
import math
def r2d(deg):
  return deg*180/math.pi

#
# get Ref. of OnlineViewer
#
o = hrp.findObject("OnlineViewer")
ov = hrp.OnlineViewerHelper.narrow(o)

#
# get user input of robot id
#
robotId = waitInputMessage("Input Robot Name")

#
# get posture of the robot
#
postureH = hrp.DblSequenceHolder()
ret = ov.getPosture(robotId, postureH)

#
# show the result
#
N = 8
msg = ""

if ret:
  msg = "Posture of "+ robotId+" in [deg]:\n\n"
  for i in range(N):
    j = i
    while j<len(postureH.value):
      msg = msg + repr(j).rjust(3)+":"
      msg = msg + repr(round(postureH.value[j],2)).rjust(5)+"   "
      j=j+N
    msg = msg+"\n"
else:
  msg = "There is no robot named "+robotId+"."

waitInput(msg)

