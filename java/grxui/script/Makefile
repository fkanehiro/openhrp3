all:target

TOP =../../../
include $(TOP)Make.rules

class_files = rbimporter$$py.class

target: $(class_files)

%$$py.class: %.py
	$(JAVA) -jar $(JYTHON_JAR) \
-Dpython.cachedir=$(JYTHON_CACHE) -Dpython.home=$(JYTHON_HOME) compile.py $<

clean: del_generic clean_subdirs
	-$(RM) *.class
