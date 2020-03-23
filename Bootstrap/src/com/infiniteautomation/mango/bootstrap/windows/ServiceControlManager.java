/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap.windows;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.Winsvc.SC_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_DESCRIPTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS;
import com.sun.jna.ptr.IntByReference;

/**
 * @author Jared Wiltshire
 */
public class ServiceControlManager implements AutoCloseable {

    private final SC_HANDLE handle;

    public ServiceControlManager(String lpMachineName, String lpDatabaseName, int dwDesiredAccess) {
        this.handle = Advapi32.INSTANCE.OpenSCManager(lpMachineName, lpDatabaseName, dwDesiredAccess);
        if (this.handle == null) {
            throwLastError();
        }
    }

    @Override
    public void close() {
        if (!Advapi32.INSTANCE.CloseServiceHandle(this.handle)) {
            throwLastError();
        }
    }

    private void throwLastError() {
        int error = Native.getLastError();
        String message = Kernel32Util.formatMessage(error);
        throw new LastErrorException(String.format("GetLastError() returned %d: %s", error, message));
    }

    public Service createService(String lpServiceName,
            String lpDisplayName, int dwDesiredAccess, int dwServiceType,
            int dwStartType, int dwErrorControl, String lpBinaryPathName,
            String lpLoadOrderGroup, IntByReference lpdwTagId,
            String[] dependencies, String lpServiceStartName, String lpPassword) {

        StringBuilder dep = new StringBuilder();
        if (dependencies != null) {
            for (String s : dependencies) {
                dep.append(s);
                dep.append("\0");
            }
        }
        dep.append("\0");

        SC_HANDLE serviceHandle = Advapi32.INSTANCE.CreateService(this.handle, lpServiceName, lpDisplayName,
                dwDesiredAccess, dwServiceType, dwStartType,
                dwErrorControl,
                lpBinaryPathName,
                lpLoadOrderGroup, lpdwTagId, dep.toString(), lpServiceStartName, lpPassword);
        if (serviceHandle == null) {
            throwLastError();
        }
        return new Service(serviceHandle);
    }

    public Service openService(String lpServiceName, int dwDesiredAccess) {
        SC_HANDLE serviceHandle = Advapi32.INSTANCE.OpenService(this.handle, lpServiceName, dwDesiredAccess);
        if (serviceHandle == null) {
            throwLastError();
        }
        return new Service(serviceHandle);
    }

    public class Service implements AutoCloseable {

        private final SC_HANDLE serviceHandle;

        public Service(SC_HANDLE serviceHandle) {
            this.serviceHandle = serviceHandle;
        }

        @Override
        public void close() {
            if (!Advapi32.INSTANCE.CloseServiceHandle(this.serviceHandle)) {
                throwLastError();
            }
        }

        public void setDescription(String description) {
            SERVICE_DESCRIPTION desc = new SERVICE_DESCRIPTION();
            desc.lpDescription = description;
            if (!Advapi32.INSTANCE.ChangeServiceConfig2(serviceHandle, Winsvc.SERVICE_CONFIG_DESCRIPTION, desc)) {
                throwLastError();
            }
        }

        public void delete() {
            if (!Advapi32.INSTANCE.DeleteService(serviceHandle)) {
                throwLastError();
            }
        }

        public void start(int dwNumServiceArgs, String[] lpServiceArgVectors) {
            if (!Advapi32.INSTANCE.StartService(serviceHandle, dwNumServiceArgs, lpServiceArgVectors)) {
                throwLastError();
            }
        }

        public void stop() {
            this.control(Winsvc.SERVICE_CONTROL_STOP);
        }

        public SERVICE_STATUS control(int dwControl) {
            SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
            if (!Advapi32.INSTANCE.ControlService(serviceHandle, dwControl, serviceStatus)) {
                throwLastError();
            }
            return serviceStatus;
        }
    }
}
