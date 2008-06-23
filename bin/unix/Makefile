all:  configsh OpenHRPpy

TOP =../../
include $(TOP)Make.rules
configsh: config.sh

OpenHRPpy: $(TOP)Make.rules $(TOP)Make.vars Makefile
	echo . `cd $(TOP) ; pwd`/bin/unix/config.sh > OpenHRPpy
	echo java -Dpython.home=\$$JYTHON_DIR -classpath \$$JYTHON_DIR/jython.jar:`cd $(TOP); echo $$PWD`/java/openhrpstubskel.jar:\$$HRP_STUB_PATH -DNS_OPT=\"\$$NS_OPT\" org.python.util.jython $$\@ >> OpenHRPpy
	chmod a+x OpenHRPpy

config.sh: $(TOP)Make.rules $(TOP)Make.vars Makefile
	echo export OPENHRPHOME=$(shell cd $(TOP); pwd) > config.sh
	echo >> config.sh
	echo -n export DYNAMICS_SIMULATOR_DIR=$$>> config.sh
	echo -n OPENHRPHOME/bin/>> config.sh
	echo >>  config.sh
	#echo export JDK_DIR=$(JDK_DIR) >> config.sh
ifeq ("$(JDK_DIR)", "")	
	echo export JAVAVM=java >> config.sh
else
	echo export JAVAVM=$(JDK_DIR)/bin/java >> config.sh
endif
	echo export JYTHON_DIR=$(JYTHON_DIR) >> config.sh
	echo >>  config.sh

	echo -n export PATH=$$>> config.sh
	echo -n PATH:$$>> config.sh
	echo    OPENHRPHOME/bin/unix >> config.sh

	echo -n export LD_LIBRARY_PATH=. >> config.sh
ifneq ("$(CLAPACK_DIR)", "")
	echo -n :$$ >> config.sh
	echo -n CLAPACK_DIR >> config.sh
endif	
ifneq ("$(OB_DIR)", "")
	echo -n :$(OB_DIR)/lib >> config.sh
endif
ifneq ("$(OPENRTM_DIR)", "")
	echo -n :$(OPENRTM_DIR)/lib >> config.sh
endif
ifneq ("$(BOOST_LIB_DIR)", "")
	echo -n :$(BOOST_LIB_DIR) >> config.sh
endif	

ifneq ("$(INTEL_COMPILER_VERSION)","")
ifeq ("$(M_ARCHITECTURE)", "i386")
	echo -n :/opt/intel/cc/$(INTEL_COMPILER_VERSION)/lib >> config.sh
else
	echo -n :/opt/intel/cce/$(INTEL_COMPILER_VERSION)/lib >> config.sh
endif
endif

ifeq  ("$(CLAPACK_VARIANT)","MKL")
ifeq  ("$(M_ARCHITECTURE)", "i386")
	echo -n :/opt/intel/mkl/9.1/lib/32 >> config.sh
else
	echo -n :/opt/intel/mkl/9.1/lib/em64t >> config.sh
endif
endif
	echo -n :$$ >> config.sh
	echo -n OPENHRPHOME/bin >> config.sh
#	echo -n :$$ >> config.sh
#	echo -n OPENHRPHOME/PatternGenerator/server >> config.sh
	echo -n :$$ >> config.sh
	echo -n LD_LIBRARY_PATH >> config.sh

	echo >>  config.sh
	echo >>  config.sh

	echo -n export CLASSPATH=$$ >> config.sh
	echo -n OPENHRPHOME/java/openhrpstubskel.jar >> config.sh
	echo -n :$$ >> config.sh
	echo OPENHRPHOME/gui/plugin/com.generalrobotix.ui.grxui_0.9.5/vrml97.jar >> config.sh

ifneq ("$(JAVA_EXTDIR_FLAGS)","")
	echo    export EXTRA_JAVA_FLAGS=$(JAVA_EXTDIR_FLAGS) >> config.sh
endif

	echo >> config.sh

	echo export NS_OPT=\"-ORBInitRef NameService=corbaloc:iiop:$(NS_HOST):$(NS_PORT)/NameService\" >> config.sh
	echo -n export NAMESERV=\" >> config.sh
ifneq ("$(OB_DIR)","")
	echo -n $(OB_DIR)/bin/ >> config.sh
endif
	echo -n omniNames -start $(NS_PORT) -logdir $$ >> config.sh
	echo -n OPENHRPHOME/bin/unix\" >> config.sh
	echo >> config.sh

#	echo -n export HRP_STUB_PATH=$$ >> config.sh
#	echo    OPENHRPHOME/Controller/IOserver/corba/IOserverStubskel.jar >> config.sh
#	echo -n export LIBHRPMODEL_PLUGIN_PATH=$$ >> config.sh
#	echo    OPENHRPHOME/DynamicsSimulator/server/plugin >> config.sh
ifneq ("$(ROBOT)", "")
	echo -n export ROBOT= >> config.sh
	echo $(ROBOT) >> config.sh
endif
	chmod +x *.sh

clean: del_generic
	-$(RM) config.sh
