#!/usr/bin/env python
# -*- python -*-
#
#  @file rtc-template
#  @brief rtc-template RTComponent source code generator tool
#  @date $Date: 2007-10-09 07:19:15 $
#  @author Noriaki Ando <n-ando@aist.go.jp>
# 
#  Copyright (C) 2004-2008
#      Task-intelligence Research Group,
#      Intelligent Systems Research Institute,
#      National Institute of
#          Advanced Industrial Science and Technology (AIST), Japan
#      All rights reserved.
# 
#  $Id: rtc-template 1815 2010-01-27 20:26:57Z n-ando $
#

import getopt, sys
import time
import re
import os
import yaml
import copy

default_profile="""
rtcProfile: 
  version: "1.0"
  id: # RTC:SampleVendor.SampleCategory.SampleComponent:1.0.0
  basicInfo:
    name: ""
    description: ""
    version: 1.0.0
    vendor: SampleVendor
    category: ""
    componentType: STATIC
    activityType: PERIODIC
    componentKind: DataFlowComponent
    maxInstances: 1
    abstract: ""
    executionRate: 1000.0
    executionType: PeriodicExecutionContext
    creationDate:
      day: ""
      hour: ""
      minute: ""
      month: ""
      second: ""
      year: ""
    updateDate:
      day: 17
      hour: 14
      minute: 0
      month: 4
      second: 0
      year: 2008
    "rtcDoc::doc":
      algorithm: ""
      creator: ""
      description: ""
      inout: ""
      license: ""
      reference: ""
    "rtcExt::versionUpLog": 
      - "2008/04/18 14:00:00:Ver1.0"
      - "2008/04/18 17:00:00:Ver1.1"
  language: 
    java: 
      library: 
        - library1
  actions: 
    onAborting:
      "rtcDoc::doc":
        description: on_aborting description
        postCondition: on_aborting Post_condition
        preCondition: on_aborting Pre_condition
      implemented: true
    onActivated:
      "rtcDoc::doc":
        description: on_activated description
        postCondition: on_activated Post_condition
        preCondition: on_activated Pre_condition
      implemented: true
    onDeactivated:
      "rtcDoc::doc":
        description: on_deactivated description
        postCondition: on_deactivated Post_condition
        preCondition: on_deactivated Pre_condition
    onError:
      "rtcDoc::doc":
        description: on_error description
        postCondition: on_error Post_condition
        preCondition: on_error Pre_condition
    onExecute:
      "rtcDoc::doc":
        description: on_execute description
        postCondition: on_execute Post_condition
        preCondition: on_execute Pre_condition
    onFinalize:
      "rtcDoc::doc":
        description: on_finalize description
        postCondition: on_finalize Post_condition
        preCondition: on_finalize Pre_condition
    onInitialize:
      "rtcDoc::doc":
        description: on_initialize description
        postCondition: on_initialize Post_condition
        preCondition: on_initialize Pre_condition
      implemented: true
    onRateChanged:
      "rtcDoc::doc":
        description: on_rate_changed description
        postCondition: on_rate_changed Post_condition
        preCondition: on_rate_changed Pre_condition
    onReset:
      "rtcDoc::doc":
        description: on_reset description
        postCondition: on_reset Post_condition
        preCondition: on_reset Pre_condition
    onShutdown:
      "rtcDoc::doc":
        description: on_shutdown description
        postCondition: on_shutdown Post_condition
        preCondition: on_shutdown Pre_condition
      implemented: true
    onStartup:
      "rtcDoc::doc":
        description: on_startup description
        postCondition: on_startup Post_condition
        preCondition: on_startup Pre_condition
    onStateUpdate:
      "rtcDoc::doc":
        description: on_state_update description
        postCondition: on_state_update Post_condition
        preCondition: on_state_update Pre_condition
  dataPorts: 
    -
      name: inport1
      portType: DataInPort
      type: "RTC::TimedLong"
      interfaceType: CorbaPort
      dataflowType: "Push,Pull"
      subscriprionType: "Periodic,New,Flush"
      idlFile: DataPort1.idl
      "rtcDoc::doc":
        type: In1Type
        description: In1Description
        number: In1Number
        occerrence: In1Occerrence
        operation: In1Operation
        semantics: In1Semantics
        unit: In1Unit
      "rtcExt::position": LEFT
      "rtcExt::varname": In1Var
  servicePorts: 
    -
      name: SrvPort1
      "rtcDoc::doc":
        description: ServicePort1 description
        ifdescription: ServicePort1 I/F description
      "rtcExt::position": LEFT
      serviceInterface: 
        -
          direction: Provided
          "rtcDoc::doc":
            description: if1 Description
            docArgument: if1 Argument
            docException: if1 Exception
            docPostCondition: if1 PostCond
            docPreCondition: if1 PreCond
            docReturn: if1 Return
          idlFile: IF1Idlfile.idl
          instanceName: IF1Instance
          name: S1IF1
          path: IF1SearchPath
          type: IF1Type
          varname: IF1VarName
  configurationSet: 
    configuration: 
      - 
        name: config1
        type: int
        varname: var1
        defaultValue: ""
        "rtcDoc::doc":
          constraint: config_constraint1
          dataname: dataname1
          defaultValue: default1
          description: config_Desc1
          range: config_range1
          unit: config_unit1
  parameters: 
    -
      defaultValue: param_def1
      name: param1
    -
      defaultValue: param_def2
      name: param2

"""


