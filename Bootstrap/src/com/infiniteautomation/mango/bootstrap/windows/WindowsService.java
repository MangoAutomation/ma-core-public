/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap.windows;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import com.infiniteautomation.mango.bootstrap.BootstrapUtils;
import com.infiniteautomation.mango.bootstrap.CoreUpgrade;
import com.infiniteautomation.mango.bootstrap.MangoBootstrap;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;

/**
 * @author Jared Wiltshire
 */
public class WindowsService extends Win32Service {

    public static final String JAR_FILENAME = "ma-bootstrap-windows.jar";

    public static void main(String[] args) throws Exception {
        String serviceName = System.getProperty("service.name", "MangoAutomation");

        try {

            if (args.length > 0) {
                String action = args[0];
                switch(action) {
                    case "install": WindowsService.install(serviceName); break;
                    case "uninstall": Win32Service.uninstall(serviceName); break;
                    case "start": Win32Service.start(serviceName); break;
                    case "stop": Win32Service.stop(serviceName); break;
                    case "init": {
                        WindowsService service = new WindowsService(serviceName);
                        service.init();
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Invalid option, the only arguments accepted are init, start, stop, install, or uninstall");
                }
                return;
            }

            // default action is to start the service, if not already installed it will be installed then started
            try {
                Win32Service.start(serviceName);
            } catch (Win32Exception e) {
                if (e.getErrorCode() == WinError.ERROR_SERVICE_DOES_NOT_EXIST) {
                    WindowsService.install(serviceName);
                    Win32Service.start(serviceName);
                } else {
                    throw e;
                }
            }

        } catch (Win32Exception e) {
            // if we are not allowed to start/stop/install etc we restart using ShellExecuteEx "runas"
            // UAC will prompt for administrator access and the the JVM will be restarted using the same command line
            if (e.getErrorCode() == WinError.ERROR_ACCESS_DENIED) {
                Win32Service.restartAsAdmin();
            } else {
                throw e;
            }
        }
    }

    public static void install(String serviceName) {
        Path javaHome = Paths.get(System.getProperty("java.home")).toAbsolutePath().normalize();
        Path javaExe = javaHome.resolve("bin").resolve("java.exe");
        String javaOptions = System.getProperty("service.javaOptions", "-server");

        Path maHome = BootstrapUtils.maHome();
        Path jarFile = maHome.resolve("boot").resolve(JAR_FILENAME);
        String userJarFile = System.getProperty("service.jar");
        if (userJarFile != null) {
            jarFile = Paths.get(userJarFile).toAbsolutePath().normalize();
        }

        String defaultCommand = String.join(" ", new String[] {
                "\"" + javaExe + "\"",
                javaOptions,
                "-Dma.home=\"" + maHome + "\"",
                "-jar \"" + jarFile + "\"",
                "init"
        });

        String displayName = System.getProperty("service.displayName", "Mango Automation");
        String description = System.getProperty("service.description", "Mango Automation by Infinite Automation Systems Inc");
        String account = System.getProperty("service.account");
        String password = System.getProperty("service.password");
        String command = System.getProperty("service.command", defaultCommand);

        String dependenciesStr = System.getProperty("service.dependencies", "Tcpip,iphlpsvc,Dnscache");
        String[] dependencies = dependenciesStr.split("\\s*,\\s*");

        Win32Service.install(serviceName, displayName, description, dependencies, account, password, command, true);
    }

    private volatile Object lifecycle;

    public WindowsService(String serviceName) {
        super(serviceName);
    }

    @Override
    protected void startImpl() throws Exception {
        Path maHome = BootstrapUtils.maHome();

        CoreUpgrade upgrade = new CoreUpgrade(maHome);
        upgrade.upgrade();

        ClassLoader cl = new MangoBootstrap(maHome).getClassLoader();
        this.lifecycle = cl.loadClass("com.serotonin.m2m2.Main")
                .getMethod("createLifecycle")
                .invoke(null);

        Method addListener = lifecycle.getClass().getMethod("addListener", Consumer.class);
        addListener.invoke(lifecycle, (Consumer<?>) this::lifecycleStateChanged);

        lifecycle.getClass().getMethod("initialize", ClassLoader.class)
        .invoke(lifecycle, Thread.currentThread().getContextClassLoader());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void stopImpl() throws Exception {
        ClassLoader cl = lifecycle.getClass().getClassLoader();

        Class<Enum> terminationReasonClass = (Class<Enum>) cl.loadClass("com.serotonin.m2m2.TerminationReason");
        Enum shutdown = Enum.valueOf(terminationReasonClass, "SHUTDOWN");

        lifecycle.getClass()
        .getMethod("terminate", terminationReasonClass)
        .invoke(lifecycle, shutdown);
    }

    private void lifecycleStateChanged(Object state) {
        try {
            String name = (String) state.getClass().getMethod("name").invoke(state);

            if (name.equals("SHUTDOWN_TASKS_RUNNING")) {
                this.setStatusStopPending();
            } else if (name.equals("TERMINATED")) {
                Object reason = lifecycle.getClass()
                        .getMethod("getTerminationReason")
                        .invoke(lifecycle);

                String reasonName = (String) reason.getClass().getMethod("name").invoke(reason);
                switch (reasonName) {
                    case "SHUTDOWN":
                    case "SHUTDOWN_HOOK":
                    case "LICENSE_VIOLATION":
                        // clean shutdown
                        this.setStatusStopped();
                        break;
                    case "ERROR":
                        this.setStatusStopped(WinError.ERROR_SERVICE_SPECIFIC_ERROR, 1);
                        break;
                    case "RESTART":
                        this.setStatusStopped(WinError.ERROR_SERVICE_SPECIFIC_ERROR, 2);
                        break;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
