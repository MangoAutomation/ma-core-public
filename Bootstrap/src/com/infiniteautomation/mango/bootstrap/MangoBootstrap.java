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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jared Wiltshire
 */
public class MangoBootstrap {

    public static final String JAR_FILENAME = "ma-bootstrap.jar";

    public static void main(String[] args) throws Exception {
        ClassLoader cl = new MangoBootstrap().getClassLoader();
        Class<?> mainClass = cl.loadClass("com.serotonin.m2m2.Main");
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

    private List<URL> urls = new ArrayList<>();
    private final Path maHome;

    public MangoBootstrap() {
        this.maHome = maHome();

        addUrl(maHome.resolve("overrides").resolve("classes"));
        addUrl(maHome.resolve("classes"));
        addUrl(maHome.resolve("overrides").resolve("properties"));
        addJars(maHome.resolve("overrides").resolve("lib"));
        addJars(maHome.resolve("lib"));
    }

    public ClassLoader getClassLoader() {
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        Thread.currentThread().setContextClassLoader(classLoader);
        return classLoader;
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

    public static Path maHome() {
        String maHomeStr = System.getProperty("ma.home");
        if (maHomeStr == null) {
            if (Files.isRegularFile(Paths.get(JAR_FILENAME))) {
                maHomeStr = "..";
            } else {
                maHomeStr = ".";
            }
        }
        return Paths.get(maHomeStr).toAbsolutePath().normalize();
    }
}
