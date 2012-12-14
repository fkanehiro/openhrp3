cmake_minimum_required(VERSION 2.8 FATAL_ERROR)

project(JoystickController)

set(CMAKE_ALLOW_LOOSE_LOOP_CONSTRUCTS true)
set(CMAKE_MODULE_PATH "${PROJECT_SOURCE_DIR}/cmake_modules")

# component settings
set(RTC_NAME "${PROJECT_NAME}")
set(RTC_VERSION "1.0.0")
set(RTC_VERSION_MAJOR "1")
set(RTC_VERSION_MINOR "0")
set(RTC_VERSION_PATCH "0")
set(RTC_LANG "c++")
set(RTC_DESCRIPTION "ModuleDescription")
set(RTC_VENDOR "AIST")
set(RTC_MAINTAINER "author@example.com")

set(IDL_FILES ""
)

set(SKEL_SRCS ""
)

set(STUB_SRCS ""
)

set(IMPL_SRCS ""
)

set(IMPL_INCS ""
)

set(LIBS_SRCS JoystickController.cpp)

set(LIBS_INCS JoystickController.h)

set(EXEC_SRCS JoystickControllerComp.cpp)

set(OTHER_SRCS CMakeLists.txt rtc.conf)


# check OpenRTM installed
find_package(OpenRTM REQUIRED)

list(APPEND CFLAGS ${OMNIORB_CFLAGS} ${OPENRTM_CFLAGS})
list(APPEND INCLUDE_DIRS ${OMNIORB_INCLUDE_DIRS} ${OPENRTM_INCLUDE_DIRS})
list(APPEND LDFLAGS ${OMNIORB_LDFLAGS} ${OPENRTM_LDFLAGS})
list(APPEND LIBRARY_DIRS ${OMNIORB_LIBRARY_DIRS} ${OPENRTM_LIBRARY_DIRS})
list(APPEND LIBRARIES ${OMNIORB_LIBRARIES} ${OPENRTM_LIBRARIES})


if(WIN32)
    # add command path
    set($ENV{PATH} $ENV{PATH} "${OPENRTM_DIR}/bin")
    set($ENV{PATH} $ENV{PATH} "${OMNIORB_DIR}/bin/x86_win32")
    # set install path prefix
else()
    # add command path
    set($ENV{PATH} $ENV{PATH} "${OPENRTM_DIR}/bin")
    set($ENV{PATH} $ENV{PATH} "${OMNIORB_DIR}/bin")
    # set install path prefix
    set(OPENRTM_SHARE_PREFIX "share/openrtm-${OPENRTM_VERSION_MAJOR}.${OPENRTM_VERSION_MINOR}")
endif()

# target settings for library/executable
include_directories(${INCLUDE_DIRS})

link_directories(${LIBRARY_DIRS})

add_definitions(${CFLAGS})
add_definitions(-I${CMAKE_CURRENT_BINARY_DIR})

foreach(idl ${IDL_FILES})
    string(REGEX REPLACE "([^\\/]*)\\.idl" "\\1" idl_base ${idl})
    add_custom_command(OUTPUT ${idl_base}Stub.cpp ${idl_base}Skel.cpp
                       COMMAND ${OPENRTM_IDL_WRAPPER} ${OPENRTM_IDL_WRAPPER_FLAGS} --idl-file=${idl}
                       COMMAND ${OPENRTM_IDLC} ${OPENRTM_IDLFLAGS} ${PROJECT_SOURCE_DIR}/${idl}
                       DEPENDS ${idl})
endforeach()

if(CMAKE_COMPILER_IS_GNUCC)
    string(REPLACE ";" " " LDFLAGS "${LDFLAGS}")
endif()

add_library(${RTC_NAME} SHARED
            ${LIBS_SRCS} ${IMPL_SRCS} ${SKEL_SRCS} ${STUB_SRCS})
target_link_libraries(${RTC_NAME} ${LIBRARIES})
set_target_properties(${RTC_NAME} PROPERTIES PREFIX "")
set_target_properties(${RTC_NAME} PROPERTIES LINK_FLAGS "${LDFLAGS}")

if(UNIX)
    add_library(${RTC_NAME}-static STATIC
                ${LIBS_SRCS} ${IMPL_SRCS} ${SKEL_SRCS} ${STUB_SRCS})
    target_link_libraries(${RTC_NAME}-static ${LIBRARIES})
    set_target_properties(${RTC_NAME}-static PROPERTIES PREFIX "")
    set_target_properties(${RTC_NAME}-static PROPERTIES LINK_FLAGS "${LDFLAGS}")
endif()

if(UNIX)
    add_executable(${RTC_NAME}Comp ${EXEC_SRCS})
    add_dependencies(${RTC_NAME}Comp ${RTC_NAME}-static)
    target_link_libraries(${RTC_NAME}Comp ${RTC_NAME}-static ${LIBRARIES})

    add_executable(${RTC_NAME}Comp-dynamic ${EXEC_SRCS})
    add_dependencies(${RTC_NAME}Comp-dynamic ${RTC_NAME})
    target_link_libraries(${RTC_NAME}Comp-dynamic ${RTC_NAME} ${LIBRARIES})
    set_target_properties(${RTC_NAME}Comp-dynamic PROPERTIES LINK_FLAGS "${LDFLAGS}")

else()
    add_executable(${RTC_NAME}Comp ${EXEC_SRCS})
    add_dependencies(${RTC_NAME}Comp ${RTC_NAME})
    target_link_libraries(${RTC_NAME}Comp ${RTC_NAME} ${LIBRARIES})
    set_target_properties(${RTC_NAME}Comp PROPERTIES LINK_FLAGS "${LDFLAGS}")
endif()

install(TARGETS ${RTC_NAME} LIBRARY DESTINATION "${PROJECT_SOURCE_DIR}"
                            ARCHIVE DESTINATION "${PROJECT_SOURCE_DIR}"
                            RUNTIME DESTINATION "${PROJECT_SOURCE_DIR}"
        COMPONENT "applications")
install(TARGETS ${RTC_NAME}Comp RUNTIME DESTINATION "${PROJECT_SOURCE_DIR}"
        COMPONENT "applications")
