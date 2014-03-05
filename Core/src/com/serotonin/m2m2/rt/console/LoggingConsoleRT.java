/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * A Queue of log messages.
 * 
 * 
 * 
 * @author Terry Packer
 *
 */
public class LoggingConsoleRT {

	public static final LoggingConsoleRT instance = new LoggingConsoleRT();
	private int historySize = 400; //Max size of history
	private LinkedList<String> console;
	
	private LoggingConsoleRT(){
		this.console = new LinkedList<String>();
	}
	
	/**
	 * Get the most recent message
	 * @return
	 */
	public String getCurrentMessage(){
		return this.console.peek();
	}
	
	/**
	 * Get all of them
	 * @return
	 */
	public List<String> getAllMessages(){
		List<String> messages = new ArrayList<String>();
		Iterator<String> it = this.console.iterator();
		while(it.hasNext())
			messages.add(it.next());
		return messages;
	}
	
	/**
	 * Add a message to the history, 
	 * remove the oldest message if
	 *  the list is longer than MAX_SIZE
	 * @param message
	 */
	public void addMessage(String message){
		if(this.console.size() >= historySize){
			this.console.removeLast(); //Drop it
		}
		console.push(message);
	}
}
