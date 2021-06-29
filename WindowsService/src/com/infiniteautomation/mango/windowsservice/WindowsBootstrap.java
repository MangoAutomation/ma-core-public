/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.windowsservice;

import java.lang.reflect.Method;

/**
 * @author Jared Wiltshire
 */
public class WindowsBootstrap {

    public static void main(String[] args) throws Exception {
        MemoryClassLoader cl = new MemoryClassLoader();
        cl.loadJar("com/infiniteautomation/mango/windowsservice/stage2.jar");

        Class<?> mainClass = cl.loadClass("com.infiniteautomation.mango.windowsservice.stage2.WindowsService");
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

}
