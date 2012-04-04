all:targets make_subdirs

SUBDIRS = corba script

TOP = ../../
include $(TOP)Make.rules

#depend:
#	$(MAKE) -C $(CORBA_DIR) depend

include Makefile.common

install: $(TOP)Make.rules $(TOP)Make.vars Makefile
	-$(MKDIR) $(OPENHRPHOME_DIR)/jar
	-$(INSTALLDATA) *.jar $(OPENHRPHOME_DIR)/jar
	$(MAKE) -C $(CORBA_DIR) install

