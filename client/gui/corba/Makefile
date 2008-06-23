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


clean: 

