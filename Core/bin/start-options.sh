# Memory settings
#MA_JAVA_OPTS="$MA_JAVA_OPTS -Xms1600m -Xmx1600m"
#MA_JAVA_OPTS="$MA_JAVA_OPTS -Xms5g -Xmx5g"
#MA_JAVA_OPTS="$MA_JAVA_OPTS -Xms10g -Xmx10g"

# Enable ALPN for HTTP/2 support on Java versions < 9
#MA_JAVA_OPTS="$MA_JAVA_OPTS -javaagent:$MA_HOME/boot/jetty-alpn-agent.jar"

# Java version >=9 compatibility options
#MA_JAVA_OPTS="$MA_JAVA_OPTS --add-modules java.activation,java.xml.bind"

# Enable debug logging
#MA_JAVA_OPTS="$MA_JAVA_OPTS -Dlog4j.configurationFile=file:$MA_HOME/classes/debug-log4j2.xml"

# High performance logging
#MA_JAVA_OPTS="$MA_JAVA_OPTS -Dlog4j2.enableThreadlocals=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
#MA_JAVA_OPTS="$MA_JAVA_OPTS -Dlog4j.configurationFile=file:$MA_HOME/classes/high-performance-log4j2.xml"

# Pause VM and wait for debugger to attach
#MA_JAVA_OPTS="$MA_JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=8090,server=y,suspend=y"

# Enable remote Java JMX debugging
#MA_JAVA_OPTS="$MA_JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8091 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

# Garbage collection options
#MA_JAVA_OPTS="$MA_JAVA_OPTS -XX:+UseConcMarkSweepGC"
#MA_JAVA_OPTS="$MA_JAVA_OPTS -XX:+UseSerialGC"
#MA_JAVA_OPTS="$MA_JAVA_OPTS -XX:+UseParallelGC"
#MA_JAVA_OPTS="$MA_JAVA_OPTS -XX:+UseParallelOldGC"
#MA_JAVA_OPTS="$MA_JAVA_OPTS -XX:ParallelGCThreads=4"
#MA_JAVA_OPTS="$MA_JAVA_OPTS -verbose:gc -XX:+PrintGCDetails"

# Out of memory handling, Mango leverages in memory caches and as such any OOM error is considered fatal
#  and all possible attempts will be made to terminate the JVM gracefully from within Mango.  These options should be enabled
#  for all production systems.  Ensure your JVM supports them.
#-XX:+ExitOnOutOfMemoryError
#-XX:+HeapDumpOnOutOfMemoryError
#-XX:HeapDumpPath=<pathname>