class Struct:
	def __init__(self):
		return

libdir_path = os.popen("rtm-config --libdir", "r").read().split("\n")
if libdir_path[0] != '':
	pyhelper_path = libdir_path[0] + "/py_helper"
	sys.path.append(pyhelper_path)
else:
	pyhelper_path = os.environ['OPENHRP_SDK_ROOT'] + "/utils/rtc-template"
	sys.path.append(pyhelper_path)

# Option format
opt_args_fmt = ["help",
		"module-name=",
		"module-type=",
		"module-desc=",
		"module-version=",
		"module-vendor=",
		"module-category=",
		"module-comp-type=",
		"module-act-type=",
		"module-max-inst=",
		"module-lang=",
                "config=",
		"inport=",
		"outport=",
		"service=",
		"service-idl=",
		"consumer=",
		"consumer-idl=",
		"idl-include=",
		"backend="]


def usage_short():
	"""
	Help message
	"""
	print("""
Usage: rtc-template [OPTIONS]

Options:

    [-h]                                  Print short help.
    [--help]                              Print details help.
    [--backend[=backend] or -b]           Specify template code generator.
    [--module-name[=name]]                Your module name.
    [--module-desc[=description]]         Module description.
    [--module-version[=version]]          Module version.
    [--module-vendor[=vendor]]            Module vendor.
    [--module-category[=category]]        Module category.
    [--module-comp-type[=component_type]] Component type.
    [--module-act-type[=activity_type]]   Component's activity type.
    [--module-max-inst[=max_instance]]    Number of maximum instance.
    [--module-lang[=language]]            Language.
    [--config[=ParamName:Type:Default]]   Configuration variable.
    [--inport[=PortName:Type]]            InPort's name and type.
    [--outport[=PortName:Type]]           OutPort's name and type
    [--service[=PortName:Name:Type]]      Service Provider Port
    [--service-idl[=IDL_file]]            IDL file name for service
    [--consumer[=PortName:Name:Type]]     Service Consumer Port
    [--consumer-idl[=IDL_file]]           IDL file name for consumer
    [--idl-include=[path]]                Search path for IDL compile

""")
def usage_long():
	"""
	Help message
	"""
	print("""
    --output[=output_file]:
        Specify base name of output file. If 'XXX' is specified,
        C++ source codes XXX.cpp, XXX.h, XXXComp.cpp Makefile.XXX is generated.

    --module-name[=name]:
        Your component's base name. This string is used as module's
        name and component's base name. A generated new component
        class name is also names as this RTC_MODULE_NAME.
        Only alphabetical and numerical characters are acceptable.

    --module-desc[=description]:
        Short description. If space characters are included, string should be
        quoted.

    --module-version[=version]:
        Your module version. ex. 1.0.0

    --module-vendor[=vendor]:
        Vendor's name of this component.

    --module-category[=category]:
        This component module's category. ex. Manipulator MobileRobot, etc...

    --module-comp-type[=component_type]:
        Specify component type.
	    'STATIC', 'UNIQUE', 'COMMUTATIVE' are acceptable.

    --module-act-type[=activity_type]:
        Specify component activity's type.
        'PERIODIC', 'SPORADIC', 'EVENT_DRIVEN' ace acceptable.

    --module-max-inst[=max_instance]:
        Specify maximum number of component instance.

    --config=[ParamName:Type:Default]:
        Specify configuration value. The 'ParamName' is used as the
        configuration value identifier. This character string is also used as
        variable name in the source code. The 'Type' is type of configuration
        value. The type that can be converted to character string is allowed.
        In C++ language, the type should have operators '<<' and '>>' that
        are defined as
        'istream& operator<<(Type)'
        and
        'ostream& operator>>(Type)'.

    --inport=[PortName:Type]:
        Specify InPort's name and type. 'PortName' is used as this InPort's
        name. This string is also used as variable name in soruce code.
        'Type' is InPort's variable type. The acceptable types are,
        Timed[ Short | Long | UShort | ULong | Float | Double | Char | Boolean
        | Octet | String ] and its sequence types.

    --outport=[PortName:Type]:
        Specify OutPort's name and type. 'PortName' is used as this OutPort's
        name. This string is also used as variable name in soruce code.
        'Type' is OutPort's variable type. The acceptable types are,
        Timed[ Short | Long | UShort | ULong | Float | Double | Char | Boolean
        | Octet | String ] and its sequence types.
		
    --service=[PortName:Name:Type]:
        Specify service name, type and port name.
        PortName: The name of Port to which the interface belongs.
              This name is used as CorbaPort's name.
        Name: The name of the service interface. This name is used as 
              the name of the interface, instance name and variable name.
        Type: The type of the serivce interface.
              This name is used as type name of the service.

    --service-idl=[IDL filename]:
        Specify IDL file of service interface.
        For simplicity, please define one interface in one IDL file, although
        this IDL file can include two or more interface definition,
		
    --consumer=[PortName:Name:Type]:
        Specify consumer name, type and port name.
        PortName: The name of Port to which the consumer belongs.
              This name is used as CorbaPort's name.
        Name: The name of the consumer. This name is used as 
              the name of the consumer, instance name and variable name.
        Type: The serivce interface type that is required by the consumer.
              This name is used as type name of the consumer.

    --consumer-idl=[IDL filename]:
        Specify IDL file of service consumer.
        For simplicity, please define one interface in one IDL file, although
        this IDL file can include two or more interface definition,
	

Example:
    rtc-template -bcxx \\
    --module-name=Sample --module-desc='Sample component' \\
    --module-version=0.1 --module-vendor=AIST --module-category=Generic \\
    --module-comp-type=DataFlowComponent --module-act-type=SPORADIC \\
    --module-max-inst=10  \\
    --config=int_param0:int:0 --config=int_param1:int:1 \\
    --config=double_param0:double:3.14 --config=double_param1:double:9.99 \\
    --config="str_param0:std::string:hoge" \\
    --config="str_param1:std::string:foo" \\
    --inport=Ref:TimedFloat --inport=Sens:TimedFloat \\
    --outport=Ctrl:TimedDouble --outport=Monitor:TimedShort \\
    --service=MySvcPort:myservice0:MyService \\
    --consumer=YourSvcPort:yourservice0:YourService \\
    --service-idl=MyService.idl --consumer-idl=YourService.idl

""")
	return

