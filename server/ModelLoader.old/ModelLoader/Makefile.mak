#-*-Mode: Makefile;-*-
all: mljar

TOP = ..\..\#
!include $(TOP)Make.rules.mak

#
# Flags used in Java compiler
#
JAVAC_FLAGS = $(JAVAC_FLAGS) -encoding EUCJIS -classpath .$(CPSEP)$(COMMON_DIR)/openhrpstubskel.jar$(CPSEP)$(MODELLOADER)/server/vrml97/vrml97.jar -d . 

!include Makefile.common
