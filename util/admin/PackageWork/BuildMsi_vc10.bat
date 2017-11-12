@echo off
setlocal

set VERSION_STRING=3.1.4

rem �f�B���N�g���֘A
set SRC_DIR=src
set PACK_SRC_DIR=pack_src
set INSTALL_DIR=pack_src\OpenHRP
set PRODUCT_DIR=pack_src\GrxUI
set PACKAGE_DIR=package-build
set SDK_INSTALL_DIR=C:\Program Files\OpenHRPSDK

rem SVN�֘A
rem set IS_PLAIN_SVN=1
set TORTOISESVN_COMMAND=C:\Program Files\TortoiseSVN\bin\TortoiseProc.exe
set SVN_URL=https://openrtp.jp/svn/hrg/openhrp/3.1/tags/3.1.4
set SVN_USERID=
set SVN_PASSWORD=
set SVN_REVISION=HEAD

rem VC�֘A
set VC_COMMAND=C:\Program Files\Microsoft Visual Studio 10.0\Common7\IDE\VCExpress.exe
rem set VC_COMMAND=C:/Program Files/Microsoft Visual Studio 9.0/Common7/IDE/devenv.exe

rem ECLIPSE LAUNCHER
rem set ECLIPSE_LAUNCHER="C:\Users\admin\eclipse_rtm1.1\eclipse\plugins\org.eclipse.equinox.launcher_1.0.101.R34x_v20081125.jar"

