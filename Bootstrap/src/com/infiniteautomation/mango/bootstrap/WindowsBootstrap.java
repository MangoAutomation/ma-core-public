/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap;

import java.lang.reflect.Method;

import com.infiniteautomation.mango.bootstrap.classloader.MemoryClassLoader;

/**
 * @author Jared Wiltshire
 */
public class WindowsBootstrap {

    public static void main(String[] args) throws Exception {
        MemoryClassLoader cl = new MemoryClassLoader();
        cl.loadJar("com/infiniteautomation/mango/bootstrap/windows.jar");

        Class<?> mainClass = cl.loadClass("com.infiniteautomation.mango.bootstrap.windows.WindowsService");
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

}
