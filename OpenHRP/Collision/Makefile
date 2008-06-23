all: target

TOP =../../
include $(TOP)Make.rules

CXX_FLAGS += -I. -I../$(CORBA_DIR)

#include Makefile.common
#-*-Mode: Makefile;-*-

#TARGET = $(LIBPFX)cd.$(DLLSFX) CollisionDetector$(EXESFX)
TARGET = $(LIBPFX)hrpCollision.$(DLLSFX)
target: $(TARGET)

ICE_SRCS = Ice/IceAABB.cpp \
	Ice/IceContainer.cpp \
	Ice/IceIndexedTriangle.cpp \
	Ice/IceMatrix3x3.cpp \
	Ice/IceMatrix4x4.cpp \
	Ice/IceRevisitedRadix.cpp \
	Ice/IceHPoint.cpp \
	Ice/IceRandom.cpp \
	Ice/IcePoint.cpp

ICE_OBJS = $(ICE_SRCS:.cpp=.o)

CD_SRCS = InsertCollisionPair.cpp \
	CdCache.cpp \
	CdScene.cpp \
	TriOverlap.cpp \
	CdWrapper.cpp \
	OPC_AABBTree.cpp \
	OPC_BaseModel.cpp \
	OPC_Collider.cpp \
	OPC_Common.cpp \
	OPC_MeshInterface.cpp \
	OPC_Model.cpp \
	OPC_OptimizedTree.cpp \
	OPC_TreeBuilders.cpp \
	OPC_TreeCollider.cpp \
	OPC_VolumeCollider.cpp 

CD_OBJS = $(CD_SRCS:.cpp=.o)
CD_LIBS	= $(LPOPT). $(LIBOPT)cd$(LIBSFX)

$(LIBPFX)hrpCollision.$(DLLSFX): $(ICE_OBJS) $(CD_OBJS)
	$(LINK) $(SOFLAGS) $(OUTOPT)$@ $(ICE_OBJS) $(CD_OBJS) $(MATH_LIB)
	cp $@ ../../lib/
#	cp *.h ../../include/OpenHRP/Collision/
#	cp Ice/*.h ../../include/OpenHRP/Collision/Ice/

clean: del_generic
	-$(RM) $(TARGET)
	-$(RM) $(ICE_OBJS)

#end include

# something wrong with g++'s -O2 
ifeq ("$(CXX)", "g++")
$(CD_OBJS):
	$(CXX) $(CXX_FLAGS) -O $(*F).cpp -o $@ 
endif

ifdef USE_INTEL_COMPILER
#CXX_FLAGS +=  -parallel -par-report=3 -par-threshold100
#CXX_FLAGS +=  -parallel -par-report=3
endif

ifeq ("$(OSNAME)", "QNX")
MATH_LIB = -lm
endif

depend: $(SRCS) Makefile
	@echo "make depend"
	@rm .depend ; touch .depend
	@for src in $(ICE_SRCS); do $(CXX) -M $(CXX_FLAGS) $$src >> .depend ; done
	@for src in $(CD_SRCS); do $(CXX) -M $(CXX_FLAGS) $$src >> .depend ; done

# depends
ifeq (.depend, $(wildcard .depend))
include .depend
else
#server.o: server.cpp CollisionDetector_impl.h CdCache.h CdScene.h
#CollisionDetector_impl.o: CollisionDetector_impl.cpp CollisionDetector_impl.h
CdCache.o: CdCache.cpp CdCache.h
CdScene.o: CdScene.cpp CdScene.h CdWrapper.h
CdWrapper.o: CdWrapper.cpp CdWrapper.h
TriOverlap.o: TriOverlap.cpp utilities.h
endif
