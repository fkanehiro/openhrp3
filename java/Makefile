all: openhrpstubskel.jar

#IDL_BASE = Common
IDL_BASE = ../idl/OpenHRP
TOP = ../
include $(TOP)Make.rules

# include Makefile.common
# -*- Makefile -*-

JAVA_STUB_SRC  = OpenHRPCommon:CollisionDetector:ModelLoader:DynamicsSimulator:ViewSimulator:OnlineViewer

STUB_PATH = -d . -cp $(PACKAGE_DIR) -sourcepath $(JAVA_STUB_SRC)

openhrpstubskel.jar: \
#	$(IDL_BASE)/OpenHRPCommon.idl \
#	$(IDL_BASE)/CollisionDetector.idl \
#	$(IDL_BASE)/ModelLoader.idl \
#	$(IDL_BASE)/DynamicsSimulator.idl \
#	$(IDL_BASE)/Controller.idl \
#	$(IDL_BASE)/ViewSimulator.idl \
#	$(IDL_BASE)/OnlineViewer.idl
	$(OB_JIDLC) $(OB_JIDL_FLAGS) -td OpenHRPCommon $(IDL_BASE)/OpenHRPCommon.idl
	$(OB_JIDLC) $(OB_JIDL_FLAGS) -td CollisionDetector $(IDL_BASE)/CollisionDetector.idl
	$(OB_JIDLC) $(OB_JIDL_FLAGS) -td ModelLoader $(IDL_BASE)/ModelLoader.idl
	$(OB_JIDLC) $(OB_JIDL_FLAGS) -td DynamicsSimulator $(IDL_BASE)/DynamicsSimulator.idl
	$(OB_JIDLC) $(OB_JIDL_FLAGS) -td Controller $(IDL_BASE)/Controller.idl
	$(OB_JIDLC) $(OB_JIDL_FLAGS) -td ViewSimulator $(IDL_BASE)/ViewSimulator.idl
	$(OB_JIDLC) $(OB_JIDL_FLAGS) -td GrxUI $(IDL_BASE)/OnlineViewer.idl
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) OpenHRPCommon/$(PACKAGE_DIR)/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) CollisionDetector/$(PACKAGE_DIR)/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) ModelLoader/$(PACKAGE_DIR)/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) ModelLoader/$(PACKAGE_DIR)/ModelLoaderPackage/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) DynamicsSimulator/$(PACKAGE_DIR)/DynamicsSimulatorPackage/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) DynamicsSimulator/$(PACKAGE_DIR)/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) Controller/$(PACKAGE_DIR)/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) ViewSimulator/$(PACKAGE_DIR)/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) ViewSimulator/$(PACKAGE_DIR)/ViewSimulatorPackage/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) ViewSimulator/$(PACKAGE_DIR)/CameraPackage/*.java
	$(JAVAC) $(JAVAC_FLAGS) $(STUB_PATH) GrxUI/$(PACKAGE_DIR)/*.java
	$(JAR) cf openhrpstubskel.jar jp

#clean: del_generic del_stubskel
clean: 
	-$(RM) openhrpstubskel.jar
	-$(RMDIR) CollisionDetector ModelLoader GrxUI OpenHRPCommon DynamicsSimulator Controller ViewSimulator jp

#end include Makefile.common
