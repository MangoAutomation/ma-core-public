@echo off
rem
rem | Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
rem | @author Matthew Lohbihler
rem
rem | Runs Mango Automation.

rem | Check if MA_HOME is properly defined
if "%MA_HOME%" == "" goto useCD
if exist "%MA_HOME%\ma-start.bat" goto okHome
echo The MA_HOME environment variable is not defined correctly: %MA_HOME%. Trying the current directory instead...

rem | Check if the current directory is ok to use
:useCD
set CURRENT_DIR=%CD%
set MA_HOME=%CURRENT_DIR%
if exist "%MA_HOME%\ma-start.bat" goto okHome

rem | Don't know where home is.
echo Cannot determine the MA home directory
goto end

rem | Found a good home. Carry on...
:okHome
echo Using %MA_HOME% as MA_HOME

rem Uncomment the following line to start with the debugger
rem set JPDA=-agentlib:jdwp=transport=dt_socket,address=8090,server=y,suspend=y

SETLOCAL ENABLEDELAYEDEXPANSION
set MA_CP=%MA_HOME%\overrides\classes
set MA_CP=%MA_CP%;%MA_HOME%\classes
set MA_CP=%MA_CP%;%MA_HOME%\overrides\properties
FOR /F %%A IN ('dir /b "%MA_HOME%\overrides\lib\*.jar"') DO set MA_CP=!MA_CP!;%MA_HOME%\overrides\lib\%%A
FOR /F %%A IN ('dir /b "%MA_HOME%\lib\*.jar"') DO set MA_CP=!MA_CP!;%MA_HOME%\lib\%%A
rem echo %MA_CP%

rem | Native libraries can be put into the overrides directory
PATH=%PATH%;%MA_HOME%\overrides

set EXECJAVA=java
if "%JAVA_HOME%" == "" goto gotJava
set EXECJAVA=%JAVA_HOME%\bin\java

:gotJava
echo Using Java at %EXECJAVA%

:restart
"%EXECJAVA%" %JPDA% -server -Dma.home="%MA_HOME%" -cp "%MA_CP%" com.serotonin.m2m2.Main
if exist "%MA_HOME%\RESTART" goto restart

:end
