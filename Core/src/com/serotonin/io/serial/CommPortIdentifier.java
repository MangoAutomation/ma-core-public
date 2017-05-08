/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.io.serial;

/**
 * @author Terry Packer
 *
 */
public class CommPortIdentifier {
	
	public static final int PORT_SERIAL = 0;
	public static final int PORT_PARALLEL = 1;
	
	private String name; 
	private int portType;
	private boolean currentlyOwned = false;
	private String currentOwner = "";
	
	public CommPortIdentifier(String name, boolean isComm){
		
		this.name = name;
		if(isComm)
			this.portType = PORT_SERIAL;
		else
			this.portType = PORT_PARALLEL;
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPortType() {
		return portType;
	}

	public void setPortType(int portType) {
		this.portType = portType;
	}

	public boolean isCurrentlyOwned() {
		return currentlyOwned;
	}

	public void setCurrentlyOwned(boolean currentlyOwned) {
		this.currentlyOwned = currentlyOwned;
	}

	public String getCurrentOwner() {
		return currentOwner;
	}

	public void setCurrentOwner(String currentOwner) {
		this.currentOwner = currentOwner;
	}

}
