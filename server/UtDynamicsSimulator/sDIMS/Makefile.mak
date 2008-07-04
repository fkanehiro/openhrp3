#-*-Mode: Makefile;-*-
all: target

TOP = ..\..\..\#
!include $(TOP)Make.rules.mak

CXX_FLAGS = $(CXX_FLAGS) /I$(CLAPACK_DIR) /I$(CLAPACK_DIR)\F2CLIBS /I../../corba -wd4996 -DSEGA

!include Makefile.common

TARGET = $(LIBPFX)sDIMS$(LIBSFX) 
target: $(TARGET)

$(TARGET): $(OBJS) 
	lib $(OUTOPT)$@ $(OBJS) 
