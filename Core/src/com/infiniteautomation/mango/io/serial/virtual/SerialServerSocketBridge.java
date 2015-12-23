/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.io.serial.SerialPortException;
import com.infiniteautomation.mango.io.serial.SerialPortIdentifier;
import com.infiniteautomation.mango.io.serial.SerialPortInputStream;
import com.infiniteautomation.mango.io.serial.SerialPortOutputStream;
import com.infiniteautomation.mango.io.serial.SerialPortProxy;

/**
 * @author Terry Packer
 *
 */
public class SerialServerSocketBridge extends SerialPortProxy{

	private final static Log LOG = LogFactory.getLog(SerialServerSocketBridge.class);
	 
	private int port;
	private int timeout = 1000; //in milliseconds

	private ServerSocket serverSocket;
	private Socket socket;
	private SerialServerSocketThread serverThread;
	
	
	/**
	 * 
	 * @param id
	 * @param address
	 * @param port
	 * @param timeout (in ms)
	 */
	public SerialServerSocketBridge(SerialPortIdentifier id, int port, int timeout) {
		super(id);
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
		if(this.serverThread != null)
			this.serverThread.shutdown();
		else{
			Exception ex = null;
			try {
				this.serverSocket.close();
			} catch (IOException e) {
				ex = e;
				LOG.error(e.getMessage(), e);
			}
			if(this.socket != null){
				try {
					this.socket.close();
				} catch (IOException e) {
					ex = e;
					LOG.error(e.getMessage(), e);
				}
			}
			if(ex != null)
				throw new SerialPortException(ex);
		}
	}

	@Override
	public void openImpl() throws SerialPortException {
		try {
			if(this.serverThread != null)
				throw new SerialPortException("Already Open.");
			this.serverSocket = new ServerSocket(this.port);
			this.serverSocket.setSoTimeout(timeout);
			this.serverThread = new SerialServerSocketThread(this);
			this.serverThread.start();
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
	
	class SerialServerSocketThread extends Thread{
		
		private SerialServerSocketBridge bridge;
		private volatile boolean running = false;
		
		public SerialServerSocketThread(SerialServerSocketBridge bridge){
			super("Serial Server Socket Thread: " + bridge.getCommPortId());
			
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try{
				while(running){
					try{
						//Only allow first connection
						Socket socket = this.bridge.serverSocket.accept();
						if(this.bridge.socket != null)
							this.bridge.socket = socket;
					}catch(Exception e){
						LOG.error(e.getMessage(), e);
					}
				}
			}finally{
				this.bridge.serverThread = null;
				try {
					this.bridge.serverSocket.close();
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
				if(this.bridge.socket != null){
					try {
						this.bridge.socket.close();
					} catch (IOException e) {
						LOG.error(e.getMessage(), e);
					}
				}
			}
		}
		
		public void shutdown(){
			this.running = false;
		}
	}
	
}
