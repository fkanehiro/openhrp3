#-*-Mode: Makefile;-*-
all: target

TOP = ..\..\#
!include $(TOP)Make.rules.mak

CXX_FLAGS = $(CXX_FLAGS) /IsDIMS /I../$(CORBA_DIR) /I$(CLAPACK_DIR) /I$(CLAPACK_DIR)/F2CLIBS -wd4996 -DSEGA

!include Makefile.common

$(SDIMSLIB): $(SDIMS_OBJS)
	lib $(OUTOPT)$@ $(SDIMS_OBJS) 

$(LIBOBJS):
	$(CXX) $(CXX_FLAGS) /DHRPMODEL_MAKE_DLL $(OBJOPT)$@ $(@:.o=.cpp)
$(SERVEROBJS):
	$(CXX) $(CXX_FLAGS) $(OBJOPT)$@ $(@:.o=.cpp)


Sensor.o: Sensor.cpp Sensor.h OpenHRPConfig.h
World.o: World.cpp World.h OpenHRPConfig.h

server.o: World.h
ModelLoaderUtil.o: ModelLoaderUtil.cpp ModelLoaderUtil.h OpenHRPConfig.h
DynamicsSimulator_impl.o: DynamicsSimulator_impl.cpp DynamicsSimulator_impl.h ModelLoaderUtil.h Sensor.h World.h 
