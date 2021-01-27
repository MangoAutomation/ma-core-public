@echo off
setlocal
if exist mango.cmd (
    cd ..
)
java -cp lib\* com.serotonin.m2m2.Main
endlocal