.PHONY: all

all: target

TOP = ../../
include $(TOP)Make.rules

CXX_FLAGS += -I../../OpenHRP/Corba -IsDIMS -DSEGA

include Makefile.common

$(SDIMSLIB): $(SDIMS_OBJS)
	ar rcus $@ $?

depend: $(CORBASRCS) $(LIBSRCS) $(SERVERSRCS) Makefile 
	@echo -n "make depend - "
	@rm -f .depend
	@touch .depend
	@for src in $(CORBASRCS);  do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@for src in $(LIBSRCS);    do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@for src in $(SERVERSRCS); do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@echo "finished"

ifeq (.depend, $(wildcard .depend))
include .depend
endif
