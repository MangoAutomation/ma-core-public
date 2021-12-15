/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.module.Module;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * @author Terry Packer
 *
 */
public class JsscSerialPortInputStreamTest extends MangoTestBase {

    @Test
    public void testListenersAtHighVolume() throws Exception {
        
        Common.serialPortManager.refreshFreeCommPorts();
        
        String portName = "/dev/null";
        List<SerialPortProxyEventListener> listeners  = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        listeners.add((evt) -> {
            count.incrementAndGet();
        });
        
        TestSerialPort port = new TestSerialPort(portName);
        long period = 100;
        TimeUnit unit = TimeUnit.NANOSECONDS;
        JsscSerialPortInputStream is = new JsscSerialPortInputStream(port, period, unit, listeners);
        
        for(int i=0; i<100; i++) {
            SerialPortEvent event = new SerialPortEvent(portName, SerialPortEvent.RXCHAR, i);
            is.serialEvent(event);
        }
        
        while(JsscSerialPortManager.instance.eventQueue.size() > 0) {
            try {
                System.out.println("Waiting...");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                
            }
        }
        is.close();
        assertEquals(100, count.get());
    }
    
    @Override
    protected MockMangoLifecycle getLifecycle() {
        return new SerialPortTestLifecycle(modules);
    }
    
    class SerialPortTestLifecycle extends MockMangoLifecycle {

        /**
         */
        public SerialPortTestLifecycle(List<Module> modules) {
            super(modules);
        }
        
        @Override
        protected SerialPortManager getSerialPortManager() {
            return new SerialPortManagerImpl();
        }
        
    }
    
    /**
     * Mask methods that are not necessary for this test
     * @author Terry Packer
     *
     */
    class TestSerialPort extends SerialPort {

        /**
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
