
# @author Shin'ichiro Nakaoka

if(NOT JYTHON)
  set(JYTHON)
  FIND_PROGRAM(JYTHON jython)
  if(NOT JYTHON)
    set(JYTHON_FOUND FALSE)
  endif()
endif()



if(JYTHON)
  set(JYTHON_FOUND TRUE)

  if(NOT JYTHON_JAR)

    execute_process(
      # "-Dpython.console= " is needed to avoid curses hang up when invoked by ccmake
      COMMAND ${JYTHON} -Dpython.console= -c "import java; print java.lang.System.getProperty(\"java.class.path\")"
      OUTPUT_VARIABLE jython_classpath
      RESULT_VARIABLE RESULT
      OUTPUT_STRIP_TRAILING_WHITESPACE)

    if(NOT RESULT EQUAL 0)
      set(JYTHON_FOUND FALSE)
    endif()
    
    if(jython_classpath)
      string(REPLACE ":" ";" jython_classpath ${jython_classpath})
      set(JYTHON_JAR)
      foreach(path ${jython_classpath})
	if(NOT JYTHON_JAR)
	  if(path MATCHES "(^|/)jython.*\\.jar")
	    set(JYTHON_JAR ${path})
	  endif()
	endif()
      endforeach()
    endif()
  endif()

  if(NOT JYTHON_HOME)
    execute_process(
      COMMAND ${JYTHON} -Dpython.console= -c "import java; print java.lang.System.getProperty(\"python.home\")"
      OUTPUT_VARIABLE JYTHON_HOME
      RESULT_VARIABLE RESULT
      OUTPUT_STRIP_TRAILING_WHITESPACE)
    
    if(NOT RESULT EQUAL 0)
      set(JYTHON_FOUND FALSE)
    endif()
  endif()

endif()

set(JYTHON ${JYTHON} CACHE FILEPATH "jython command file")
set(JYTHON_JAR ${JYTHON_JAR} CACHE FILEPATH "jython.jar file")
set(JYTHON_HOME ${JYTHON_HOME} CACHE PATH "jython home directory")

if(JYTHON_FOUND)
  if(NOT Jython_FIND_QUIETLY)
    message(STATUS "Found ${JYTHON}")
    if(JYTHON_JAR)
      message(STATUS " jar file is ${JYTHON_JAR}")
    endif()
    if(JYTHON_HOME)
      message(STATUS " home directory is ${JYTHON_HOME}")
    endif()
  endif()
else()
  if(NOT Jython_FIND_QUIETLY)
    if(Jython_FIND_REQUIRED)
      message(FATAL_ERROR "Jython required, please specify jython command path to JYTHON variable")
    endif()
  endif()
endif()
