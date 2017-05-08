package com.serotonin.util;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;

import com.serotonin.timer.TimerTask;
import com.serotonin.timer.TimerTrigger;

abstract public class LoggingConfigRefreshJob extends TimerTask {
    public static final String LOG_FILE = "log4j.xml";

    private final Log LOG = LogFactory.getLog(LoggingConfigRefreshJob.class);

    private long lastUpdate = -1;

    public LoggingConfigRefreshJob(TimerTrigger trigger) {
        super(trigger, "LoggingConfigRefreshJob");
    }

    @Override
    public void run(long runtime) {
        if (LOG.isDebugEnabled())
            LOG.debug("Running LoggingConfigRefreshJob");

        File f = getConfigFile();
        if (f != null && f.lastModified() != lastUpdate) {
            lastUpdate = f.lastModified();

            LOG.info("Updating log configuration");
            try {
                DOMConfigurator.configure(f.toURI().toURL());
            }
            catch (Exception e) {
                LOG.error("Error during log configuration update", e);
                // If the update didn't work, we might not have any logging, so dump to console as well.
                e.printStackTrace();
            }
        }
    }

    abstract protected File getConfigFile();
}