rem Eclipse���i�֘A(��{�I�ɂ͐ݒ�s�v)
set ECLIPSE_ANT_XML=product.xml
set SRC_UI_GRXUI_DIR=%SRC_DIR%\GrxUIonEclipse-project-0.9.8\src\com\generalrobotix\ui\grxui
set SRC_INITIALIZER_WINRCP=%SRC_UI_GRXUI_DIR%\PreferenceInitializer.java.winrcp
set SRC_INITIALIZER_ORIGIN=%SRC_UI_GRXUI_DIR%\PreferenceInitializer.java
set SRC_INITIALIZER_TMP=%SRC_UI_GRXUI_DIR%\PreferenceInitializer.java.tmp
set ECLIPSE_WORKSPACE=workspace
set PRODUCT_PLUGIN_NAME=com.generalrobotix.ui.grxui_0.9.8.jar
set PRODUCT_PLUGIN_PATH=GrxUI\plugins\%PRODUCT_PLUGIN_NAME%
set PRODUCT_DIRNAME=GrxUI
set PRODUCT_ZIP=GrxUI-win32.win32.x86.zip

set BASE_DIR=%CD%

if "%1"=="" (
call :AllStart
goto :EndBatch
)

if /i "%1"=="svnexport" (
call :SvnExport
goto :EndBatch
)

if /i "%1"=="cmake" (
call :CMake
goto :EndBatch
)

if /i "%1"=="build" (
call :Build
goto :EndBatch
)

if /i "%1"=="CopyFile" (
call :CopyFile
goto :EndBatch
)

if /i "%1"=="plugin" (
call :Plugin
goto :EndBatch
)

if /i "%1"=="package" (
call :Package
goto :EndBatch
)

if /i "%1"=="MakeMsi" (
call :MakeMsi
goto :EndBatch
)

if /i "%1"=="product" (
call :Product
goto :EndBatch
)

if /i "%1"=="clean" (
call :Clean
goto :EndBatch
)

echo Usage: OpenHRP-Build [Option]
echo Option List
echo svnexport Subversion����\�[�X�̃G�N�X�|�[�g
echo cmake     �\�����[�V�����t�@�C���쐬
echo build     VC�ɂ��r���h�EOpenHRP�̃C���X�g�[��
echo CopyFile
echo plugin    Eclipse�v���O�C���쐬
echo product   Eclipse���i�쐬
echo package   msi�t�@�C���쐬
echo clean     �쐬�t�@�C���폜

goto :EndBatch

:AllStart
call :SvnExport
if errorlevel 1 goto :EOF

call :CMake
if errorlevel 1 goto :EOF

call :Build
if errorlevel 1 goto :EOF

call :Plugin
if errorlevel 1 goto :EOF

call :Product
if errorlevel 1 goto :EOF

call :Package
if errorlevel 1 goto :EOF

goto :EOF

:SvnExport
echo Subversion����\�[�X�̃G�N�X�|�[�g���s���܂��B
call :SvnExportClean

if "%IS_PLAIN_SVN%" == "1" (
call svn export -r %SVN_REVISION% %SVN_URL% %SRC_DIR% --username %SVN_USERID% --password %SVN_PASSWORD%
) else (
call "%TORTOISESVN_COMMAND%" /command:export /path:%SRC_DIR% /fromurl:%SVN_URL% /notempfile /closeonend:1
)
goto :EOF

:CMake
if not exist %SRC_DIR% (
echo �G�N�X�|�[�g�����\�[�X�����݂��܂���B
exit /b 1
)
cd %SRC_DIR%
if defined INSTALL_DIR (
echo %BASE_DIR%\%INSTALL_DIR% �ɃC���X�g�[������\�����[�V�����t�@�C�����쐬���܂��B
call cmake -G "Visual Studio 10" -DCMAKE_INSTALL_PREFIX:PATH=%BASE_DIR%\%INSTALL_DIR% -DOMNIORB_DIR="C:\Program Files\OpenRTM-aist\vs2010\1.1\omniORB\4.1.5" -DOPENRTM_DIR="C:\Program Files\OpenRTM-aist\vs2010\1.1" -DOPENRTM_VERSION=1.1.0
rem -DJDK_DIR="C:\Program Files\Java\jdk1.6.0_32"
) else (
echo %ProgramFiles%\OpenHRP �ɃC���X�g�[������\�����[�V�����t�@�C�����쐬���܂��B
call cmake -G "Visual Studio 10"
)
if errorlevel 1 set IS_ERROR=1
cd %BASE_DIR%
exit /b %IS_ERROR%

:Build
call :BuildClean

echo VC�ɂ��r���h���s���܂��B���O�� make_debug.log �ɏo�͂���܂��B
call "%VC_COMMAND%" %SRC_DIR%\OpenHRP.sln /rebuild Debug /out make_debug.log
if errorlevel 1 (
echo VC�ɂ��r���h�Ɏ��s���܂����B
exit /b 1
)

echo OpenHRP�̃C���X�g�[�����s���܂��B���O�� install_debug.log �ɏo�͂���܂��B
call "%VC_COMMAND%" %SRC_DIR%\OpenHRP.sln /build Debug /project INSTALL.vcxproj /out install_debug.log
if errorlevel 1 (
echo VC�ɂ��r���h�Ɏ��s���܂����B
exit /b 1
)

echo VC�ɂ��r���h���s���܂��B���O�� make_release.log �ɏo�͂���܂��B
call "%VC_COMMAND%" %SRC_DIR%\OpenHRP.sln /rebuild Release /out make_release.log
if errorlevel 1 (
echo VC�ɂ��r���h�Ɏ��s���܂����B
exit /b 1
)

echo OpenHRP�̃C���X�g�[�����s���܂��B���O�� install_release.log �ɏo�͂���܂��B
call "%VC_COMMAND%" %SRC_DIR%\OpenHRP.sln /build Release /project INSTALL.vcxproj /out install_release.log
if errorlevel 1 (
echo VC�ɂ��r���h�Ɏ��s���܂����B
exit /b 1
)
goto :EOF

:CopyFile
cd %SRC_DIR%
call cmake -G "Visual Studio 10" -DCMAKE_INSTALL_PREFIX:PATH="%SDK_INSTALL_DIR%"
if errorlevel 1 set IS_ERROR=1
copy hrplib\hrpModel\config.h ..\pack_src\OpenHRP\include\OpenHRP-3.2\hrpModel\config.h
cd %BASE_DIR%
copy %SRC_INITIALIZER_ORIGIN% %SRC_INITIALIZER_TMP%
copy %SRC_INITIALIZER_WINRCP% %SRC_INITIALIZER_ORIGIN%
exit /b %IS_ERROR%


:Plugin
call :PluginClean
echo Eclipse�v���O�C���쐬���s���܂��B
call java -jar %ECLIPSE_LAUNCHER% -application org.eclipse.ant.core.antRunner -data "%ECLIPSE_WORKSPACE%" -buildfile %ECLIPSE_ANT_XML%

if not exist %PRODUCT_ZIP% (
echo Eclipse�v���O�C���쐬�Ɏ��s���܂����B
exit /b 1
)
unzip %PRODUCT_ZIP%
copy %PRODUCT_PLUGIN_PATH% .\

call :PluginTmpClean
goto :EOF

:Product
rem call :ProductClean
rem echo Eclipse�v���O�C���쐬���s���܂��B

copy %SRC_INITIALIZER_ORIGIN% %SRC_INITIALIZER_TMP%
copy %SRC_INITIALIZER_WINRCP% %SRC_INITIALIZER_ORIGIN%

rem java -jar %ECLIPSE_LAUNCHER% -application org.eclipse.ant.core.antRunner -data "%ECLIPSE_WORKSPACE%" -buildfile %ECLIPSE_ANT_XML%
rem if not exist %PRODUCT_ZIP% (
rem echo Eclipse�v���O�C���쐬�Ɏ��s���܂����B
rem exit /b 1
rem )
rem unzip %PRODUCT_ZIP%

rem move /Y %PRODUCT_DIRNAME% %PACK_SRC_DIR%
rem move /Y %SRC_INITIALIZER_TMP% %SRC_INITIALIZER_ORIGIN%
rem call :ProductTmpClean
goto :EOF

:Package
call :PackageClean
cd %PACKAGE_DIR%

echo �C���X�g�[���p�b�P�[�W�Ɋ܂܂����e�����W���܂��B
call ruby collect.rb config_vs2010.yaml
if errorlevel 1 exit /b 1
cd %BASE_DIR%
goto :EOF

:MakeMsi
cd %PACKAGE_DIR%
echo ���W�������e����XML�t�@�C�����쐬���܂��B
call ruby create_wxs.rb config_vs2010.yaml
if errorlevel 1 exit /b 1

echo WiX���g�p����XML�t�@�C�����R���p�C�����܂��B(���{��)
candle OpenHRPj.wxs -out OpenHRPj.wixobj
if errorlevel 1 exit /b 1

echo WiX���g�p����msi�t�@�C�����쐬���܂��B(���{��)
light -ext WixUIExtension -cultures:ja-jp -loc WixUI_Alt_ja-jp.wxl -out %BASE_DIR%/OpenHRPSDK%VERSION_STRING%-j_vc10.msi OpenHRPj.wixobj
if errorlevel 1 exit /b 1

echo WiX���g�p����XML�t�@�C�����R���p�C�����܂��B(�p��)
candle OpenHRPe.wxs -out OpenHRPe.wixobj
if errorlevel 1 exit /b 1

echo WiX���g�p����msi�t�@�C�����쐬���܂��B(�p��)
light -ext WixUIExtension -out %BASE_DIR%/OpenHRPSDK%VERSION_STRING%-e_vc10.msi OpenHRPe.wixobj
if errorlevel 1 exit /b 1

cd %BASE_DIR%
call :PackageTmpClean
goto :EOF

:Clean
call :SvnExportClean
call :CMakeClean
call :BuildClean
call :PluginClean
call :ProductClean
call :PackageClean
if exist %PACK_SRC_DIR% rmdir /s /q %PACK_SRC_DIR%
exit /b 0

:SvnExportClean
if exist %SRC_DIR% rmdir /s /q %SRC_DIR%
exit /b 0

:CMakeClean
exit /b 0

:BuildClean
if exist make_debug.log del make_debug.log
if exist install_debug.log del install_debug.log
if exist make_release.log del make_release.log
if exist install_release.log del install_release.log
if exist %INSTALL_DIR% rmdir /s /q %INSTALL_DIR%
exit /b 0

:PluginClean
if exist %PRODUCT_PLUGIN_NAME% del %PRODUCT_PLUGIN_NAME%
call :PluginTmpClean
exit /b 0

:PluginTmpClean
if exist %PRODUCT_ZIP% del %PRODUCT_ZIP%
if exist %ECLIPSE_WORKSPACE% rmdir /s /q %ECLIPSE_WORKSPACE%
if exist %PRODUCT_DIRNAME% rmdir /s /q %PRODUCT_DIRNAME%
exit /b 0

:ProductClean
if exist %PRODUCT_DIR% rmdir /s /q %PRODUCT_DIR%
call :ProductTmpClean
exit /b 0

:ProductTmpClean
if exist %PRODUCT_ZIP% del %PRODUCT_ZIP%
if exist %ECLIPSE_WORKSPACE% rmdir /s /q %ECLIPSE_WORKSPACE%
if exist %PRODUCT_DIRNAME% rmdir /s /q %PRODUCT_DIRNAME%
exit /b 0

:PackageClean
if exist OpenHRPSDK%VERSION_STRING%-j_vc10.msi del OpenHRPSDK%VERSION_STRING%-j_vc10.msi
if exist OpenHRPSDK%VERSION_STRING%-e_vc10.msi del OpenHRPSDK%VERSION_STRING%-e_vc10.msi
call :PackageTmpClean
exit /b 0

:PackageTmpClean
if exist %PACKAGE_DIR%\OpenHRPj.wixobj del %PACKAGE_DIR%\OpenHRPj.wxs
if exist %PACKAGE_DIR%\OpenHRPe.wixobj del %PACKAGE_DIR%\OpenHRPe.wxs
if exist %PACKAGE_DIR%\OpenHRPj.wixobj del %PACKAGE_DIR%\OpenHRPj.wixobj
if exist %PACKAGE_DIR%\OpenHRPe.wixobj del %PACKAGE_DIR%\OpenHRPe.wixobj
rem if exist %PACKAGE_DIR%\OpenHRPSDK rmdir /s /q %PACKAGE_DIR%\OpenHRPSDK
if exist OpenHRPSDK%VERSION_STRING%-j.wixpdb del OpenHRPSDK%VERSION_STRING%-j.wixpdb
if exist OpenHRPSDK%VERSION_STRING%-e.wixpdb del OpenHRPSDK%VERSION_STRING%-e.wixpdb
exit /b 0

:EndBatch
endlocal
@echo on
