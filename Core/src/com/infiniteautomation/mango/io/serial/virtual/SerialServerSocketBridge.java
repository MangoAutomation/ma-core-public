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
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.io.serial.SerialPortException;
import com.infiniteautomation.mango.io.serial.SerialPortIdentifier;
import com.infiniteautomation.mango.io.serial.SerialPortInputStream;
import com.infiniteautomation.mango.io.serial.SerialPortOutputStream;
import com.infiniteautomation.mango.io.serial.SerialPortProxy;
import com.infiniteautomation.mango.io.serial.SerialPortProxyEvent;
import com.infiniteautomation.mango.io.serial.SerialPortProxyEventListener;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;

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
	private SerialServerSocketBridgeOutputStream serialOutputStream;
	private SerialServerSocketBridgeInputStream serialInputStream;
	
	
	/**
	 * 
	 * @param id
	 * @param address
	 * @param port
	 * @param timeout (in ms)
	 */
	public SerialServerSocketBridge(SerialPortIdentifier id, int port, int bufferSize, int timeout) {
		super(id);
		this.port = port;
		this.timeout = timeout;
		this.serialOutputStream = new SerialServerSocketBridgeOutputStream();
		this.serialInputStream = new SerialServerSocketBridgeInputStream(bufferSize);
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
		if(this.serverThread != null) {
			this.serverThread.shutdown();
			Exception ex = null;
			try {
				//unblock the accept() method
				this.serverSocket.close();
			} catch (IOException e) {
				ex = e;
				LOG.error(e.getMessage(), e);
			}
			if(this.socket != null){
				try {
					//unblock the read() method
					this.socket.close();
				} catch (IOException e) {
					ex = e;
					LOG.error(e.getMessage(), e);
				} finally {
					this.socket = null;
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
			this.serverSocket.setSoTimeout(0);
			this.serverThread = new SerialServerSocketThread(this);
			this.serverThread.start();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw new SerialPortException(e);
		}

		
	}
	
	private void connect() {
		if(this.socket == null)
			throw new ShouldNeverHappenException("No socket to connect to.");
		try {
			serialOutputStream.connect(getSocketOutputStream());
			serialInputStream.connect(getSocketInputStream());
		} catch(Exception e) {
			throw new ShouldNeverHappenException("Failed to connect streams.");
		}
	}
	
	private void disconnect() {
		serialOutputStream.connect(null);
		serialInputStream.connect(null);
		if(this.socket != null) {
			try {
				//This will unblock the socket listener thread from its read call
				this.socket.close();
			} catch(Exception e) {
				LOG.error("Error closing socket for " + getCommPortId() + " error: " + e.getMessage());
			} finally{
				this.socket = null;
			}
		}
	}
	
	int available() {
		try {
			return this.serialInputStream.available();
		} catch(Exception e) {
			return 0;
		}
	}

	@Override
	public SerialPortInputStream getInputStream() {
		return serialInputStream;
	}

	@Override
	public SerialPortOutputStream getOutputStream() {
		return serialOutputStream;
	}

	public InputStream getSocketInputStream() throws IOException{
		return this.socket.getInputStream();
	}
	
	public OutputStream getSocketOutputStream() throws IOException{
		return this.socket.getOutputStream();
	}
	
	class SerialServerSocketThread extends Thread{
		
		private SerialServerSocketBridge bridge;
		private volatile boolean running = true;
		private volatile boolean socketRunning = false;
		private final SerialServerSocketBridgeInputStream stream;
		
		public SerialServerSocketThread(SerialServerSocketBridge bridge){
			super("Serial Server Socket Thread: " + bridge.getCommPortId());
			this.bridge = bridge;
			this.stream = (SerialServerSocketBridgeInputStream)bridge.getInputStream();
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
						if(this.bridge.socket == null) {
							this.bridge.socket = socket;
							this.bridge.socket.setSoTimeout(this.bridge.timeout);
							this.bridge.connect();
							socketRunning = true;
							while(socketRunning) {
								try {
									int available = stream.bufferRead();
									if(available != -1) {
										SerialPortProxyEvent spe = new SerialPortProxyEvent(Common.timer.currentTimeMillis());
										for(SerialPortProxyEventListener l : this.bridge.listeners) {
											l.serialEvent(spe);
										}
									} else {
										//No connection, disconnected.
										socketRunning = false;
										this.bridge.disconnect();
									}
								} catch(SerialServerSocketConnectionClosedException | SocketException e) {
									socketRunning = false;
									this.bridge.disconnect();
								} catch(IOException e) {
									LOG.error("IOException buffer reading for " + this.bridge.getCommPortId(), e);
								}
							}
						} else {
							//Should be unreachable?
							socket.close();
						}
					} catch(SocketException e) {
						if(running)
							LOG.error(e.getMessage(), e);
					} catch(Exception e){
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
						this.bridge.socket = null;
					} catch (IOException e) {
						LOG.error(e.getMessage(), e);
					}
				}
			}
		}
		
		public void shutdown(){
			this.running = false;
			this.socketRunning = false;
		}
	}
}
