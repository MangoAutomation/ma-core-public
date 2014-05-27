/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	private ConcurrentLinkedQueue<LogConsoleMessage> console; 
	
	private LoggingConsoleRT(){
		this.console = new ConcurrentLinkedQueue<LogConsoleMessage>();
		//Init with at least 1 message
		TranslatableMessage m = new TranslatableMessage("startup.startingUp");
		this.console.add(new LogConsoleMessage(m.translate(Translations.getTranslations()) , System.currentTimeMillis()));
	}
	
	/**
	 * Get the most recent message
	 * @return
	 */
	public String getCurrentMessage(){
		if(this.console.size() == 0)
			return "";
		else
			return this.console.peek().getMessage();
	}
	
	/**
	 * Get all of them
	 * @return
	 */
	public List<String> getAllMessages(){
		List<String> messages = new ArrayList<String>();
		Iterator<LogConsoleMessage> it = this.console.iterator();
		while(it.hasNext())
			messages.add(it.next().getMessage());
		return messages;
	}
	
	/**
	 * Add a message to the history, 
	 * remove the oldest message if
	 *  the list is longer than MAX_SIZE
	 * @param message
	 */
	public void addMessage(String message){
		while(this.console.size() >= historySize){
			this.console.poll(); //Drop it
		}
		console.add(new LogConsoleMessage(message, System.currentTimeMillis()));
	}
	
	/**
	 * Get messages since a given time, very simple 
	 * will redo when we move to better storage
	 * @param time
	 * @return
	 */
	public List<String> getMessagesSince(long time){
		
		List<String> messages = new ArrayList<String>();
		Iterator<LogConsoleMessage> it = this.console.iterator();
		while(it.hasNext()){
			LogConsoleMessage message = it.next();
			if(message.getTimestamp() >= time)
				messages.add(message.getMessage());
		}
		return messages;
		
	}
	
	public int getHistorySize(){
		return this.historySize;
	}
	
	public void setHistorySize(int size){
		this.historySize = size;
	}
	
	
}
