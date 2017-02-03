/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt.console;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Log Appender to capture log information for output to the Frontend
 * 
 * @author Terry Packer
 */
@Plugin(name = "MangoFrontendAppender", category = "Core", elementType = "appender", printObject = true)
public class MangoFrontendAppender extends AbstractAppender {

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Lock readLock = rwLock.readLock();
	
	/**
	 * @param name
	 * @param filter
	 * @param layout
	 */
	protected MangoFrontendAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
	}

	@PluginFactory
    public static MangoFrontendAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        if (name == null) {
            LOGGER.error("No name provided for MangoFrontendAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new MangoFrontendAppender(name, filter, layout, true);
    }
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.logging.log4j.core.Appender#append(org.apache.logging.log4j.
	 * core.LogEvent)
	 */
	@Override
	public void append(LogEvent event) {
		readLock.lock();
		try{
			final byte[] bytes = getLayout().toByteArray(event);
			LoggingConsoleRT.instance.addMessage(new String(bytes));
		}catch(Exception e){
			if(!ignoreExceptions())
				throw new AppenderLoggingException(e);
		}finally{
			readLock.unlock();
		}
	}

}
