/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap.windows;

import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.Winsvc.SC_ACTION;
import com.sun.jna.platform.win32.Winsvc.SC_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_DESCRIPTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_FAILURE_ACTIONS;
import com.sun.jna.platform.win32.Winsvc.SERVICE_FAILURE_ACTIONS_FLAG;
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

        public void setFailureActions(List<SC_ACTION> actions, int resetPeriod, String rebootMsg, String command) {
            SERVICE_FAILURE_ACTIONS.ByReference actionStruct = new SERVICE_FAILURE_ACTIONS.ByReference();
            actionStruct.dwResetPeriod = resetPeriod;
            actionStruct.lpRebootMsg = rebootMsg;
            actionStruct.lpCommand = command;
            actionStruct.cActions = actions.size();

            actionStruct.lpsaActions = new SC_ACTION.ByReference();
            SC_ACTION[] actionArray = (SC_ACTION[]) actionStruct.lpsaActions.toArray(actions.size());

            int i = 0;
            for (SC_ACTION action : actions) {
                actionArray[i].type = action.type;
                actionArray[i].delay = action.delay;
                i++;
            }

            if (!Advapi32.INSTANCE.ChangeServiceConfig2(serviceHandle, Winsvc.SERVICE_CONFIG_FAILURE_ACTIONS, actionStruct)) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
        }

        public void setFailureActionsFlag(boolean flagValue) {
            SERVICE_FAILURE_ACTIONS_FLAG flag = new SERVICE_FAILURE_ACTIONS_FLAG();
            flag.fFailureActionsOnNonCrashFailures = flagValue ? 1 : 0;

            if (!Advapi32.INSTANCE.ChangeServiceConfig2(serviceHandle, Winsvc.SERVICE_CONFIG_FAILURE_ACTIONS_FLAG, flag)) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
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
