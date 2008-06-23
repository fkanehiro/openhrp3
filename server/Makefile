SUBDIRS = AistDynamicsSimulator CollisionDetector ControllerBridge UtDynamicsSimulator ModelLoader

all:
	for dir in $(SUBDIRS); do $(MAKE) -C $$dir all; done;

clean:
	for dir in $(SUBDIRS); do $(MAKE) -C $$dir clean; done;
