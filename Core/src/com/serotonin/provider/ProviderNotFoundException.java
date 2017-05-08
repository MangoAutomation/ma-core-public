package com.serotonin.provider;

public class ProviderNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String providerId;

    public ProviderNotFoundException(String providerId) {
        super(providerId);
        this.providerId = providerId;
    }

    public String getProviderId() {
        return providerId;
    }
}
