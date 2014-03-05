/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

import org.apache.log4j.Layout;
import org.apache.log4j.WriterAppender;

/**
 * @author Terry Packer
 *
 */
public class LoggingConsoleAppender extends WriterAppender{
		
	private volatile LoggingConsoleWriter consoleWriter;
	
	public LoggingConsoleAppender(){
		this.consoleWriter = new LoggingConsoleWriter();
	}

	public LoggingConsoleAppender(Layout layout) {
		this.consoleWriter = new LoggingConsoleWriter();
		setLayout(layout);
		activateOptions();
	}
	
	
	public void activateOptions() {
		setWriter(consoleWriter);
		super.activateOptions();
	}
	
}