def usage():
	usage_short()
	usage_long()
	return
		

def CreateBasicInfo(opts):
	"""
	MakeModuleProfile

	Create ModuleProfile list from command options
	"""
	mapping = {
		'name': 'name',
		'desc': 'description',
		'version': 'version',
		'vendor': 'vendor',
		'category': 'category',
		'comp-type': 'componentType',
		'act-type': 'activityType',
		'type': 'componentKind',
		'max-inst': 'maxInstances'
		}
	# default "basicInfo"
	prof = {
		"name": "",
		"description": "",
		"version": "1.0.0",
		"vendor": "",
		"category": "",
		"componentType": "STATIC",
		"activityType": "PERIODIC",
		"componentKind": "DataFlowComponent",
		"maxInstances": "1",
		"abstract": "",
		"executionRate": "1000.0",
		"executionType": "PeriodicExecutionContext",
		"creationDate":
			{
			"day": "",
			"hour": "",
			"minute": "",
			"month": "",
			"second": "",
			"year": ""
			},
		"updateDate":
			{
			"year": "",
			"month": "",
			"day": "",
			"hour": "",
			"minute": "",
			"second": "",
			},
		"rtcDoc::doc":
			{
			"algorithm": "",
			"creator": "",
			"description": "",
			"inout": "",
			"license": "",
			"reference": ""
			},
		"rtcExt::versionUpLog": []
		}

	# obtain --module-xxx options' values
	for opt, arg in opts:
		if opt.find("--module-") == 0:
			var = opt.replace("--module-","")
			if prof.has_key(mapping[var]):
				prof[mapping[var]] = arg
	# set creationDate
	cDate = time.localtime()
	i = 0
	cDateKey = ['year', 'month', 'day', 'hour', 'minute', 'second']
	for key in cDateKey:
		prof["creationDate"][key] = cDate[i]
		i += 1
	# check empty values
	empty = []
	for key in prof.keys():
		if prof[key] == "":
			empty.append(key)

	return prof

