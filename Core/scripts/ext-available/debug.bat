if "%1" == "init" (
	set JPDA=-agentlib:jdwp=transport=dt_socket,address=8090,server=y,suspend=n
)