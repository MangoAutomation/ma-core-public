/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.systemSettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public void testMultiThreadingAddListener() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        List<SystemSettingsListenerTester> listeners = new ArrayList<>();
        AtomicInteger running = new AtomicInteger();
        int threads = 5;
        for(int i=0; i<threads; i++) {
            SystemSettingsListenerTester l = new SystemSettingsListenerTester();
            listeners.add(l);
            Thread t = new Thread() {
                @Override
                public void run() {
                    //add listener
                    SystemSettingsEventDispatcher.INSTANCE.addListener(l);                    
                    //Do work
                    SystemSettingsEventDispatcher.INSTANCE.fireSystemSettingSaved("testKey", "old", "new");
                    try{ Thread.sleep(10); }catch(InterruptedException e) {}
                    SystemSettingsEventDispatcher.INSTANCE.fireSystemSettingRemoved("testKey", "old", "default");
                    running.decrementAndGet();
                };
            };
            running.incrementAndGet();
            t.start();
        }

        //Wait for all listeners to be removed or fail
        int wait = 10;
        while(wait > 0) {
            try{ Thread.sleep(500); }catch(InterruptedException e) {}
            if(running.get() == 0) {
                break;
            }
            wait--;
        }
        //Get the map ensure it is empty i.e. all have been removed.
        Field listenerMapField = SystemSettingsEventDispatcher.INSTANCE.getClass().getDeclaredField("listenerMap");
        listenerMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Set<SystemSettingsListener>> listenerMap = (Map<String, Set<SystemSettingsListener>>) listenerMapField.get(SystemSettingsEventDispatcher.INSTANCE);
        Set<SystemSettingsListener> testKeyListeners = listenerMap.get("testKey");
        assertEquals(threads, testKeyListeners.size());
        //Remove all
        for(SystemSettingsListenerTester l : listeners) {
            assertTrue(l.saved.get());
            assertTrue(l.removed.get());
            //remove listener
            SystemSettingsEventDispatcher.INSTANCE.removeListener(l);
        }
    }
    
    
    @Test
    public void testMultiThreadingAddRemoveListener() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        AtomicInteger running = new AtomicInteger();
        int threads = 5;
        for(int i=0; i<threads; i++) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    SystemSettingsListenerTester l = new SystemSettingsListenerTester();
                    //add listener
                    SystemSettingsEventDispatcher.INSTANCE.addListener(l);                    
                    //Do work
                    SystemSettingsEventDispatcher.INSTANCE.fireSystemSettingSaved("testKey", "old", "new");
                    try{ Thread.sleep(10); }catch(InterruptedException e) {}
                    SystemSettingsEventDispatcher.INSTANCE.fireSystemSettingRemoved("testKey", "old", "default");
                    //remove listener
                    SystemSettingsEventDispatcher.INSTANCE.removeListener(l);
                    running.decrementAndGet();
                };
            };
            running.incrementAndGet();
            t.start();
        }

        //Wait for all listeners to be removed or fail
        int wait = 10;
        while(wait > 0) {
            try{ Thread.sleep(500); }catch(InterruptedException e) {}
            if(running.get() == 0) {
                break;
            }
            wait--;
        }
        //Get the map ensure it is empty i.e. all have been removed.
        Field listenerMapField = SystemSettingsEventDispatcher.INSTANCE.getClass().getDeclaredField("listenerMap");
        listenerMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Set<SystemSettingsListener>> listenerMap = (Map<String, Set<SystemSettingsListener>>) listenerMapField.get(SystemSettingsEventDispatcher.INSTANCE);
        Set<SystemSettingsListener> testKeyListeners = listenerMap.get("testKey");
        assertNull(testKeyListeners);
    }
    
    @Test
    public void testMultiThreadingSaveRemoveSetting() {

        List<SystemSettingsListenerTester> listeners = new ArrayList<>();
        int threads = 5;
        for(int i=0; i<threads; i++) {
            SystemSettingsListenerTester l = new SystemSettingsListenerTester();
            //save for later
            listeners.add(l);
            //add listener
            SystemSettingsEventDispatcher.INSTANCE.addListener(l);
            Thread t = new Thread() {
                @Override
                public void run() {
                    
                    //Do work
                    SystemSettingsEventDispatcher.INSTANCE.fireSystemSettingSaved("testKey", "old", "new");
                    try{ Thread.sleep(10); }catch(InterruptedException e) {}
                    SystemSettingsEventDispatcher.INSTANCE.fireSystemSettingRemoved("testKey", "old", "default");
                };
            };
            t.start();
        }

        //Wait for all listeners to be fired or fail
        int wait = 10;
        while(wait > 0) {
            try{ Thread.sleep(500); }catch(InterruptedException e) {}
            boolean allFired = true;
            for(SystemSettingsListenerTester l : listeners) {
                if(!l.saved.get() || !l.removed.get()) {
                    allFired = false;
                    break;
                }
            }
            if(allFired)
                break;
            wait--;
        }
        for(SystemSettingsListenerTester l : listeners) {
            assertTrue(l.saved.get());
            assertTrue(l.removed.get());
            //remove listener
            SystemSettingsEventDispatcher.INSTANCE.removeListener(l);
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
