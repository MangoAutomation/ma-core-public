if "%1" == "init" (
        rem Add required java modules not inherently loaded in Java 9, comma separated
		set JAVAOPTS=%JAVAOPTS% --add-modules java.activation,java.xml.bind
)
