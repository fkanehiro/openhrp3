# Block multiple inclusion
IF(__GET_WIN_SYSTEM_PREFIX_PATHS_INCLUDED)
  RETURN()
ENDIF()
SET(__GET_WIN_SYSTEM_PREFIX_PATHS_INCLUDED 1)

IF(DEFINED "ENV{ProgramW6432}")
  # 32-bit binary on 64-bit windows.
  # The 64-bit program files are in ProgramW6432.
  LIST(APPEND WIN_SYSTEM_PREFIX_PATHS "$ENV{ProgramW6432}")

  # The 32-bit program files are in ProgramFiles.
  IF(DEFINED "ENV{ProgramFiles}")
    LIST(APPEND WIN_SYSTEM_PREFIX_PATHS "$ENV{ProgramFiles}")
  ENDIF()
ELSE()
  # 64-bit binary, or 32-bit binary on 32-bit windows.
  IF(DEFINED "ENV{ProgramFiles}")
    LIST(APPEND WIN_SYSTEM_PREFIX_PATHS "$ENV{ProgramFiles}")
  ENDIF()
  IF(DEFINED "ENV{ProgramFiles(x86)}")
    # 64-bit binary.  32-bit program files are in ProgramFiles(x86).
    LIST(APPEND WIN_SYSTEM_PREFIX_PATHS "$ENV{ProgramFiles(x86)}")
  ELSEIF(DEFINED "ENV{SystemDrive}")
    # Guess the 32-bit program files location.
    IF(EXISTS "$ENV{SystemDrive}/Program Files (x86)")
      LIST(APPEND WIN_SYSTEM_PREFIX_PATHS
        "$ENV{SystemDrive}/Program Files (x86)")
    ENDIF()
  ENDIF()
ENDIF()
LIST(APPEND WIN_SYSTEM_PREFIX_PATHS "$ENV{HOMEDRIVE}")
