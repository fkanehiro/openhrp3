.PHONY: all robot

all: target

TOP = ../../
HEADER_DIR=../../OpenHRP

include $(TOP)Make.rules

LIBSFX   = a
AR	 = ar
AR_FLAGS = rcus

CXX_FLAGS += -I$(HEADER_DIR)/Corba -I$(HEADER_DIR)/Parser

BOOST_LIBS = -lboost_signals$(BOOST_LIB_SFX)

#include Makefile.common
#-*-Mode: Makefile;-*-

SERVERSRCS = \
	ModelLoader_impl.cpp \
	BodyInfo_impl.cpp \
	VrmlFieldCopyUtil.cpp \
	ImageConverter.cpp \
	server.cpp 

SERVEROBJS	= $(SERVERSRCS:.cpp=.o)

TARGETS = ModelLoader$(EXESFX)

target: $(TARGETS)


ModelLoader$(EXESFX): ../../lib/$(LIBPFX)HRPModelParser.$(DLLSFX) $(SERVEROBJS)
	$(LINK) $(LINK_FLAGS) $(OUTOPT)$@ $(SERVEROBJS) ../../lib/HRPCorbaStubs.lib $(LPOPT)../../lib $(LIBOPT)HRPModelParser $(LIBOPT)HRPModelUniform $(OB_LIBS) $(LIBPNG_LIB) $(LIBJPEG_LIB)
	cp $@ ../../bin/

clean: del_generic
	-$(RM) $(TARGETS)

# end include Makefile.common

depend: $(SERVERSRCS)  Makefile 
	@echo -n "make depend - "
	@rm -f .depend
	@touch .depend
	@for src in $(SERVERSRCS); do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@echo "finished"

ifeq (.depend, $(wildcard .depend))
include .depend
endif