def CreateActions(opts):
	actnames = [
		"onInitialize",
		"onFinalize",
		"onActivated",
		"onDeactivated",
		"onAborting",
		"onError",
		"onReset",
		"onExecute",
		"onStateUpdate",
		"onShutdown",
		"onStartup",
		"onRateChanged",
		]

	actions = {}
	for a in actnames:
		actions[a] = {
			"rtcDoc::doc": {
				"description": a + " description",
				"postCondition": a + " Post_condition",
				"preCondition": a + " Pre_condition"
				},
			"implemented": True
			}
	return actions

def CreateConfigurationSet(opts):
	"""
	MakeConfigurationParameters

	Create Configuration list from command options
	"""
	prof_list = []
	prof = {
		"name": "",
		"type": "",
		"varname": "",
		"defaultValue": "",
		"rtcDoc::doc":
			{
			"type": "type", # type
			"constraint": "constraint",
			"dataname": "dataname",
			"defaultValue": "default",
			"description": "description",
			"range": "range",
			"unit": "unit"
			}
		}
	for opt, arg in opts:
		if opt == ("--config"):
			try:
				# For C++ scope resolution operator 
				arg = re.sub("::", "@@", arg)
				name, type, default = arg.split(":")
				name    = re.sub("@@", "::", name)
				type    = re.sub("@@", "::", type)
				default = re.sub("@@", "::", default)
			except:
				sys.stderr.write("Invalid option: " \
							 + opt \
							 + "=" \
							 + arg)
			tmp = copy.deepcopy(prof)
			tmp["name"] = name
			tmp["varname"] = name
                        tmp["l_name"] = name.lower()
                        tmp["u_name"] = name.upper()
			tmp["type"] = type
			tmp["defaultValue"] = default
			tmp["rtcDoc::doc"]["defaultValue"] = default
			prof_list.append(tmp)
	return {'configuration': prof_list}


def CreateDataPorts(opts):
	"""
	MakePortProfile

	Create PortProfile list from command options
	"""
	prof_list = []
	prof = {
		"name": "",
		"portType": "",
		"type": "",
		"interfaceType": "CorbaPort",
		"dataflowType": "Push,Pull",
		"subscriptionType": "Periodic,New,Flush",
		"idlFile": "",
		"rtcDoc::doc":
			{
			"type": "",
			"description": "",
			"number": "",
			"occerrence": "",
			"operation": "",
			"semantics": "",
			"unit": ""
			},
		"rtcExt::position": "",
		"rtcExt::varname": ""
		}
	cnt = 0
	portType = {"--inport": "DataInPort", "--outport": "DataOutPort"}
	position = {"--inport": "LEFT", "--outport": "RIGHT"}
	for opt, arg in opts:
		if opt == "--inport" or opt == "--outport":
			try:
				arg = re.sub("::", "@@", arg)
				name, type = arg.split(":")
				name    = re.sub("@@", "::", name)
				type    = re.sub("@@", "::", type)
			except:
				sys.stderr.write("Invalid option: " \
							 + opt \
							 + "=" \
							 + arg)
			tmp = copy.deepcopy(prof)
			tmp["name"] = name
			tmp["portType"] = portType[opt]
			tmp["type"] = type
			tmp["num"]  = cnt
			tmp["rtcDoc::doc"]["type"] = type
			tmp["rtcDoc::doc"]["number"] = cnt
			tmp["rtcExt::varname"] = name
			tmp["rtcExt::position"] = position[opt]
			prof_list.append(tmp)
			cnt += 1
	return prof_list


