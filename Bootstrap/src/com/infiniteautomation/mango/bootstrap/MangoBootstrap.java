/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Used to setup the classpath correctly then start Mango. Handles unzipping the core zip bundle if it is the
 * installation directory.
 *
 * @author Jared Wiltshire
 */
@SuppressWarnings("unused")
public class MangoBootstrap {

    public static void main(String[] args) throws Exception {
        MangoBootstrap bootstrap = new MangoBootstrap()
                .addJarsFromDirectory(Paths.get("lib"));

        CoreUpgrade upgrade = new CoreUpgrade(bootstrap.getInstallationDirectory());
        upgrade.upgrade();

        bootstrap.startMango(args);
    }

    private final List<URL> urls = new ArrayList<>();
    private final Path workingDirectory;
    private final Path installationDirectory;
    private final Path configFile;

    public MangoBootstrap() {
        this.workingDirectory = Paths.get(".").normalize().toAbsolutePath();
        this.installationDirectory = installationDirectory();
        this.configFile = configurationFile();
    }

    public Path getInstallationDirectory() {
        return installationDirectory;
    }

    public Path getConfigFile() {
        return configFile;
    }

    /**
     * @return class loader that can be used to start Mango
     */
    public URLClassLoader createClassLoader() {
        return createClassLoader(MangoBootstrap.class.getClassLoader());
    }

    /**
     * @param parent parent classloader
     * @return class loader that can be used to start Mango
     */
    public URLClassLoader createClassLoader(ClassLoader parent) {
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }

    /**
     * Starts Mango without forking
     * @param args passed to the Mango main method
     */
    public void startMango(String... args) throws Exception {
        // ensure Mango Core can find Mango home
        System.setProperty("mango.paths.home", installationDirectory.toString());
        System.setProperty("mango.config", configFile.toString());

        ClassLoader classLoader = createClassLoader();
        Class<?> mainClass = classLoader.loadClass("com.serotonin.m2m2.Main");
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

    /**
     * Add all jars from a directory to the class path.
     * @param directory relative to installation directory
     * @return this
     */
    public MangoBootstrap addJarsFromDirectory(Path directory) {
        Path d = installationDirectory.resolve(directory);
        if (Files.isDirectory(d)) {
            try {
                Files.list(d)
                        .filter(p -> p.toString().endsWith(".jar"))
                        .forEach(this::addPathInternal);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Add a path to the class path
     * @param path relative to installation directory
     * @return this
     */
    public MangoBootstrap addPath(Path path) {
        Path p = installationDirectory.resolve(path);
        if (Files.exists(p)) {
            addPathInternal(p);
        }
        return this;
    }

    private void addPathInternal(Path path) {
        try {
            urls.add(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Path configurationFile() {
        Optional<Path> explicitlySpecified = Arrays.stream(new Path[]{
                getPath(System.getProperty("mango.config")),
                getPath(System.getenv("mango_config")),
                getPath(System.getenv("MA_ENV_PROPERTIES"))
        }).filter(Objects::nonNull)
                .findFirst();

        if (explicitlySpecified.isPresent()) {
            return explicitlySpecified.get().toAbsolutePath().normalize();
        }

        // if data path is set via env variable, use that by default instead of install directory
        Path dataDirectory = Arrays.stream(new Path[]{
                getPath(System.getProperty("mango.paths.data"), installationDirectory),
                getPath(System.getenv("mango_paths_data"), installationDirectory)
        }).filter(Objects::nonNull)
                .findFirst()
                .orElse(installationDirectory);

        Path userHome = getPath(System.getProperty("user.home"));

        Optional<Path> existingPaths = Arrays.stream(new Path[]{
                installationDirectory.resolve(Paths.get("overrides", "properties", "env.properties")),
                installationDirectory.resolve(Paths.get("env.properties")),
                dataDirectory.resolve(Paths.get("env.properties")),
                dataDirectory.resolve(Paths.get("mango.properties")),
                userHome != null ? userHome.resolve("mango.properties") : null,
        }).filter(Objects::nonNull)
                .filter(Files::exists)
                .findFirst();

        return existingPaths.orElse(dataDirectory.resolve("mango.properties")).toAbsolutePath().normalize();
    }

    private Path installationDirectory() {
        Path fromJarLocation;
        try {
            URI uri = MangoBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path jarPath = Paths.get(uri);
            fromJarLocation = jarPath.toString().endsWith(".jar") ? jarPath.getParent().getParent() : null;
        } catch (Exception e) {
            fromJarLocation = null;
        }

        Optional<Path> installDirectory = Arrays.stream(new Path[]{
                getPath(System.getProperty("mango.paths.home")),
                getPath(System.getenv("mango_paths_home")),
                getPath(System.getenv("MA_HOME")),
                fromJarLocation,
                getPath(".."), // double click jar file
                getPath(".") // run java -jar boot/ma-bootstrap.jar from MA_HOME
        }).filter(Objects::nonNull)
                .filter(this::isInstallationDirectory)
                .findFirst();

        installDirectory.orElseThrow(() -> new RuntimeException("Can't find install directory, please set the environment variable 'mango_paths_home'"));
        return installDirectory.get().toAbsolutePath().normalize();
    }

    private Path getPath(String path) {
        return getPath(path, workingDirectory);
    }

    private Path getPath(String path, Path relativeTo) {
        if (path == null) return null;
        try {
            return relativeTo.resolve(path).normalize();
        } catch (InvalidPathException e) {
            return null;
        }
    }

    public boolean isInstallationDirectory(Path testMaHome) {
        return Files.isRegularFile(testMaHome.resolve("release.properties")) || Files.isRegularFile(testMaHome.resolve("release.signed"));
    }
}
