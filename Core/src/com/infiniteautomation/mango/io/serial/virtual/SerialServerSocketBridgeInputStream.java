package com.infiniteautomation.mango.io.serial.virtual;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.infiniteautomation.mango.io.serial.SerialPortInputStream;
import com.serotonin.ShouldNeverHappenException;

public class SerialServerSocketBridgeInputStream extends SerialPortInputStream {
	private InputStream stream = null;
	private ByteArrayInputStream bufferStream = null;
	private final int bufferSize;
	
	public SerialServerSocketBridgeInputStream(int bufferSize) {
		super();
		this.bufferSize = bufferSize;
	}
	
	public void connect(InputStream in) {
		this.stream = in;
	}
	
	public int bufferRead() throws IOException, SerialServerSocketConnectionClosedException {
		if(stream == null)
			return -1;
		if(bufferStream != null)
			return bufferStream.available();
		//Add one so we can insert a -1 at the end and close our buffer stream
		byte[] data = new byte[bufferSize+1];
		int read = stream.read(data, 0, data.length);
		if(read == -1)
			throw new SerialServerSocketConnectionClosedException();
		data[read] = -1;
		bufferStream = new ByteArrayInputStream(data);
		return read;
	}
	
	@Override
	public int read() throws IOException {
		if(this.bufferStream == null) {
			try { 
				bufferRead(); 
			} catch(SerialServerSocketConnectionClosedException e) {
				return -1;
			}
			if(this.bufferStream == null)
				return -1;
		}
		int read = this.bufferStream.read();
		if(read == -1) {
			this.bufferStream.close();
			this.bufferStream = null;
		}
		return read;
	}

	@Override
	public int available() throws IOException {
		if(this.bufferStream == null)
			return 0;
		return this.bufferStream.available();
	}

	@Override
	public void closeImpl() throws IOException {
		if(this.stream != null)
			this.stream.close();
		if(this.bufferStream != null)
			this.bufferStream.close();
	}

	@Override
	public int peek() {
		throw new ShouldNeverHappenException("Unimplemented.");
	}

}
