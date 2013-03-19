
add_subdirectory(AistDynamicsSimulator)
add_subdirectory(ModelLoader)
add_subdirectory(CollisionDetector)

if (NOT QNXNTO)
add_subdirectory(UtDynamicsSimulator)
add_subdirectory(ODEDynamicsSimulator)
add_subdirectory(ControllerBridge)
add_subdirectory(PathPlanner)
endif(NOT QNXNTO)
