/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.systemSettings;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.serotonin.m2m2.MangoTestBase;

/**
 * @author Terry Packer
 *
 */
public class SystemSettingsEventDispatcherTest extends MangoTestBase {
    
    @Test
    public void testMultiThreading() {
        
        List<SystemSettingsListenerTester> listeners = new ArrayList<>();
        AtomicInteger running = new AtomicInteger();
        //TODO Bug in event dispatcher class where rejected tasks are ignored
        int threads = 5;
        for(int i=0; i<threads; i++) {
            Thread t = new Thread() {
                public void run() {
                    SystemSettingsListenerTester l = new SystemSettingsListenerTester();
                    //save for later
                    listeners.add(l);
                    //add listener
                    SystemSettingsEventDispatcher.addListener(l);
                    //Do work
                    SystemSettingsEventDispatcher.fireSystemSettingSaved("testKey", "old", "new");
                    try{ Thread.sleep(10); }catch(InterruptedException e) {}
                    SystemSettingsEventDispatcher.fireSystemSettingRemoved("testKey", "old", "default");
                    //remove listener
                    SystemSettingsEventDispatcher.removeListener(l);
                    running.decrementAndGet();
                };
            };
            running.incrementAndGet();
            t.start();
        }
        
        int wait = 10;
        while(wait > 0) {
            try{ Thread.sleep(500); }catch(InterruptedException e) {}
            if(running.get() == 0) {
                break;
            }
            wait--;
        }
        for(SystemSettingsListenerTester l : listeners) {
            assertTrue(l.saved.get());
            assertTrue(l.removed.get());
        }
        
    }

    
    class SystemSettingsListenerTester implements SystemSettingsListener {
        AtomicBoolean saved = new AtomicBoolean();
        AtomicBoolean removed = new AtomicBoolean();
        
        @Override
        public void systemSettingsSaved(String key, String oldValue,
                String newValue) {
            if("testKey".equals(key)) {
                saved.set(true);
            }
        }

        @Override
        public void systemSettingsRemoved(String key, String lastValue,
                String defaultValue) {
            if("testKey".equals(key)) {
                removed.set(true);
            }
        }

        @Override
        public List<String> getKeys() {
            return Arrays.asList("testKey");
        }
    }
    
}
