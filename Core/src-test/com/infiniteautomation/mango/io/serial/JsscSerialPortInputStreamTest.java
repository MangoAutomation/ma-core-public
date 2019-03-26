/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * @author Terry Packer
 *
 */
public class JsscSerialPortInputStreamTest {

    @Test
    public void testListenersAtHighVolume() throws SerialPortException, IOException {
        
        String portName = "/dev/null";
        List<SerialPortProxyEventListener> listeners  = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        listeners.add((evt) -> {
            count.incrementAndGet();
        });
        
        TestSerialPort port = new TestSerialPort(portName);
        JsscSerialPortInputStream is = new JsscSerialPortInputStream(port, listeners);
        
        for(int i=0; i<100; i++) {
            SerialPortEvent event = new SerialPortEvent(portName, SerialPortEvent.RXCHAR, i);
            is.serialEvent(event);
        }
        
        while(is.listenerTasks.size() > 0) {
            try {
                System.out.println("Waiting...");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                
            }
        }
        is.close();
        assertEquals(100, count.get());
    }
    
    /**
     * Mask methods that are not necessary for this test
     * @author Terry Packer
     *
     */
    class TestSerialPort extends SerialPort {

        /**
         * @param portName
         */
        public TestSerialPort(String portName) {
            super(portName);
        }
        
        @Override
        public void addEventListener(SerialPortEventListener listener, int mask)
                throws SerialPortException {
            //No-op for testing
        }
        
        @Override
        public boolean removeEventListener() throws SerialPortException {
            //No-op for testing
            return true;
        }
        
        @Override
        public byte[] readBytes(int byteCount) throws SerialPortException {
            return new byte[] {0,0,0,0};
        }
        
        @Override
        public boolean closePort() throws SerialPortException {
            //No-op for testing
            return true;
        }
        
    }
    
}
