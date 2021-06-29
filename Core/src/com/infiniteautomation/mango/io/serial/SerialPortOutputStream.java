/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Terry Packer
 *
 */
public abstract class SerialPortOutputStream  extends OutputStream {

	@Override
	public abstract void write(int arg0) throws IOException;

	@Override
	public abstract void flush();
	
}
