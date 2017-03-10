if "%1" == "init" (
        rem Concurrent Garbage Collection
        rem Medium to large size data sets and multi cpu systems where response time is more important
        rem than overall throughput.  The techniques used to imimize pauses can reduce application
        rem performance.
		set JAVAOPTS=%JAVAOPTS% -XX:+UseConcMarkSweepGC -verbose:gc -XX:+PrintGCDetails
        rem -verbose:gc -XX:+PrintGCDetails = Print the details from the collector's runs
        rem remove this if the output is not desired
)
