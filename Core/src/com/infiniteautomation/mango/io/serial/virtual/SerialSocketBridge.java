/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.io.serial.SerialPortException;
import com.infiniteautomation.mango.io.serial.SerialPortIdentifier;
import com.infiniteautomation.mango.io.serial.SerialPortInputStream;
import com.infiniteautomation.mango.io.serial.SerialPortOutputStream;
import com.infiniteautomation.mango.io.serial.SerialPortProxy;

/**
 * Class to connect a serial port to a Client Socket/Inet Address
 * 
 * @author tpacker
 *
 */
public class SerialSocketBridge extends SerialPortProxy{

	private final static Logger LOG = LoggerFactory.getLogger(SerialSocketBridge.class);
	 
	private String address;
	private int port;
	private int timeout = 1000; //in milliseconds
	
	private Socket socket;
	
	/**
	 * 
	 * @param id
	 * @param address
	 * @param port
	 * @param timeout (in ms)
	 */
	SerialSocketBridge(SerialPortIdentifier id, String address, int port, int timeout) {
		super(id);
		this.address = address;
		this.port = port;
		this.timeout = timeout;
	}

	@Override
	public byte[] readBytes(int i) throws SerialPortException {
		byte[] read = new byte[i];
		try {
			this.socket.getInputStream().read(read);
		} catch (IOException e) {
			throw new SerialPortException(e.getMessage());
		}
		return read;
	}

	@Override
	public void writeInt(int arg0) throws SerialPortException {
		
		try {
			this.socket.getOutputStream().write(arg0);
		} catch (IOException e) {
			throw new SerialPortException(e.getMessage());
		}
		
	}

	@Override
	public void closeImpl() throws SerialPortException {
		try {
			this.socket.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new SerialPortException(e.getMessage());
		}
		
	}

	@Override
	public void openImpl() throws SerialPortException {
		try {
			this.socket = new Socket(this.address, this.port);
			this.socket.setSoTimeout(timeout);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw new SerialPortException(e);
		}

		
	}

	@Override
	public SerialPortInputStream getInputStream() {
		try{
			return new SerialSocketBridgeInputStream(this.socket.getInputStream());
		}catch(IOException e){
			LOG.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public SerialPortOutputStream getOutputStream() {
		try{
			return new SerialSocketBridgeOutputStream(this.socket.getOutputStream());
		}catch(IOException e){
			LOG.error(e.getMessage(), e);
			return null;
		}
	}

	public InputStream getSocketInputStream() throws IOException{
		return this.socket.getInputStream();
	}
	
	public OutputStream getSocketOutputStream() throws IOException{
		return this.socket.getOutputStream();
	}
}
