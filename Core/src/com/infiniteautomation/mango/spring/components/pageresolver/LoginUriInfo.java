/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components.pageresolver;

/**
 * Container for login URI info
 *
 * @author Terry Packer
 */
public class LoginUriInfo {

    /**
     * Default uri for a user to login to
     */
    private final String uri;

    /**
     * Does this URI need to be accessed to update a license agreement or other setting
     */
    private final boolean required;

    public LoginUriInfo(String uri, boolean required) {
        this.uri = uri;
        this.required = required;
    }

    public String getUri() {
        return uri;
    }

    public boolean isRequired() {
        return required;
    }
}
