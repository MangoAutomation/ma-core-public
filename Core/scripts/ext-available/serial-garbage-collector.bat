if "%1" == "init" (
        rem Specifically use the Serial Garbage Collector (Default)
        rem Designed for use with small data sets up to 100MB and single processor machines
        
		rem -verbose:gc -XX:+PrintGCDetails Also print the details from the collector's runs
        rem    Remove this if the output is not desired
		
		set JAVAOPTS=%JAVAOPTS% -XX:+UseSerialGC -verbose:gc -XX:+PrintGCDetails
)
