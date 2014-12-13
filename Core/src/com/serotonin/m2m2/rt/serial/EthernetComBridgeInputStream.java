package com.serotonin.m2m2.rt.serial;

import java.io.IOException;
import java.io.InputStream;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.io.serial.SerialPortInputStream;

public class EthernetComBridgeInputStream extends SerialPortInputStream{

	private InputStream stream;
	
	public EthernetComBridgeInputStream(InputStream is){
		this.stream = is;
	}
	
	@Override
	public int read() throws IOException {
		return this.stream.read();
	}

	@Override
	public int available() throws IOException {
		return this.stream.available();
	}

	@Override
	public void closeImpl() throws IOException {
		this.stream.close();
	}

	@Override
	public int peek() {
		throw new ShouldNeverHappenException("Unimplemented.");
	}

}
