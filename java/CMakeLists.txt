set(GrxUI_ON_ECLIPSE_DIR ${OpenHRP_SOURCE_DIR}/GrxUIonEclipse-project-0.9.8)

macro(configure_grxui_on_eclipse)
    set(depends_rtm_path "src/com/generalrobotix/ui/depends/rtm")
    CONFIGURE_FILE( ${GrxUI_ON_ECLIPSE_DIR}/${depends_rtm_path}/SwitchDependVerClockGenerator.java.in ${GrxUI_ON_ECLIPSE_DIR}/${depends_rtm_path}/SwitchDependVerClockGenerator.java)
    CONFIGURE_FILE( ${GrxUI_ON_ECLIPSE_DIR}/META-INF/MANIFEST.MF.in ${GrxUI_ON_ECLIPSE_DIR}/META-INF/MANIFEST.MF)
    CONFIGURE_FILE( ${GrxUI_ON_ECLIPSE_DIR}/classpath.in ${GrxUI_ON_ECLIPSE_DIR}/.classpath)
    CONFIGURE_FILE( ${GrxUI_ON_ECLIPSE_DIR}/plugin.xml.in ${GrxUI_ON_ECLIPSE_DIR}/plugin.xml)
    CONFIGURE_FILE( ${GrxUI_ON_ECLIPSE_DIR}/grxuirc.xml.in ${GrxUI_ON_ECLIPSE_DIR}/grxuirc.xml)
    CONFIGURE_FILE( ${GrxUI_ON_ECLIPSE_DIR}/grxuirc_win.xml.in ${GrxUI_ON_ECLIPSE_DIR}/grxuirc_win.xml)
    CONFIGURE_FILE( ${GrxUI_ON_ECLIPSE_DIR}/build.properties.in ${GrxUI_ON_ECLIPSE_DIR}/build.properties)
endmacro()



if( ${OPENRTM_VERSION} STREQUAL ${OPENRTM_VERSION042} )
  set(rtm_stub_jar OpenRTM-aist-0.4.1.jar)
  set(OPENRTM_VERSION_NUM "042")
  set(ADDITIONAL_EXCLUDING "|com/generalrobotix/ui/depends/rtm/ClockGeneratorRtm100_impl.java")
  set(GRX_PATH_PLANNING_ALGORITHM_ITEM "<item class=\"com.generalrobotix.ui.item.GrxPathPlanningAlgorithmItem\"/>" )

  set(GRX_PATH_PLANNING_VIEW_PART "
      <view
            class=\"com.generalrobotix.ui.view.GrxPathPlanningViewPart\"
            id=\"com.generalrobotix.ui.view.GrxPathPlanningViewPart\"
            category=\"com.generalrobotix.ui.view\"
            name=\"%PathPlanningView.name\">
      </view>" )

  set(OPEN_EXPORT_PACKAGE "jp.go.aist.rtm,
 jp.go.aist.rtm.RTC,
 jp.go.aist.rtm.RTC.SDOPackage,
 jp.go.aist.rtm.RTC.buffer,
 jp.go.aist.rtm.RTC.executionContext,
 jp.go.aist.rtm.RTC.log,
 jp.go.aist.rtm.RTC.port,
 jp.go.aist.rtm.RTC.port.publisher,
 jp.go.aist.rtm.RTC.util" )

 configure_grxui_on_eclipse()
elseif(${OPENRTM_VERSION} STREQUAL ${OPENRTM_VERSION100})
  set(rtm_stub_jar openrtmstubskel.jar)
  set(OPENRTM_VERSION_NUM "100")
  set(ADDITIONAL_EXCLUDING "|com/generalrobotix/ui/view/GrxPathPlanning*.java|com/generalrobotix/ui/depends/rtm/ClockGeneratorRtm042_impl.java|jp/**/*.java")
  set(GRX_PATH_PLANNING_ALGORITHM_ITEM "" )
  set(GRX_PATH_PLANNING_VIEW_PART "" )
  set(OPEN_EXPORT_PACKAGE "RTM,
 jp.go.aist.openrtm.OpenRTM,
 jp.go.aist.openrtm.RTC" )

  add_subdirectory(rtmStubCorba)
  message(STATUS "OpenRTM version is ${OPENRTM_VERSION}. Generate Makefile compiles openrtmstubskel.jar.")
  configure_grxui_on_eclipse()

else()
  set(rtm_stub_jar openrtmstubskel.jar)
  set(OPENRTM_VERSION_NUM "100")
  set(ADDITIONAL_EXCLUDING "|com/generalrobotix/ui/view/GrxPathPlanning*.java|com/generalrobotix/ui/depends/rtm/ClockGeneratorRtm042_impl.java|jp/**/*.java")
  set(GRX_PATH_PLANNING_ALGORITHM_ITEM "" )
  set(GRX_PATH_PLANNING_VIEW_PART "" )
  set(OPEN_EXPORT_PACKAGE "RTM,
 jp.go.aist.openrtm.OpenRTM,
 jp.go.aist.openrtm.RTC" )

  add_subdirectory(rtmStubCorba)
  message(STATUS "OpenRTM version is ${OPENRTM_VERSION}. Generate Makefile compiles openrtmstubskel.jar.")

  configure_grxui_on_eclipse()
endif()


ADD_SUBDIRECTORY(corba)

# install(FILES vrml97.jar DESTINATION ${RELATIVE_JAVA_INSTALL_PATH})
