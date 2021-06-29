/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import com.serotonin.m2m2.module.SystemInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.OperatingSystemInfoDefinition.OsInfo;

/**
 * Class to define Read only settings/information that can be provided
 *
 * @author Terry Packer
 */
public class OperatingSystemInfoDefinition extends SystemInfoDefinition<OsInfo>{

    public final String KEY = "osInfo";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public OsInfo getValue() {
        OsInfo info = new OsInfo();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if(osBean != null){
            info.setArchitecture(osBean.getArch());
            info.setOperatingSystem(osBean.getName());
            info.setOsVersion(osBean.getVersion());
        }
        return info;
    }

    public class OsInfo{
        private String architecture = "unknown";
        private String operatingSystem = "unknown";
        private String osVersion = "unknown";

        public String getArchitecture() {
            return architecture;
        }
        public void setArchitecture(String architecture) {
            this.architecture = architecture;
        }
        public String getOperatingSystem() {
            return operatingSystem;
        }
        public void setOperatingSystem(String operatingSystem) {
            this.operatingSystem = operatingSystem;
        }
        public String getOsVersion() {
            return osVersion;
        }
        public void setOsVersion(String version) {
            this.osVersion = version;
        }
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.osInfoDesc";
    }

}
