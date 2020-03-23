/*
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package com.infiniteautomation.mango.bootstrap.windows;

import com.infiniteautomation.mango.bootstrap.windows.ServiceControlManager.Service;
import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.Winsvc.HandlerEx;
import com.sun.jna.platform.win32.Winsvc.SERVICE_MAIN_FUNCTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_TABLE_ENTRY;

/**
 * Baseclass for a Win32 service.
 * @author Jared Wiltshire
 */
public abstract class Win32Service {

    protected final String serviceName;
    private volatile SERVICE_STATUS_HANDLE serviceStatusHandle;
    private final HandlerEx serviceControlHandler;
    private final SERVICE_MAIN_FUNCTION serviceMainFunction;
    private final int startWaitHint;
    private final int stopWaitHint;

    /**
     * Creates a new instance of Win32Service.
     *
     * @param serviceName internal name of the service
     */
    public Win32Service(String serviceName) {
        this.serviceName = serviceName;
        this.serviceControlHandler = this::serviceControl;
        this.serviceMainFunction = this::serviceMain;
        this.startWaitHint = 60000;
        this.stopWaitHint = 60000;
        this.init();
    }

    /**
     * Initialize the service, connect to the ServiceControlManager.
     */
    private void init() {
        Thread.currentThread().setName("Service " + serviceName + " control handler");
        SERVICE_TABLE_ENTRY entry = new SERVICE_TABLE_ENTRY();
        entry.lpServiceName = serviceName;
        entry.lpServiceProc = serviceMainFunction;

        if (!Advapi32.INSTANCE.StartServiceCtrlDispatcher((SERVICE_TABLE_ENTRY[]) entry.toArray(2))) {
            throwLastError();
        }
    }

    protected void setStatusStartPending() {
        SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
        serviceStatus.dwServiceType = WinNT.SERVICE_WIN32_OWN_PROCESS;
        serviceStatus.dwCurrentState = Winsvc.SERVICE_START_PENDING;
        serviceStatus.dwWaitHint = startWaitHint;
        setStatus(serviceStatus);
    }

    protected void setStatusRunning() {
        SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
        serviceStatus.dwServiceType = WinNT.SERVICE_WIN32_OWN_PROCESS;
        serviceStatus.dwCurrentState = Winsvc.SERVICE_RUNNING;
        serviceStatus.dwControlsAccepted = Winsvc.SERVICE_ACCEPT_STOP | Winsvc.SERVICE_ACCEPT_SHUTDOWN;
        setStatus(serviceStatus);
    }

    protected void setStatusStopPending() {
        SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
        serviceStatus.dwServiceType = WinNT.SERVICE_WIN32_OWN_PROCESS;
        serviceStatus.dwCurrentState = Winsvc.SERVICE_STOP_PENDING;
        serviceStatus.dwWaitHint = stopWaitHint;
        setStatus(serviceStatus);
    }

    protected void setStatusStopped() {
        setStatusStopped(WinError.NO_ERROR, 0);
    }

    protected void setStatusStopped(int win32ExitCode, int dwServiceSpecificExitCode) {
        SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
        serviceStatus.dwServiceType = WinNT.SERVICE_WIN32_OWN_PROCESS;
        serviceStatus.dwCurrentState = Winsvc.SERVICE_STOPPED;
        serviceStatus.dwWin32ExitCode = win32ExitCode;
        if (win32ExitCode == WinError.ERROR_SERVICE_SPECIFIC_ERROR) {
            serviceStatus.dwServiceSpecificExitCode = dwServiceSpecificExitCode;
        }
        setStatus(serviceStatus);
    }

    protected void setStatus(SERVICE_STATUS serviceStatus) {
        Advapi32.INSTANCE.SetServiceStatus(serviceStatusHandle, serviceStatus);
    }

    protected abstract void startImpl() throws Exception;
    protected abstract void stopImpl() throws Exception;

    protected void serviceMain(int dwArgc, Pointer lpszArgv) {
        try {
            SERVICE_STATUS_HANDLE serviceStatusHandle = Advapi32.INSTANCE.RegisterServiceCtrlHandlerEx(serviceName, serviceControlHandler, null);
            if (serviceStatusHandle == null) {
                return;
            }
            this.serviceStatusHandle = serviceStatusHandle;

            Thread.currentThread().setName("Service " + serviceName + " main");

            setStatusStartPending();
            startImpl();
            setStatusRunning();

        } catch (Throwable t) {
            setStatusStopped(WinError.ERROR_SERVICE_SPECIFIC_ERROR, 1);
        }
    }

    protected void terminate() {
        new Thread(() -> {
            try {
                setStatusStopPending();
                stopImpl();
                setStatusStopped();
            } catch (Throwable t) {
                setStatusStopped(WinError.ERROR_SERVICE_SPECIFIC_ERROR, 1);
            }
        }, "Service " + serviceName + " terminate").start();
    }

    protected int serviceControl(int dwControl, int dwEventType, Pointer lpEventData, Pointer lpContext) {
        switch (dwControl) {
            case Winsvc.SERVICE_CONTROL_STOP:
            case Winsvc.SERVICE_CONTROL_SHUTDOWN:
                terminate();
                return WinError.NO_ERROR;
            case Winsvc.SERVICE_CONTROL_INTERROGATE:
                return WinError.NO_ERROR;
            default:
                return WinError.ERROR_CALL_NOT_IMPLEMENTED;
        }
    }

    protected void throwLastError() {
        int error = Native.getLastError();
        String message = Kernel32Util.formatMessage(error);
        throw new LastErrorException(String.format("GetLastError() returned %d: %s", error, message));
    }

    /**
     * Install the service.
     *
     * @param serviceName  service name
     * @param displayName  visible name
     * @param description  description
     * @param dependencies array of other services to depend on or null
     * @param account      service account or null for LocalSystem
     * @param password     password for service account or null
     * @param command      command line to start the service
     */
    public static void install(String serviceName, String displayName, String description, String[] dependencies, String account, String password, String command) {
        try (ServiceControlManager sc = new ServiceControlManager(null, null, Winsvc.SC_MANAGER_ALL_ACCESS)) {
            try (Service service = sc.createService(serviceName, displayName, Winsvc.SERVICE_ALL_ACCESS, WinNT.SERVICE_WIN32_OWN_PROCESS, WinNT.SERVICE_AUTO_START,
                    WinNT.SERVICE_ERROR_NORMAL, command, null, null, dependencies, account, password)) {
                service.setDescription(description);
            }
        }
    }

    /**
     * Uninstall the service.
     */
    public static void uninstall(String serviceName) {
        try (ServiceControlManager sc = new ServiceControlManager(null, null, Winsvc.SC_MANAGER_ALL_ACCESS)) {
            try (Service service = sc.openService(serviceName, Winsvc.SERVICE_ALL_ACCESS)) {
                service.delete();
            }
        }
    }

    /**
     * Ask the ServiceControlManager to start the service.
     */
    public static void start(String serviceName) {
        try (ServiceControlManager sc = new ServiceControlManager(null, null, WinNT.GENERIC_EXECUTE)) {
            try (Service service = sc.openService(serviceName, WinNT.GENERIC_EXECUTE)) {
                service.start(0, null);
            }
        }
    }

    /**
     * Ask the ServiceControlManager to stop the service.
     */
    public static void stop(String serviceName) {
        try (ServiceControlManager sc = new ServiceControlManager(null, null, WinNT.GENERIC_EXECUTE)) {
            try (Service service = sc.openService(serviceName, WinNT.GENERIC_EXECUTE)) {
                service.stop();
            }
        }
    }

}