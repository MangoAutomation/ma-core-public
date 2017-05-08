package com.serotonin.util.properties;

import java.util.Properties;

public interface ReloadCallback {
    void propertiesReload(Properties oldProps, Properties newProps);
}
