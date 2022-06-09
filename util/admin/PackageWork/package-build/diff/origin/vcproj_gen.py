#!/usr/bin/env python
# -*- Python -*-
#
#  @file vcproj_gen.py
#  @brief rtc-template VC++ project/solution file generator class
#  @date $Date$
#  @author Noriaki Ando <n-ando@aist.go.jp>
# 
#  Copyright (C) 2008
#      Task-intelligence Research Group,
#      Intelligent Systems Research Institute,
#      National Institute of
#          Advanced Industrial Science and Technology (AIST), Japan
#      All rights reserved.
# 
#  $Id: vcproj_gen.py 775 2008-07-28 16:14:45Z n-ando $
# 

import os
import yat
import gen_base

copyprops = """
copy "%RTM_ROOT%\\etc\\rtm_config.vsprops" .
"""

userprops = """<?xml version="1.0" encoding="shift_jis"?>
<VisualStudioPropertySheet
	ProjectType="Visual C++"
	Version="8.00"
	Name="User property"
	>
	<UserMacro
		Name="user_lib"
		Value=""
	/>
	<UserMacro
		Name="user_libd"
		Value=""
	/>
</VisualStudioPropertySheet>
"""


class vcproj_gen(gen_base.gen_base):
	"""
	VC++ project-file generator
	"""
	_fname_space = 16
	def __init__(self, data, opts):
		self.vcvers = {
			"VC8": {
				"proj_ver": "8.00",
				"sln_ver": "9.00",
				"suffix": "_vc8",
				"sln_fname": "",
				"exeproj_fname": "",
				"dllproj_fname": ""
				},
			"VC9": {
				"proj_ver": "9.00",
				"sln_ver": "10.00",
				"suffix": "_vc9",
				"sln_fname": "",
				"exeproj_fname": "",
				"dllproj_fname": ""
				}
			}

		self.data = data
		base = self.data["basicInfo"]["name"]
		for key in self.vcvers.keys():
			v = self.vcvers[key]
			suffix = v["suffix"]
			v["sln_fname"]     = base + suffix + ".sln"
			v["exeproj_fname"] = base + "Comp" + suffix + ".vcproj"
			v["dllproj_fname"] = base + suffix + ".vcproj"
		self.CreateSourceList()

		self.tags = {}
		self.gen_tags(self.tags)

		return

	def CreateSourceList(self):
		name = self.data["basicInfo"]["name"]
		es = self.data["exesources"] = []
		eh = self.data["exeheaders"] = []
		ds = self.data["dllsources"] = []
		dh = self.data["dllheaders"] = []

		es += [name + ".cpp", name + "Comp.cpp"]
		eh += [name + ".h"]
		ds += [name + ".cpp"]
		dh += [name + ".h"]

		slist = [es, ds]
		hlist = [eh, dh]
		nlist = ["skel_basename", "impl_basename"]
		for sidl in self.data["service_idl"]:
			for l in slist:
				l += [(sidl[key] + ".cpp") for key in nlist]
			for l in hlist:
				l += [(sidl[key] + ".h") for key in nlist]
		nlist = ["stub_basename"]
		for sidl in self.data["consumer_idl"]:
			for l in slist:
				l += [(sidl[key] + ".cpp") for key in nlist if sidl.has_key(key)]
			for l in hlist:
				l += [(sidl[key] + ".h") for key in nlist if sidl.has_key(key)]
		

	def check_overwrite(self, fname, wmode="wb"):
		"""
		Check file exist or not.
		"""
		msg = " already exists. Overwrite? (y/n)"
		if (os.access(fname, os.F_OK)):
			ans = raw_input("\"" + fname + "\"" + msg)
			if (ans == "y" or ans == "Y"):
				return file(fname, "wb")
			else:
				return None
		else:
			return file(fname, "wb")
		return None, None

	def print_vcproject(self):
		"""
		Generate VC++ project-file
		"""
		import vcprojtool
		proj_name = self.data["basicInfo"]["name"]
		version   = self.data["basicInfo"]["version"]
		exefiles = {"source": self.data["exesources"],
			    "header": self.data["exeheaders"],
			    "resource": [],
			    "yaml": []}
		dllfiles = {"source": self.data["dllsources"],
			    "header": self.data["dllheaders"],
			    "resource": [],
			    "yaml": []}
		for key in self.vcvers.keys():
			v = self.vcvers[key]
			# Create vcproj for EXE
			y = vcprojtool.YamlConfig("RTCEXE", v["proj_ver"],
						  proj_name+"Comp", version,
						  exefiles).generate()
			t = vcprojtool.VCProject("RTCEXE", y).generate()
			fname = v["exeproj_fname"]
			fd = self.check_overwrite(fname)
			if fd != None:
				fd.write(t)
				print("  File \"" + fname + "\" was generated.")
				fd.close()
			# Create vcproj for DLL
			y = vcprojtool.YamlConfig("RTCDLL", v["proj_ver"],
						  proj_name, version,
						  dllfiles).generate()
			t = vcprojtool.VCProject("RTCDLL", y).generate()
			fname = v["dllproj_fname"]
			fd = self.check_overwrite(fname)
			if fd != None:
				fd.write(t)
				print("  File \"" + fname + "\" was generated.")
				fd.close()
		return

	def print_solution(self):
		"""
		Generate VC++ solution-file
		"""
		import slntool
		for key in self.vcvers.keys():
			v = self.vcvers[key]
			fname = v["sln_fname"]
			fd = self.check_overwrite(fname)
			if fd != None:
				flist = [v["exeproj_fname"], v["dllproj_fname"]]
				yamltxt = slntool.get_slnyaml(None,  flist)
				sln = slntool.gen_solution(key, yamltxt)
				fd.write(sln)
				print("  File \"" + fname + "\" was generated.")
				fd.close()
		return

	def print_copybat(self):
		fname = "copyprops.bat"
		fd = self.check_overwrite(fname)
		if fd != None:
			o = copyprops.replace("\r\n","\n").replace("\n", "\r\n")
			fd.write(o)
			print("  File \"" + fname + "\" was generated.")
			fd.close()

	def print_userprops(self):
		fname = "user_config.vsprops"
		fd = self.check_overwrite(fname)
		if fd != None:
			o = userprops.replace("\r\n","\n").replace("\n", "\r\n")
			fd.write(o)
			print("  File \"" + fname + "\" was generated.")
			fd.close()

	def print_all(self):
		self.print_vcproject()
		self.print_solution()
		self.print_copybat()
		self.print_userprops()

