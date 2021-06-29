/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
