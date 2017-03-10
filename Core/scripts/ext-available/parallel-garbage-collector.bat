if "%1" == "init" (
	rem Parallel Garbage Collection aka Throughput collector
	rem performs minor collections in parallel.  Medium to large size
	rem data sets and multi cpu systems.
	
	rem -XX:+UseParallelOldGC Use parallel compaction, perform major collections in parallel
	rem -XX:ParallelGCThreads=4 Optionally tune the number of threads used
	rem Print the details from the collector's runs
	rem    remove this line if the output is not desired
	
	set JAVAOPTS=%JAVAOPTS% -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:ParallelGCThreads=4 -verbose:gc -XX:+PrintGCDetails
)
