
# @author Shin'ichiro Nakaoka

set(sources_short
  
  GrxBaseController.java
  GrxBaseItem.java
  GrxBasePlugin.java
  GrxBaseView.java
  GrxPluginManager.java
  GrxTimeSeriesItem.java
  GrxUI.java
  GrxUIFrame.java

  item/GrxCollisionPairItem.java
  item/GrxGraphItem.java
  item/GrxModeInfoItem.java
  item/GrxModelItem.java
  item/GrxProjectItem.java
  item/GrxPythonScriptItem.java
  item/GrxTextItem.java
  item/GrxWorldStateItem.java

  util/AlertBox.java
  util/ComboBoxDialog.java
  util/ConfirmDialog.java
  util/ErrorDialog.java
  util/FileInput.java
  util/FileNameLocationDialog.java
  util/GrxConfigBundle.java
  util/GrxConfigPane.java
  util/GrxCorbaUtil.java
  util/GrxDebugUtil.java
  util/GrxGuiUtil.java
  util/GrxORBMonitor.java
  util/GrxPluginLoader.java
  util/GrxProcessManager.java
  util/GrxProcessingWindow.java
  util/GrxXmlUtil.java
  util/IconProperties.java
  util/ItemPropertyDoubleSpin.java
  util/JTextAreaEx.java
  util/MessageBundle.java
  util/MessageDialog.java
  util/ModalDialog.java
  util/OrderedHashMap.java
  util/TableSorter.java
  util/WarningDialog.java

  view/Grx3DView.java
  view/GrxDutyMonitorView.java
  view/GrxGraphView.java
  view/GrxItemView.java
  view/GrxJythonPromptView.java
  view/GrxLoggerView.java
  view/GrxModelTreeView.java
  view/GrxNullView.java
  view/GrxORBMonitorView.java
  view/GrxOpenHRPView.java
  view/GrxProcessManagerView.java
  view/GrxPropertyView.java
  view/GrxRobotStatView.java
  view/GrxTextEditorView.java
  view/MenuDialog.java

  view/graph/Attribute.java
  view/graph/AttributeInfo.java
  view/graph/AttributeProperties.java
  view/graph/AxisInfo.java
  view/graph/DataItem.java
  view/graph/DataItemInfo.java
  view/graph/DataKind.java
  view/graph/DataModel.java
  view/graph/DataSeries.java
  view/graph/DroppableXYGraph.java
  view/graph/EPSDialog.java
  view/graph/EPSGraphics.java
  view/graph/FileOpenFailException.java
  view/graph/GUIStatus.java
  view/graph/GraphElement.java
  view/graph/GraphPanel.java
  view/graph/GraphProperties.java
  view/graph/HRangeDialog.java
  view/graph/InvalidClassException.java
  view/graph/LogFileFormatException.java
  view/graph/LogFileOutputException.java
  view/graph/LogManager.java
  view/graph/SEAxisAngle.java
  view/graph/SEBoolean.java
  view/graph/SEDouble.java
  view/graph/SEDoubleArray.java
  view/graph/SEDoubleTextWithSpin.java
  view/graph/SEEnumeration.java
  view/graph/SEIntArray.java
  view/graph/SEInteger.java
  view/graph/SEString.java
  view/graph/SETranslation.java
  view/graph/SeriesDialog.java
  view/graph/SimulationTime.java
  view/graph/SpinControl.java
  view/graph/SpinListener.java
  view/graph/StringExchangeException.java
  view/graph/StringExchangeable.java
  view/graph/Time.java
  view/graph/TrendGraph.java
  view/graph/TrendGraphManager.java
  view/graph/TrendGraphModel.java
  view/graph/VRangeDialog.java
  view/graph/XYLineGraph.java

  view/simulation/CollisionPairPanel.java
  view/simulation/ControllerPanel.java
  view/simulation/SimulationParameterPanel.java
  
  view/tdview/BadLinkStructureException.java
  view/tdview/BehaviorHandler.java
  view/tdview/BehaviorInfo.java
  view/tdview/BehaviorManager.java
  view/tdview/ExampleFileFilter.java
  view/tdview/FittingInfo.java
  view/tdview/GUIAction.java
  view/tdview/ImageToMovie.java
  view/tdview/InvKinemaChangeListener.java
  view/tdview/InvKinemaHandler.java
  view/tdview/InvKinemaResolver.java
  view/tdview/IseBehavior.java
  view/tdview/IseBehaviorHandler.java
  view/tdview/JointRotationHandler.java
  view/tdview/Manipulatable.java
  view/tdview/MyBufferToImage.java
  view/tdview/ObjectFittingHandler.java
  view/tdview/ObjectRotationHandler.java
  view/tdview/ObjectToolBar.java
  view/tdview/ObjectTranslationHandler.java
  view/tdview/OperationHandler.java
  view/tdview/PopButtonGroup.java
  view/tdview/ProjectErrorHandler.java
  view/tdview/RecordingDialog.java
  view/tdview/RecordingManager.java
  view/tdview/ResizableCanvas.java
  view/tdview/RoomViewHandler.java
  view/tdview/SceneGraphModifier.java
  view/tdview/SimpleMoviePlayer.java
  view/tdview/ThreeDDrawable.java
  view/tdview/TransformChangeEvent.java
  view/tdview/TraverseOperation.java
  view/tdview/ViewChangeEvent.java
  view/tdview/ViewHandler.java
  view/tdview/ViewInfo.java
  view/tdview/ViewModeChangeListener.java
  view/tdview/ViewModeChangeMulticaster.java
  view/tdview/ViewToolBar.java
  view/tdview/WalkViewHandler.java
  view/tdview/WorldReplaceListener.java

  view/vsensor/Camera_impl.java
  view/vsensor/Canvas3DI.java
  view/vsensor/ColorBufferBrowser.java
  view/vsensor/OffScreenCanvas3D.java
  view/vsensor/OnScreenCanvas3D.java
  )