def CreateServicePorts(opts):
	"""
	MakePortInterface

	Create Port interface profile list from command options
	"""
	prof_list = []

	prof = {
		"name": "",
		"rtcDoc::doc":
			{
			"description": "",
			"ifdescription": "",
			},
		"rtcExt::position": "LEFT",
		"serviceInterface": []
		}
	ifprof = {
		"name": "",
		"type": "",
		"direction": "",
		"varname": "",
		"instanceName": "",
		"idlFile": "",
		"path": "",
		"rtcDoc::doc":
			{
			"description": "",
			"docArgument": "",
			"docException": "",
			"docPostCondition": "",
			"docPreCondition": "",
			"docReturn": ""
			}
		}

	def findport(prof_list, port_name):
		for p in prof_list:
			if p["name"] == port_name:
				return p
		return None
	def lr(port):
		cnt = {"Provided": 0, "Required": 0}
		for ifprof in port["serviceInterface"]:
			cnt[ifprof["direction"]] += 1
		if cnt["Provided"] < cnt["Required"]:
			return "LEFT"
		else:
			return "RIGHT"
	cnt = 0
	ifType = {"--service": "Provided", "--consumer": "Required"}
	for opt, arg in opts:
		if opt == "--service" or opt == "--consumer":
			try:
				arg = re.sub("::", "@@", arg)
				portname, name, type = arg.split(":")
				portname = re.sub("@@", "::", portname)
				name     = re.sub("@@", "::", name)
				type     = re.sub("@@", "::", type)
			except:
				sys.stderr.write("Invalid option: " \
						 + opt \
						 + "=" \
						 + arg)
			port = findport(prof_list, portname)
			if port == None:
				port = copy.deepcopy(prof)
				port["name"] = portname
				prof_list.append(port)
				
			tmp = copy.deepcopy(ifprof)
			tmp["name"] = name
			tmp["type"] = type
			tmp["direction"] = ifType[opt]
			tmp["varname"] = name
			tmp["instanceName"] = name
			idlfname = FindIdlFile(opts, type)
			if idlfname == None:
				print("Error:")
				print("IDL file not found for a interface: ", type)
				sys.exit(1)
			tmp["idlFile"] = idlfname
			port["serviceInterface"].append(tmp)
			port["rtcExt::position"] = lr(port)
			cnt += 1
	return prof_list


def FindIdlFile(opts, ifname):
	_re_text = "interface\s+" + ifname
	for opt, arg in opts:
		if opt == "--service-idl" or opt == "--consumer-idl":
			fname = arg
			fd = open(fname, "r")
			if fd == None:
				print("IDL file not found: ", arg)
				sys.exit(1)
			t = fd.read()
			if None != re.compile(_re_text).search(t):
				fd.close()
				return fname
			fd.close()
	return None

def PickupIDLFiles(dict):
	svcidls = {}
	cnsidls = {}
	for svc in dict["servicePorts"]:
		for sif in svc["serviceInterface"]:
			if sif["direction"] == "Provided":
				svcidls[sif["idlFile"]] = ""
			elif sif["direction"] == "Required":
				if not svcidls.has_key(sif["idlFile"]):
					cnsidls[sif["idlFile"]] = ""
	dict["service_idl"] = []
	dict["consumer_idl"] = []
	for f in svcidls.keys():
		idl = {}
		idl["idl_fname"] = f
		idl["idl_basename"] = f.split(".")[0]
		dict["service_idl"].append(idl)
	for f in cnsidls.keys():
		idl = {}
		idl["idl_fname"] = f
		idl["idl_basename"] = f.split(".")[0]
		dict["consumer_idl"].append(idl)
	return

def CreateId(dict):
	dict["id"] = "RTC:" + \
	    dict["basicInfo"]["vendor"] + "." + \
	    dict["basicInfo"]["category"] + "." + \
	    dict["basicInfo"]["name"] + ":" + \
	    dict["basicInfo"]["version"]

def find_opt(opts, value, default):
	for opt, arg in opts:
		if opt.find(value) == 0:
			return arg

	return default


def find_opt_list(opts, value, default):
	list = []
	if len(default) > 0:
		list += default
	for opt, arg in opts:
		if opt == ("--" + value):
			list.append(arg)
	return list


class Backend:
	def __init__(self, mod_name, mod):
		self.mod = mod
		self.obj = getattr(mod, mod_name)
		self.mod_name = mod_name


