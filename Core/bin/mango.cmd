@echo off
setlocal
if exist mango.cmd (
    cd ..
)
java -jar boot\ma-bootstrap.jar
endlocal
