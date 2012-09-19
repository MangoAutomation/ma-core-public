@echo off
rem
rem | Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
rem | @author Matthew Lohbihler
rem
rem | Runs Mango M2M2.

rem | Check if M2M2_HOME is properly defined
if "%M2M2_HOME%" == "" goto useCD
if exist "%M2M2_HOME%\m2m2-start.bat" goto okHome
echo The M2M2_HOME environment variable is not defined correctly: %M2M2_HOME%. Trying the current directory instead...

rem | Check if the current directory is ok to use
:useCD
set CURRENT_DIR=%CD%
set M2M2_HOME=%CURRENT_DIR%
if exist "%M2M2_HOME%\m2m2-start.bat" goto okHome

rem | Don't know where home is.
echo Cannot determine the M2M2 home directory
goto end

rem | Found a good home. Carry on...
:okHome
echo Using %M2M2_HOME% as M2M2_HOME

rem Uncomment the following line to start with the debugger
rem set JPDA=-agentlib:jdwp=transport=dt_socket,address=8090,server=y,suspend=y

SETLOCAL ENABLEDELAYEDEXPANSION
set M2M2_CP=%M2M2_HOME%\overrides
set M2M2_CP=%M2M2_CP%;%M2M2_HOME%\classes
set M2M2_CP=%M2M2_CP%;%M2M2_HOME%\overrides
FOR /F %%A IN ('dir /b %M2M2_HOME%\lib\*.jar') DO set M2M2_CP=!M2M2_CP!;%M2M2_HOME%\lib\%%A
rem echo %M2M2_CP%

rem | Native libraries can be put into the overrides directory
PATH=%PATH%;%M2M2_HOME%\overrides

set EXECJAVA=java
if "%JAVA_HOME%" == "" goto gotJava
set EXECJAVA=%JAVA_HOME%\bin\java

:gotJava
echo Using Java at %EXECJAVA%

:restart
"%EXECJAVA%" %JPDA% -server -Dm2m2.home="%M2M2_HOME%" -cp "%M2M2_CP%" com.serotonin.m2m2.Main
if exist "%M2M2_HOME%\RESTART" goto restart

:end