class BackendLoader:
	def __init__(self):
		self.backends = {}
		self.opts = []
		self.available()
		return
		

	def available(self):
		path_list = [pyhelper_path, "."]
		for path in path_list:
			for f in os.listdir(path):
				if re.compile("_gen.py$").search(f):
					mod_name = f.replace(".py", "")
					opt_name = f.replace("_gen.py", "")
					mod = __import__(mod_name, globals(), locals(), [])
					try:
						mod.usage()
						be = Backend(mod_name, mod)
						self.backends[opt_name] = be
					except:
						pass

		return self.backends


	def check_args(self, args):
		for opt in args:
			if opt.find('-b') == 0:
				backend_name = opt.replace("-b", "")
				if self.backends.has_key(backend_name):
					self.opts.append(backend_name)
				else:
					print("No such backend: ", backend_name)
					sys.exit(-1)
			elif opt.find('--backend=') == 0:
				backend_name = opt.replace("--backend=", "")
				if self.backends.has_key(backend_name):
					self.opts.append(backend_name)
				else:
					print("No such backend: ", backend_name)
					sys.exit(-1)
		return self.opts


	def get_opt_fmts(self):
		fmts = []
		for be in self.opts:
			fmts += self.backends[be].mod.get_opt_fmt()
		return fmts


	def usage_available(self):
		print("The following backends are available.")
		space = 10
		for key in self.backends:
			desc = self.backends[key].mod.description()			
			print("    -b" + key + ("." * (space - len(key))) + desc)
		print("""
Backend [xxx] specific help can be available by the following options.
    -bxxx --help|-h or --backend=xxx --help|-h
	""")
		return


	def usage(self):
		for be in self.opts:
			print(self.backends[be].mod.usage())			
			print("")
		return

	def usage_short(self):
		for be in self.opts:
			print(self.backends[be].mod.usage_short())
			print("")
		return


	def generate_code(self, data, opts):
		for be in self.opts:
			self.backends[be].obj(data, opts).print_all()
		return
		

def fmtd_args(width, args):
	arg_fmt = [""]
	w = 0
	line = 0
	for a in args:
		w += len(a) + 1
		if w > width:
			w = len(a) + 1
			line += 1
			arg_fmt.append("")
		arg_fmt[line] += a + " "
	return arg_fmt



def main():
	global opt_args_fmt

	backends = BackendLoader()
	backends.check_args(sys.argv[1:])
	opt_args_fmt += backends.get_opt_fmts()

	try:
		opts, args = getopt.getopt(sys.argv[1:], "b:ho:v", opt_args_fmt)
	except getopt.GetoptError:
		print("Error: Invalid option.", getopt.GetoptError)
		usage_short()
		backends.usage_available()
		sys.exit(-1)

	if not opts:
		usage_short()
		backends.usage_available()
		sys.exit(-1)

	output = None
	verbose = False
	output_cxx = False
	output_python = False

	for o, a in opts:
		if o == "-v":
			verbose = True
		if o in ("-h"):
			usage_short()
			backends.usage_available()
			backends.usage_short()
			sys.exit(0)
		if o in ("--help"):
			usage()
			backends.usage_available()
			backends.usage()
			sys.exit(0)
		if o in ("-o", "--output"):
			output = a
			# ...

	prefix = os.popen("rtm-config --prefix", "r").read().split("\n")
	idl_inc = []
	if prefix[0] != '':
		idl_inc.append(prefix[0] + "/include/rtm/idl")
		idl_inc.append(prefix[0] + "/include/rtm")
	idl_inc.append(".")

	# Create dictionary for ezt
	data = {'basicInfo':        CreateBasicInfo(opts),
		'actions':          CreateActions(opts),
                'configurationSet': CreateConfigurationSet(opts),
		'dataPorts':        CreateDataPorts(opts),
		'servicePorts':     CreateServicePorts(opts),
		'args':             sys.argv,
		'fmtd_args':        fmtd_args(70, sys.argv),
		'idl_include':      idl_inc
		}
	CreateId(data)
	PickupIDLFiles(data)

	if not data.has_key('fname'):
		data['fname'] = data['basicInfo']['name']
	backends.generate_code(data, opts)

	import README_gen
	readme = README_gen.README_gen(data)
	readme.print_all()
	import profile_gen
	profile = profile_gen.profile_gen(data)
	profile.print_all()
	return
	

if __name__ == "__main__":
	main()
