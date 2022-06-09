#!/usr/bin/env python
# -*- Python -*-

import sys
import time
sys.path.append(".")

# Import RTM module
import OpenRTM_aist
import RTC

# for convert()
import math

# This module's spesification
# <rtc-template block="module_spec">
tkjoystick_spec = ["implementation_id", "TkJoyStick", 
		   "type_name",         "TkJoyStick", 
		   "description",       "Sample component for MobileRobotCanvas component", 
		   "version",           "1.0", 
		   "vendor",            "Noriaki Ando and Shinji Kurihara", 
		   "category",          "example", 
		   "activity_type",     "DataFlowComponent", 
		   "max_instance",      "10", 
		   "language",          "Python", 
		   "lang_type",         "SCRIPT",
		   ""]
# </rtc-template>

import tkjoystick

class Position:
	def __init__(self, x = 0.0, y = 0.0, r = 0.0, th = 0.0):
		self.x = x
		self.y = y
		self.r = r
		self.th = th

position = Position()
#tkJoyCanvas = tkjoystick.TkJoystick()


class TkJoyStick(OpenRTM_aist.DataFlowComponentBase):
	def __init__(self, manager):
		OpenRTM_aist.DataFlowComponentBase.__init__(self, manager)

		self._d_pos = RTC.TimedFloatSeq(RTC.Time(0,0),[])
		self._posOut = OpenRTM_aist.OutPort("pos", self._d_pos)
		self._d_vel = RTC.TimedFloatSeq(RTC.Time(0,0),[])
		self._velOut = OpenRTM_aist.OutPort("vel", self._d_vel)
		

		# Set OutPort buffers
		self.registerOutPort("pos",self._posOut)
		self.registerOutPort("vel",self._velOut)

		self._k = 1.0
		self.x = 0.0
		self.y = 0.0

		 
	def onInitialize(self):
		# Bind variables and configuration variable
		
		return RTC.RTC_OK


	def onShutdown(self, ec_id):
		return RTC.RTC_OK


	def onExecute(self, ec_id):
		self._d_pos.data = [self.x, self.y]
		self._d_vel.data = self.convert(self.x, self.y)
		self._posOut.write()
		self._velOut.write()
		
		return RTC.RTC_OK

	"""
	 \brief CanvasのデータをMobileRobotCanvas用のデータに変換する。
	"""
	def convert(self, x, y):
		_th = math.atan2(y,x)
		_v = self._k * math.hypot(x, y)
		_vl = _v * math.cos(_th - (math.pi/4.0))
		_vr = _v * math.sin(_th - (math.pi/4.0))
		print(x, y, _vl, _vr)
		return [_vl, _vr]

	def set_pos(self, pos, pol):
		self.x = pos[0]
		self.y = pos[1]
		self.r = pol[0]
		self.th = pol[1]



#def MyModuleInit(manager):
#    profile = OpenRTM_aist.Properties(defaults_str=tkjoystick_spec)
#    manager.registerFactory(profile,
#                            TkJoyStick,
#                            OpenRTM_aist.Delete)
#
#    # Create a component
#    comp = manager.createComponent("TkJoyStick")



def main():
	tkJoyCanvas = tkjoystick.TkJoystick()
	tkJoyCanvas.master.title("TkJoystick")
	mgr = OpenRTM_aist.Manager.init(sys.argv)
	mgr.activateManager()

	# Register component
	profile = OpenRTM_aist.Properties(defaults_str=tkjoystick_spec)
	mgr.registerFactory(profile,
			    TkJoyStick,
			    OpenRTM_aist.Delete)
	# Create a component
	comp = mgr.createComponent("TkJoyStick")

	tkJoyCanvas.set_on_update(comp.set_pos)
	mgr.runManager(True)
	tkJoyCanvas.mainloop()

if __name__ == "__main__":
	main()

