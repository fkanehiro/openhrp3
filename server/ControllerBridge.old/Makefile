all: target

TOP = ../../
HEADER_DIR=../../OpenHRP
include $(TOP)Make.rules

TARGET = ControllerBridge$(EXESFX)
target: $(TARGET)

SRCS =  \
	BridgeConf.cpp \
	VirtualRobotPortHandler.cpp \
	VirtualRobotRTC.cpp \
	Controller_impl.cpp \
	main.cpp

OBJS = $(SRCS:.cpp=.o)

CXX_FLAGS += -I$(HEADER_DIR)/Corba/
# CXX_FLAGS += -I$(OPENRTM_DIR)/include -I$(OPENRTM_DIR)/include/rtm/idl

BOOST_LIBS = -lboost_filesystem$(BOOST_LIB_SFX) -lboost_program_options$(BOOST_LIB_SFX) -lboost_regex$(BOOST_LIB_SFX)

depend: $(SRCS)
	@echo "make depend"
	@$(RM) .depend ; touch .depend
	@for src in $(SRCS); do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done

# depends
ifeq (.depend, $(wildcard .depend))
include .depend
endif

ControllerBridge$(EXESFX): $(OBJS) 
	$(LINK) $(LINK_FLAGS) $(OUTOPT)$@ $(OBJS) ../../lib/HRPCorbaStubs.lib $(OB_LIBS) $(RTM_LIBS) $(BOOST_LIBS) 
	cp $@ ../../bin/

clean: del_generic
	-$(RM) $(TARGET)
