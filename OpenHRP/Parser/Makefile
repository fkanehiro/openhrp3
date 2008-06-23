.PHONY: all robot

all: target

TOP = ../../
include $(TOP)Make.rules

LIBSFX   = a
AR	 = ar
AR_FLAGS = rcus

CXX_FLAGS += -I../Corba -I.

BOOST_LIBS = -lboost_signals$(BOOST_LIB_SFX)

#include Makefile.common
#-*-Mode: Makefile;-*-

#CORBASRCS = \
#	../$(CORBA_DIR)/OpenHRPCommon.cpp \
#	../$(CORBA_DIR)/ModelLoader.cpp

SCANNERSRC = EasyScanner.cpp

PARSERSRCS = \
	ModelNodeSet.cpp \
	VrmlNodes.cpp \
	VrmlParser.cpp \
	VrmlWriter.cpp

UNIFORMSRCS = \
	UniformedShape.cpp \
	CalculateNormal.cpp

#SERVERSRCS = \
#	ModelLoader_impl.cpp \
#	BodyInfo_impl.cpp \
#	VrmlFieldCopyUtil.cpp \
#	ImageConverter.cpp \
#	server.cpp 

#CORBAOBJS	= $(CORBASRCS:.cpp=.o)
PARSEROBJS	= $(PARSERSRCS:.cpp=.o)
UNIFORMOBJS	= $(UNIFORMSRCS:.cpp=.o)
#SERVEROBJS	= $(SERVERSRCS:.cpp=.o)

TARGETS = \
	$(LIBPFX)EasyScanner.$(LIBSFX) \
	$(LIBPFX)HRPModelUniform.$(DLLSFX) \
	$(LIBPFX)HRPModelParser.$(DLLSFX) \
#	ModelLoader$(EXESFX)

target: $(TARGETS)


$(LIBPFX)EasyScanner.$(LIBSFX): EasyScanner.o
	$(AR) $(AR_FLAGS) $@ $?
	cp $@ ../../lib/

$(LIBPFX)HRPModelParser.$(DLLSFX): $(PARSEROBJS) $(LIBPFX)EasyScanner.$(LIBSFX)
	$(LINK) $(LINK_FLAGS) $(SOFLAGS) $(OUTOPT)$@ $(PARSEROBJS) $(BOOST_LIBS) $(LPOPT). -lEasyScanner 
	cp $@ ../../lib/

$(LIBPFX)HRPModelUniform.$(DLLSFX): $(UNIFORMOBJS)
	$(LINK) $(LINK_FLAGS) $(SOFLAGS) $(OUTOPT)$@ $(UNIFORMOBJS) $(BOOST_LIBS) $(LPOPT).
	cp $@ ../../lib/

#ModelLoader$(EXESFX): $(LIBPFX)HRPModelParser.$(DLLSFX) $(SERVEROBJS) $(CORBAOBJS)
#	$(LINK) $(LINK_FLAGS) $(OUTOPT)$@ $(SERVEROBJS) $(CORBAOBJS) $(LPOPT). $(LIBOPT)HRPModelParser $(LIBOPT)HRPModelUniform $(OB_LIBS) $(LIB#PNG_LIB) $(LIBJPEG_LIB)

clean: del_generic
	-$(RM) $(TARGETS)

# end include Makefile.common

#depend: $(CORBASRCS) $(PARSERSRCS) $(SCANNERSRC) $(SERVERSRCS) Makefile 
depend: $(PARSERSRCS) $(SCANNERSRC)  Makefile 
	@echo -n "make depend - "
	@rm -f .depend
	@touch .depend
#	@for src in $(CORBASRCS);  do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@for src in $(PARSERSRCS);    do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@for src in $(SCANNERSRC);    do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done	
#	@for src in $(SERVERSRCS); do $(CXX) -M $(CXX_FLAGS) $$src >> .depend; done
	@echo "finished"

ifeq (.depend, $(wildcard .depend))
include .depend
endif
