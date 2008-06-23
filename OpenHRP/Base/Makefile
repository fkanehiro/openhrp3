.PHONY: all

all: target

TOP = ../../
HEADER_DIR=../Corba/
include $(TOP)Make.rules

#CXX_FLAGS += -I../$(CORBA_DIR)
CXX_FLAGS += -I$(HEADER_DIR)

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

LIBSRCS = \
	Sensor.cpp \
	Body.cpp \
	BodyCustomizerInterface.cpp \
	Link.cpp \
	LinkTraverse.cpp \
	LinkPath.cpp \
	ForwardDynamics.cpp \
	ForwardDynamicsABM.cpp \
	ForwardDynamicsCBM.cpp \
	tvmet3d.cpp \
	quaternion.cpp \
	MatrixSolvers.cpp \
	World.cpp \
	ConstraintForceSolver.cpp \
	ModelLoaderUtil.cpp

LIBOBJS		= $(LIBSRCS:.cpp=.o)

#TARGETS = $(LIBPFX)hrpModel3.$(DLLSFX)
TARGETS = $(LIBPFX)hrpBase.$(DLLSFX)

target: $(TARGETS)

$(LIBPFX)hrpBase.$(DLLSFX): $(LIBOBJS)  
	$(LINK) $(LINK_FLAGS) $(SOFLAGS) $(OUTOPT)$@ ../../lib/HRPCorbaStubs.lib  $(LIBOBJS) $(LAPACK_LIBS) $(OB_LIBS) $(BOOST_LIBS) $(DL_LIBS)
	cp $@ ../../lib/

clean: del_generic
	$(RM) $(TARGETS)

#end include Makefile.common

depend: $(LIBSRCS) Makefile 
	@echo -n "make depend - "
	@rm -f .depend
	@touch .depend	
	@for src in $(LIBSRCS);    do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@echo "finished"

ifeq (.depend, $(wildcard .depend))
include .depend
endif
