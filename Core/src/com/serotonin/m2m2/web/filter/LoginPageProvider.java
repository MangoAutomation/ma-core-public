package com.serotonin.m2m2.web.filter;

import com.serotonin.provider.Provider;

public interface LoginPageProvider extends Provider {
    void setForwardUri(String forwardUrl);

    String getForwardUri();
}
