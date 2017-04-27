@echo off
rem
rem | Copyright (C) 2006-2015 Serotonin Software Technologies Inc. All rights reserved.
rem | @author Matthew Lohbihler, Woody Beverley, Terry Packer
rem
rem | Runs Mango Automation. (Script Version 2.6.0 - To be run from MA_HOME\bin)

rem | Check if MA_HOME is properly defined
if "%MA_HOME%" == "" goto useCD
if exist "%MA_HOME%\bin\ma-start.bat" goto okHome
echo The MA_HOME environment variable is not defined correctly: %MA_HOME%. Trying the current directory instead...

rem | Check if the current directory is ok to use
:useCD
set BIN_DIR=%CD%
pushd ..
set MA_HOME=%CD%
popd
if exist "%MA_HOME%\bin\ma-start.bat" goto okHome

rem | Don't know where home is.
echo Cannot determine the MA home directory
goto end

rem | Found a good home. Carry on...
:okHome
echo Using %MA_HOME% as MA_HOME

SETLOCAL ENABLEDELAYEDEXPANSION
set MA_CP=%MA_HOME%\overrides\classes
set MA_CP=%MA_CP%;%MA_HOME%\classes
set MA_CP=%MA_CP%;%MA_HOME%\overrides\properties
set MA_CP=%MA_CP%;%MA_HOME%\overrides\lib\*
set MA_CP=%MA_CP%;%MA_HOME%\lib\*

rem FOR /F %%A IN ('dir /b "%MA_HOME%\overrides\lib\*.jar"') DO set MA_CP=!MA_CP!;%MA_HOME%\overrides\lib\%%A
rem FOR /F %%A IN ('dir /b "%MA_HOME%\lib\*.jar"') DO set MA_CP=!MA_CP!;%MA_HOME%\lib\%%A
echo %MA_CP%

rem | Native libraries can be put into the overrides directory
PATH=%PATH%;%MA_HOME%\overrides

set EXECJAVA=java
set EXECJAR=jar
if "%JAVA_HOME%" == "" goto gotJava
set EXECJAVA=%JAVA_HOME%\bin\java
set EXECJAR=%JAVA_HOME%\bin\jar

:gotJava
echo Using Java at %EXECJAVA%

rem Remove Range.class if it exists:
if exist "%MA_HOME%\classes\org\jfree\data\Range.class" (
	del "%MA_HOME%\classes\org\jfree\data\Range.class"
)

set JAVAOPTS=
set JPDA=
if exist "%MA_HOME%\bin\ext-enabled" (
	for /R "%MA_HOME%\bin\ext-enabled" %%f in (*.bat) do (
		call "%MA_HOME%\bin\ext-enabled/%%~nf" init
	)
)
:restart
if exist "%MA_HOME%"\m2m2-core-*.zip (
echo Core upgrade found...
rem Check to see we have the jar utility
"%EXECJAR%" >nul 2>&1
if errorlevel 9009 if not errorlevel 9010 (
	echo No Jar utility found to extract upgrade, Please manually unzip core upgrade and restart Mango...
	goto end
)

rem We have an upgrade, clean up old libs, extract zip and upgrade
echo Cleaning lib folder...
del /q "%MA_HOME%"\lib\*.jar
echo Cleaning work folder...
del /s /q  "%MA_HOME%"\work
pushd ..
echo Extracting Core Upgrade...
"%EXECJAR%" xf "%MA_HOME%"\m2m2-core-*.zip
del /q "%MA_HOME%"\m2m2-core-*.zip
popd
rem Run the upgrade script that will finish by calling this script to start Mango
upgrade.bat
)
if exist "%MA_HOME%\bin\ext-enabled" (
	for /R "%MA_HOME%\bin\ext-enabled" %%f in (*.bat) do (
		call "%MA_HOME%\bin\ext-enabled/%%~nf" start
	)
)
"%EXECJAVA%" %JPDA% %JAVAOPTS% -server -Dma.home="%MA_HOME%" -cp "%MA_CP%" com.serotonin.m2m2.Main
if exist "%MA_HOME%\RESTART" (
	if exist "%MA_HOME%\bin\ext-enabled" (
		for /R "%MA_HOME%\bin\ext-enabled" %%f in (*.bat) do (
			call "%MA_HOME%\bin\ext-enabled/%%~nf" restart
		)
	)
	goto restart
)
:end
