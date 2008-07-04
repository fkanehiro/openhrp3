all:targets make_subdirs

SUBDIRS = corba

TOP = ../../#
!include $(TOP)Make.rules.mak

#depend:
#	$(MAKE) -C $(CORBA_DIR) depend

!include Makefile.common
