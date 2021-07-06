/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used to setup the classpath correctly then start Mango. Handles unzipping the core zip bundle if it is the
 * installation directory.
 *
 * @author Jared Wiltshire
 */
public class MangoBootstrap {

    public static final String MAIN_CLASS = "com.serotonin.m2m2.Main";

    public static void main(String[] args) throws Exception {
        MangoBootstrap bootstrap = new MangoBootstrap()
                .addJarDirectory(Paths.get("lib"));

        CoreUpgrade upgrade = new CoreUpgrade(bootstrap.getInstallationDirectory());
        upgrade.upgrade();

        Optional<String> fork = Arrays.stream(args).filter("-fork"::equals).findAny();

        if (fork.isPresent()) {
            Optional<String> wait = Arrays.stream(args).filter("-wait"::equals).findAny();
            Optional<String> io = Arrays.stream(args).filter("-io"::equals).findAny();
            bootstrap.forkMango(wait.isPresent(), io.isPresent());
        } else {
            bootstrap.startMango();
        }
    }

    private final List<ClassPathEntry> classPath = new ArrayList<>();
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

    @SuppressWarnings("unused")
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
        URL[] urls = classPath.stream()
                .flatMap(e -> e.jarDirectory ? listJars(e.path) : Stream.of(e.path))
                .map(this::pathToUrl)
                .toArray(URL[]::new);
        return new URLClassLoader(urls, parent);
    }

    private Stream<Path> listJars(Path directory) {
        try {
            return Files.list(directory)
                    .filter(p -> p.toString().endsWith(".jar"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Starts Mango without forking
     */
    public void startMango() throws Exception {
        // ensure Mango Core can find Mango home
        System.setProperty("mango.paths.home", installationDirectory.toString());
        System.setProperty("mango.config", configFile.toString());

        ClassLoader classLoader = createClassLoader();
        Class<?> mainClass = classLoader.loadClass(MAIN_CLASS);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) new String[0]);
    }

    /**
     * Forks and starts Mango in a new process.
     */
    public void forkMango(boolean wait, boolean io) throws Exception {
        String javaCmd = "java";
        Path javaHome = getPath(System.getenv("JAVA_HOME"));
        if (javaHome != null && Files.isDirectory(javaHome)) {
            javaCmd = javaHome.resolve(Paths.get("bin", "java")).toString();
        }

        ProcessBuilder builder = new ProcessBuilder()
                .command(javaCmd, "-server", MAIN_CLASS);

        if (io) {
            builder.inheritIO();
        }

        String classPath = this.classPath.stream()
                .map(e -> e.jarDirectory ? e.path.toString() + File.separator + "*" : e.path.toString())
                .collect(Collectors.joining(File.pathSeparator));

        Map<String, String> env = builder.environment();
        env.put("CLASSPATH", classPath);
        env.put("mango_paths_home", installationDirectory.toString());
        env.put("mango_config", configFile.toString());

        Process process = builder.start();
        if (wait) {
            process.waitFor();
        }
    }

    /**
     * Add all jars from a directory to the class path.
     * @param directory relative to installation directory
     * @return this
     */
    public MangoBootstrap addJarDirectory(Path directory) {
        Path d = installationDirectory.resolve(directory);
        if (Files.isDirectory(d)) {
            classPath.add(new ClassPathEntry(d, true));
        }
        return this;
    }

    /**
     * Add a path to the class path
     * @param path relative to installation directory
     * @return this
     */
    @SuppressWarnings("unused")
    public MangoBootstrap addPath(Path path) {
        Path p = installationDirectory.resolve(path);
        if (Files.exists(p)) {
            classPath.add(new ClassPathEntry(p));
        }
        return this;
    }

    private URL pathToUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Config file (mango.properties) search path:
     * $mango_config
     * $MA_ENV_PROPERTIES (legacy environment variable)
     * $mango_paths_data/mango.properties
     * $mango_paths_data/env.properties
     * ~/mango.properties
     * $mango_paths_home/env.properties (legacy location)
     * $mango_paths_home/overrides/properties/env.properties (legacy location)
     *
     * @return path to config file
     */
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

        Optional<Path> dataDirectory = Arrays.stream(new Path[]{
                getPath(System.getProperty("mango.paths.data"), installationDirectory),
                getPath(System.getenv("mango_paths_data"), installationDirectory)
        }).filter(Objects::nonNull)
                .findFirst();
        Path userHome = getPath(System.getProperty("user.home"));

        List<Path> configPaths = new ArrayList<>();
        if (dataDirectory.isPresent()) {
            Path dir = dataDirectory.get();
            configPaths.add(dir.resolve("mango.properties"));
            configPaths.add(dir.resolve("env.properties"));
        }
        if (userHome != null) {
            configPaths.add(userHome.resolve("mango.properties"));
        }
        configPaths.add(installationDirectory.resolve(Paths.get("env.properties")));
        configPaths.add(installationDirectory.resolve(Paths.get("overrides", "properties", "env.properties")));

        Optional<Path> existingPaths = configPaths.stream()
                .filter(Objects::nonNull)
                .filter(Files::exists)
                .findFirst();

        Path defaultLocation = dataDirectory.orElse(installationDirectory).resolve("mango.properties");
        return existingPaths.orElse(defaultLocation).toAbsolutePath().normalize();
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

    private static class ClassPathEntry {
        final Path path;
        final boolean jarDirectory;

        private ClassPathEntry(Path path) {
            this(path, false);
        }

        private ClassPathEntry(Path path, boolean jarDirectory) {
            this.path = path;
            this.jarDirectory = jarDirectory;
        }
    }
}
