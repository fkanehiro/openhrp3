SUBDIRS = OpenHRP server java client bin/unix sample

all:
	for dir in $(SUBDIRS); do $(MAKE) -C $$dir all; done;

clean:
	for dir in $(SUBDIRS); do $(MAKE) -C $$dir clean; done;
