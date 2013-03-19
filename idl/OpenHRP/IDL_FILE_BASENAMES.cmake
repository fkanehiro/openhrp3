
set(IDL_FILE_BASENAMES
  OpenHRPCommon
  World
  ModelLoader
  CollisionDetector
  DynamicsSimulator
  Controller
  ViewSimulator
  OnlineViewer
  ClockGenerator
)

if (NOT QNXNTO)
set(IDL_FILE_BASENAMES ${IDL_FILE_BASENAMES} PathPlanner)
endif (NOT QNXNTO)
