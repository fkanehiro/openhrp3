.PHONY: all

all: target

TOP = ../../
HEADER_DIR=../../OpenHRP
include $(TOP)Make.rules

#CXX_FLAGS += -I../$(CORBA_DIR)
CXX_FLAGS += -I$(HEADER_DIR)/Corba -I$(HEADER_DIR)/Collision

ifeq ("$(OSNAME)","QNX")
MATH_LIBS=-lblas -lf2c -lm
else
DL_LIBS=-ldl
endif

ifdef USE_INTEL_COMPILER
#CXX_FLAGS +=  -parallel -par-report=3 -par-runtime-control -ansi-alias -restrict -unroll0
ifndef OPENMP
#OMP_LIB = -lguide -lpthread
endif
LINK_FLAGS += $(INTEL_RPATH)
endif

BOOST_LIBS = -lboost_filesystem$(BOOST_LIB_SFX) -lboost_regex$(BOOST_LIB_SFX)

#include Makefile.common
#-*-Mode: Makefile;-*-

SERVERSRCS = \
	CollisionDetector_impl.cpp \
	server.cpp 

SERVEROBJS	= $(SERVERSRCS:.cpp=.o)

TARGETS = CollisionDetector$(EXESFX)

target: $(TARGETS)

CollisionDetector$(EXESFX): ../../lib/$(LIBPFX)hrpCollision.$(DLLSFX) $(SERVEROBJS)  
	$(LINK) $(LINK_FLAGS) $(OUTOPT)$@ $(SERVEROBJS) ../../lib/HRPCorbaStubs.lib $(LPOPT)../../lib $(LIBOPT)hrpCollision$(LIBSFX) $(LAPACK_LIBS) $(OB_LIBS) $(MATH_LIBS) $(OMP_LIB)
	cp $@ ../../bin/

clean: del_generic
	$(RM) $(TARGETS)

#end include Makefile.common

depend: $(SERVERSRCS) Makefile 
	@echo -n "make depend - "
	@rm -f .depend
	@touch .depend	
	@for src in $(SERVERSRCS); do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@echo "finished"

ifeq (.depend, $(wildcard .depend))
include .depend
endif
