@echo off
rem
rem | Copyright (C) 2006-2015 //infintie Automation. All rights reserved.
rem | @author Phillip Dunlap
rem
rem | Runs H2 Web Console. (Script Version 1.0.0 - To be run from MA_HOME\bin)

rem | Define the port to start H2 on...
set H2_PORT=8085

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

set EXECJAVA=java
if "%JAVA_HOME%" == "" goto gotJava
set EXECJAVA=%JAVA_HOME%\bin\java

:gotJava
echo Using Java at %EXECJAVA%

rem | Put the whole lib folder on CP because we cannot do h2*.jar...
rem | Add -webAllowOthers if you will be using the web console from an origin other than localhost
"%EXECJAVA%" -cp "%MA_HOME%\lib\*" org.h2.tools.Server -web -webPort "%H2_PORT%" -baseDir "%MA_HOME%\"

:end