set(sources)
foreach(source ${sources_short})
  list(APPEND sources com/generalrobotix/ui/${source})
endforeach()

set(classpath
  "${CLASSPATH}:${OpenHRP_BINARY_DIR}/java/openhrpstubskel.jar:${OpenHRP_BINARY_DIR}/java/vrml97.jar")

set(javac_flags -target 1.5 -encoding EUCJIS -classpath ${classpath} -d classes)

if(CMAKE_BUILD_TYPE STREQUAL Debug OR CMAKE_BUILD_TYPE STREQUAL RelWithDebInfo)
  list(APPEND javac_flags -g)
endif()

set(grxui_jar ${OpenHRP_BINARY_DIR}/java/grxui.jar)

add_custom_command(
  OUTPUT  ${grxui_jar} classes
  COMMAND ${CMAKE_COMMAND} -E make_directory classes
  COMMAND ${JAVAC} ${javac_flags} ${sources}
  COMMAND ${JAR} cmf Manifest.mf ${grxui_jar} resources -C classes com
  DEPENDS ${sources}
)


add_custom_target(GrxUI ALL DEPENDS ${grxui_jar})

add_dependencies(GrxUI JavaSutbSkel)

set_directory_properties(PROPERTIES ADDITIONAL_MAKE_CLEAN_FILES classes)

set(grxui_rc_files
  grxuirc.xml
)

install(FILES ${grxui_jar} DESTINATION ${RELATIVE_GRXUI_INSTALL_PATH})
install(FILES ${grxui_rc_files} DESTINATION ${RELATIVE_GRXUI_INSTALL_PATH})

file(GLOB grxui_script_files script/*.py)
install(FILES ${grxui_script_files} DESTINATION ${RELATIVE_GRXUI_INSTALL_PATH}/script)

install(DIRECTORY resources
  DESTINATION ${RELATIVE_GRXUI_INSTALL_PATH}
  PATTERN ".svn" EXCLUDE)

install(DIRECTORY DESTINATION ${RELATIVE_GRXUI_INSTALL_PATH}/plugin)
install(DIRECTORY DESTINATION ${RELATIVE_GRXUI_INSTALL_PATH}/log)
