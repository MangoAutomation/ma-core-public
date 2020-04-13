/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jared Wiltshire
 */
public class MangoBootstrap {

    public static void main(String[] args) throws Exception {
        Path maHome = BootstrapUtils.maHome();

        CoreUpgrade upgrade = new CoreUpgrade(maHome);
        upgrade.upgrade();

        ClassLoader classLoader = new MangoBootstrap(maHome).createClassLoader();
        Class<?> mainClass = classLoader.loadClass("com.serotonin.m2m2.Main");
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

    private List<URL> urls = new ArrayList<>();
    private final Path maHome;

    public MangoBootstrap(Path maHome) {
        this.maHome = maHome;

        addUrl(maHome.resolve("overrides").resolve("classes"));
        addUrl(maHome.resolve("classes"));
        addUrl(maHome.resolve("overrides").resolve("properties"));
        addJars(maHome.resolve("overrides").resolve("lib"));
        addJars(maHome.resolve("lib"));
    }

    public URLClassLoader createClassLoader() {
        return createClassLoader(MangoBootstrap.class.getClassLoader());
    }

    public URLClassLoader createClassLoader(ClassLoader parent) {
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
    }

    public MangoBootstrap addJars(Path directory) {
        if (Files.isDirectory(directory)) {
            try {
                Files.list(directory)
                .filter(p -> p.toString().endsWith(".jar"))
                .forEach(this::addUrl);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public MangoBootstrap addUrl(Path p) {
        if (Files.exists(p)) {
            try {
                urls.add(p.toUri().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public List<URL> getUrls() {
        return urls;
    }

    public Path getMaHome() {
        return maHome;
    }

}
