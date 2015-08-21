/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Terry Packer
 *
 */
public abstract class SerialPortOutputStream  extends OutputStream {

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public abstract void write(int arg0) throws IOException;

	@Override
	public abstract void flush();
	
}
