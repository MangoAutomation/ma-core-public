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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.bootstrap.windows.ServiceControlManager.Service;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI.SHELLEXECUTEINFO;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.Winsvc.HandlerEx;
import com.sun.jna.platform.win32.Winsvc.SC_ACTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_MAIN_FUNCTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_TABLE_ENTRY;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

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
    }

    /**
     * Initialize the service, connect to the ServiceControlManager.
     */
    public void init() {
        Thread.currentThread().setName("Service " + serviceName + " control handler");
        SERVICE_TABLE_ENTRY entry = new SERVICE_TABLE_ENTRY();
        entry.lpServiceName = serviceName;
        entry.lpServiceProc = serviceMainFunction;

        if (!Advapi32.INSTANCE.StartServiceCtrlDispatcher((SERVICE_TABLE_ENTRY[]) entry.toArray(2))) {
            throw new Win32Exception(Native.getLastError());
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
    public static void install(String serviceName, String displayName, String description, String[] dependencies, String account, String password, String command, boolean restartOnFail) {
        try (ServiceControlManager sc = new ServiceControlManager(null, null, Winsvc.SC_MANAGER_ALL_ACCESS)) {
            try (Service service = sc.createService(serviceName, displayName, Winsvc.SERVICE_ALL_ACCESS, WinNT.SERVICE_WIN32_OWN_PROCESS, WinNT.SERVICE_AUTO_START,
                    WinNT.SERVICE_ERROR_NORMAL, command, null, null, dependencies, account, password)) {
                service.setDescription(description);
                if (restartOnFail) {
                    List<SC_ACTION> actions = new ArrayList<>(7);
                    actions.add(action(0x01, 0)); // SC_ACTION_RESTART, 0 second delay
                    actions.add(action(0x01, 0)); // SC_ACTION_RESTART, 0 second delay
                    actions.add(action(0x01, 0)); // SC_ACTION_RESTART, 0 second delay
                    actions.add(action(0x01, 30000)); // SC_ACTION_RESTART, 30 second delay
                    actions.add(action(0x01, 60000)); // SC_ACTION_RESTART, 60 second delay
                    actions.add(action(0x01, 120000)); // SC_ACTION_RESTART, 120 second delay
                    actions.add(action(0x00, 0)); // SC_ACTION_NONE, give up

                    // failure count resets after 15 minutes
                    service.setFailureActions(actions, 900, null, null);

                    // trigger failure actions if service stops with error code (default is to only run when process terminates unexpectedly)
                    service.setFailureActionsFlag(true);
                }
            }
        }
    }

    public static void restartAsAdmin() {
        String cmd = Kernel32Ext.INSTANCE.GetCommandLine();
        if (cmd == null) {
            throw new Win32Exception(Native.getLastError());
        }

        IntByReference arraySize = new IntByReference();
        Pointer arrayPtr = Shell32Ext.INSTANCE.CommandLineToArgv(cmd, arraySize.getPointer());
        if (arrayPtr == null) {
            throw new Win32Exception(Native.getLastError());
        }
        String[] paramArray = arrayPtr.getWideStringArray(0, arraySize.getValue());
        String params = Arrays.stream(paramArray).skip(1).collect(Collectors.joining(" "));

        SHELLEXECUTEINFO info = new SHELLEXECUTEINFO();
        info.lpVerb = "runas";
        info.lpFile = paramArray[0];
        info.lpParameters = params;
        info.fMask = 0x00000100 | 0x00008000;
        //info.fMask = SEE_MASK_NOASYNC | SEE_MASK_NO_CONSOLE;

        if (!Shell32.INSTANCE.ShellExecuteEx(info)) {
            throw new Win32Exception(Native.getLastError());
        }

        // wait for process to complete
        // set SEE_MASK_NOCLOSEPROCESS 0x00000040
        //        IntByReference exitCode = new IntByReference();
        //        Kernel32.INSTANCE.WaitForSingleObject(info.hProcess, 30000);
        //        Kernel32.INSTANCE.GetExitCodeProcess(info.hProcess, exitCode);
        //        System.out.println(exitCode.getValue());
        //        Kernel32.INSTANCE.CloseHandle(info.hProcess);
    }

    private static SC_ACTION action(int type, int delay) {
        SC_ACTION action = new SC_ACTION();
        action.type = type;
        action.delay = delay;
        return action;
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

    public interface Kernel32Ext extends Kernel32 {
        Kernel32Ext INSTANCE = Native.load("kernel32", Kernel32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        String GetCommandLine();
    }

    public interface Shell32Ext extends Shell32 {
        Shell32Ext INSTANCE = Native.load("shell32", Shell32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer CommandLineToArgv(String commandLine, Pointer numArgs);
    }

}