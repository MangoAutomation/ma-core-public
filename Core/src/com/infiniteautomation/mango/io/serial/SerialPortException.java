/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

/**
 * @author Matthew Lohbihler
 *
 */
public class SerialPortException extends Exception {
    private static final long serialVersionUID = -1;
    
    public SerialPortException(String message) {
        super(message);
    }

    public SerialPortException(Throwable cause) {
        super(cause);
    }

}
