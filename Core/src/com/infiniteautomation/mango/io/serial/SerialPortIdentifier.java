/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

/**
 * @author Terry Packer
 *
 */
public class SerialPortIdentifier {
	
	private String name; 
	private int type;

	private String currentOwner = "";
	private SerialPortProxy port;
	
	public SerialPortIdentifier(String name, int type){
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public int getType(){
		return this.type;
	}

	public String getCurrentOwner() {
		return currentOwner;
	}

	public void setCurrentOwner(String currentOwner) {
		this.currentOwner = currentOwner;
	}

	public SerialPortProxy getPort() {
		return port;
	}

	public void setPort(SerialPortProxy port) {
		this.port = port;
	}
	
	@Override
	public String toString(){
		String commPortId = "";
		if(port != null)
			commPortId = port.getCommPortId();
		return commPortId + " - " + name;
	}
}
