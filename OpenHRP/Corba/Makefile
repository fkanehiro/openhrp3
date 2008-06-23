all: HRPCorbaStubs.lib HRPCorbaSkeletons.lib

TOP =../../
HEADER_DIR=.
LIBS_DIR=../../lib/
include $(TOP)Make.rules

CXX_FLAGS += -I$(HEADER_DIR)

HRPCorbaStubs.lib: CollisionDetector.o Controller.o DynamicsSimulator.o \
	OpenHRPCommon.o ViewSimulator.o ModelLoader.o OnlineViewer.o
	$(AR) $(ARFLAGS) $@ $^
	cp $@ $(LIBS_DIR)
 
HRPCorbaSkeletons.lib: CollisionDetectorDynSK.o ControllerDynSK.o \
	DynamicsSimulatorDynSK.o OpenHRPCommonDynSK.o ViewSimulatorDynSK.o OnlineViewerDynSK.o
	$(AR) $(ARFLAGS) $@ $^
	cp $@ $(LIBS_DIR)

OpenHRPCommon.cpp OpenHRPCommon.h : $(IDL_DIR)/OpenHRPCommon.idl
	$(OB_IDLC) $(OB_IDL_FLAGS) $(IDL_DIR)/OpenHRPCommon.idl

DynamicsSimulator.cpp DynamicsSimulator.h : $(IDL_DIR)/DynamicsSimulator.idl
	$(OB_IDLC) $(OB_IDL_FLAGS) $(IDL_DIR)/DynamicsSimulator.idl

ModelLoader.cpp ModelLoader.h : $(IDL_DIR)/ModelLoader.idl
	$(OB_IDLC) $(OB_IDL_FLAGS) $(IDL_DIR)/ModelLoader.idl

CollisionDetector.cpp CollisionDetector.h : $(IDL_DIR)/CollisionDetector.idl
	$(OB_IDLC) $(OB_IDL_FLAGS) $(IDL_DIR)/CollisionDetector.idl

Controller.h Controller.cpp: $(IDL_DIR)/Controller.idl
	$(OB_IDLC) $(OB_IDL_FLAGS) $(IDL_DIR)/Controller.idl

ViewSimulator.h ViewSimulator.cpp: $(IDL_DIR)/ViewSimulator.idl
	$(OB_IDLC) $(OB_IDL_FLAGS) $(IDL_DIR)/ViewSimulator.idl

OnlineViewer.cpp OnlineViewer.h : $(IDL_DIR)/OnlineViewer.idl
	$(OB_IDLC) $(OB_IDL_FLAGS) $(IDL_DIR)/OnlineViewer.idl

ViewSimulator.o: ViewSimulator.cpp ViewSimulator.h OpenHRPCommon.h
DynamicsSimulator.o: DynamicsSimulator.cpp DynamicsSimulator.h OpenHRPCommon.h
CollisionDetector.o: CollisionDetector.cpp CollisionDetector.h OpenHRPCommon.h \
	ModelLoader.h
ModelLoader.o: ModelLoader.cpp ModelLoader.h OpenHRPCommon.h
OnlineViewer.o: OnlineViewer.cpp OnlineViewer.h OpenHRPCommon.h
OpenHRPCommon.o: OpenHRPCommon.cpp OpenHRPCommon.h
Controller.o: Controller.cpp Controller.h OpenHRPCommon.h DynamicsSimulator.h \
	ViewSimulator.h

ViewSimulatorDynSK.o: ViewSimulatorDynSK.cpp ViewSimulator.h
DynamicsSimulatorDynSK.o: DynamicsSimulatorDynSK.cpp DynamicsSimulator.h
CollisionDetectorDynSK.o: CollisionDetectorDynSK.cpp CollisionDetector.h
ModelLoaderDynSK.o: ModelLoaderDynSK.cpp ModelLoader.h
OnlineViewerDynSK.o: OnlineViewerDynSK.cpp OnlineViewer.h
OpenHRPCommonDynSK.o: OpenHRPCommonDynSK.cpp OpenHRPCommon.h
ControllerDynSK.o: ControllerDynSK.cpp Controller.h

clean: del_generic clean_subdirs
	-$(RM) *.cpp OpenHRPCommon.h ViewSimulator.h CollisionDetector.h DynamicsSimulator.h ModelLoader.h OnlineViewr.h Controller.h
