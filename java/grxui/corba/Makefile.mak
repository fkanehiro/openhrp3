# -*- Makefile -*-
all: target

IDL_BASE = OnlineViewer

TOP = ..\..\..\#
!include $(TOP)Make.rules.mak

!include Makefile.common

$(OBJS):
	$(CXX) $(CXX_FLAGS) /D_MAKE_DLL $(OBJOPT)$@ $(@:.o=.cpp)

#clean: del_generic