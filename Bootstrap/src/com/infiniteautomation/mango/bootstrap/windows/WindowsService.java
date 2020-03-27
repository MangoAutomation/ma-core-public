/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap.windows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.infiniteautomation.mango.bootstrap.MangoBootstrap;

/**
 * @author Jared Wiltshire
 */
public class WindowsService extends Win32Service {

    public static final String JAR_FILENAME = "ma-bootstrap-windows.jar";

    public static void main(String[] args) throws Exception {
        String serviceName = System.getProperty("service.name", "MangoAutomation");

        if (args.length > 0) {
            String action = args[0];
            switch(action) {
                case "install": WindowsService.install(serviceName); break;
                case "uninstall": Win32Service.uninstall(serviceName); break;
                default:
                    throw new IllegalArgumentException("Invalid option, the only arguments accepted are install, or uninstall");
            }
            return;
        }

        new WindowsService(serviceName);
    }

    public static void install(String serviceName) {
        String maHomeStr = System.getProperty("ma.home");
        if (maHomeStr == null) {
            if (Files.isRegularFile(Paths.get(JAR_FILENAME))) {
                maHomeStr = "..";
            } else {
                maHomeStr = ".";
            }
        }

        Path javaHome = Paths.get(System.getProperty("java.home")).normalize().toAbsolutePath();
        Path javaExe = javaHome.resolve("bin").resolve("java.exe");
        String javaOptions = System.getProperty("service.javaOptions", "-server");

        Path maHome = Paths.get(maHomeStr).normalize().toAbsolutePath();
        Path jarFile = maHome.resolve("boot").resolve(JAR_FILENAME);
        String userJarFile = System.getProperty("service.jar");
        if (userJarFile != null) {
            jarFile = Paths.get(userJarFile).normalize().toAbsolutePath();
        }

        String defaultCommand = String.join(" ", new String[] {
                "\"" + javaExe + "\"",
                javaOptions,
                "-Dma.home=\"" + maHome + "\"",
                "-jar \"" + jarFile + "\""
        });

        String displayName = System.getProperty("service.displayName", "Mango Automation");
        String description = System.getProperty("service.description", "Mango Automation by Infinite Automation Systems Inc");
        String account = System.getProperty("service.account");
        //String account = System.getProperty("service.account", "NT AUTHORITY\\LocalService");
        String password = System.getProperty("service.password");
        String command = System.getProperty("service.command", defaultCommand);

        String dependenciesStr = System.getProperty("service.dependencies", "Tcpip,iphlpsvc,Dnscache");
        String[] dependencies = dependenciesStr.split("\\s*,\\s*");

        Win32Service.install(serviceName, displayName, description, dependencies, account, password, command, true);
    }

    private volatile MangoFunctions functions;

    public WindowsService(String serviceName) {
        super(serviceName);
    }

    @Override
    protected void startImpl() throws Exception {
        ClassLoader cl = new PrefixClassLoader("stage2", new MangoBootstrap().getClassLoader());
        Class<?> implClass = cl.loadClass("com.infiniteautomation.mango.bootstrap.windows.stage2.MangoFunctionsImpl");
        this.functions = (MangoFunctions) implClass.newInstance();

        this.functions.start();
    }

    @Override
    protected void stopImpl() throws Exception {
        functions.stop();
    }

}
