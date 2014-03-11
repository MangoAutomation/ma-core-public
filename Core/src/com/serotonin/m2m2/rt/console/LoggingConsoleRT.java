/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

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
		
		//Init with at least 1 message
		TranslatableMessage m = new TranslatableMessage("startup.startingUp");
		this.console.add(m.translate(Translations.getTranslations()));
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
