all: target

MODULE = OnlineViewer
IDL_BASE = OnlineViewer


TOP =../../../
include $(TOP)Make.rules

include Makefile.common

CORBAOBJS = $(SRCS:.cpp=.o)

CXX_FLAGS += -I. -fno-schedule-insns -fno-schedule-insns2 -fno-strict-aliasing

#$(CORBAOBJS):
#	$(CXX) $(CXX_FLAGS) $(OBJOPT)$@ $<

stubs: $(HDRS) $(CORBAOBJS)

depend: $(HDRS) $(SRCS) Makefile
	@echo "make depend"
	@rm .depend; touch .depend
	@for src in $(SRCS); do $(CXX) -M $(CXX_FLAGS) $$src > .depend; done

# depends
ifeq (.depend, $(wildcard .depend))
include .depend
endif

install: $(TOP)Make.rules $(TOP)Make.vars Makefile
	-$(MKDIR) $(OPENHRPHOME_DIR)/include/OpenHRP/$(CORBA_DIR)
	-$(MKDIR) $(OPENHRPHOME_DIR)/idl/OpenHRP
	-$(MKDIR) $(OPENHRPHOME_DIR)/lib
	-$(INSTALLDATA) $(HDRS) $(OPENHRPHOME_DIR)/include/OpenHRP/$(CORBA_DIR)
	-$(INSTALLDATA) *.idl $(OPENHRPHOME_DIR)/idl/OpenHRP
	-$(INSTALL) $(LIBPFX)grxuiclient.$(DLLSFX) $(OPENHRPHOME_DIR)/lib
clean: 

