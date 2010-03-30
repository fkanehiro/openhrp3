@echo off
setlocal

rem set INSTALL_DIR=%ProgramFiles%\OpenHRP
set VC_PLATFORM=Visual Studio 9 2008
set VC_COMMAND=C:/Program Files/Microsoft Visual Studio 9.0/Common7/IDE/VCExpress.exe
set ECLIPSE_LAUNCHER=C:\eclipse\plugins\org.eclipse.equinox.launcher_1.0.101.R34x_v20081125.jar

if "%1"=="" (
call :AllStart
goto :EndBatch
)

if /i "%1"=="cmake" (
call :CMake
goto :EndBatch
)

if /i "%1"=="debug-build-install" (
call :DebugBuild
goto :EndBatch
)

if /i "%1"=="build-install" (
call :Build
goto :EndBatch
)

if /i "%1"=="build-plugin" (
call :Plugin
goto :EndBatch
)

echo "Usage: BuildInstall [Option]"
echo "Option List:"
echo "  cmake"
echo "  build-install"
echo "  debug-build-install"
echo "  build-plugin"
goto :EndBatch

:AllStart
call :CMake
if errorlevel 1 goto :EOF

call :Build
if errorlevel 1 goto :EOF

call :Plugin
if errorlevel 1 goto :EOF
goto :EOF

:CMake
if defined INSTALL_DIR (
echo Output solution file. It install in %INSTALL_DIR%
call cmake -G "%VC_PLATFORM%" -DCMAKE_INSTALL_PREFIX:PATH=%INSTALL_DIR%
) else (
echo Output solution file. It install in %ProgramFiles%\OpenHRP
call cmake -G "%VC_PLATFORM%"
)
goto :EOF

:Build
if not exist OpenHRP.sln (
echo Not Found OpenHRP.sln. Please "BuildInstall cmake".
exit /b 1
)
echo Build solution. Log file is make_release.log
call "%VC_COMMAND%" OpenHRP.sln /rebuild Release /out make_release.log
echo Install OpenHRP. Log file is install_release.log
call "%VC_COMMAND%" OpenHRP.sln /build Release /project INSTALL.vcproj /out install_release.log
goto :EOF

:DebugBuild
if not exist OpenHRP.sln (
echo Not Found OpenHRP.sln. Please "BuildInstall cmake".
exit /b 1
)
echo Build debug solution. Log file is make_debug.log
call "%VC_COMMAND%" OpenHRP.sln /rebuild Debug /out make_debug.log
echo Install OpenHRP. Log file is install_debug.log
call "%VC_COMMAND%" OpenHRP.sln /build Debug /project INSTALL.vcproj /out install_debug.log
goto :EOF

:Plugin
if not exist bin\Release\openhrp-controller-bridge.exe (
echo not build OpenHRP. Please "BuildInstall Build".
exit /b 1
)
echo Build eclipse pliugin.
call java -jar %ECLIPSE_LAUNCHER% -application org.eclipse.ant.core.antRunner -data "workspace" -buildfile BuildPlugin.xml
if exist workspace rmdir /s /q workspace
goto :EOF

:EndBatch
endlocal
@echo on
