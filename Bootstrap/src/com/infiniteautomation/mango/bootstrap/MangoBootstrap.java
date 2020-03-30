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
        Path maHome = maHome();

        CoreUpgrade upgrade = new CoreUpgrade(maHome);
        upgrade.upgrade();

        ClassLoader cl = new MangoBootstrap(maHome).getClassLoader();
        Class<?> mainClass = cl.loadClass("com.serotonin.m2m2.Main");
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
        String[] possibleMaHomes = new String[] {
                System.getProperty("ma.home"),
                "..",
                "."
        };

        Path maHome = null;
        for (String possibleMaHome : possibleMaHomes) {
            if (possibleMaHome == null) {
                continue;
            }

            Path test = Paths.get(possibleMaHome);
            if (isMaHome(test)) {
                maHome = test;
                break;
            }
        }

        if (maHome == null) {
            throw new RuntimeException("Can't find MA_HOME, please set a Java system property -Dma.home=\"path\\to\\mango\"");
        }

        return maHome.toAbsolutePath().normalize();
    }

    private static boolean isMaHome(Path testMaHome) {
        return Files.isRegularFile(testMaHome.resolve("release.properties")) || Files.isRegularFile(testMaHome.resolve("release.signed"));
    }
}
