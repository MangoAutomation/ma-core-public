/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.grpc;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall.Listener;

/**
 * Sets the authentication into the Spring Security context when calling gRPC service methods.
 *
 * @author Jared Wiltshire
 */
public class AuthenticationServerCallListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    private final Authentication authentication;

    public AuthenticationServerCallListener(Listener<ReqT> listener, Authentication authentication) {
        super(listener);
        this.authentication = authentication;
    }

    private void withAuthentication(Runnable command) {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContext newContext = SecurityContextHolder.createEmptyContext();
            newContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(newContext);
            command.run();
        } finally {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if (emptyContext.equals(original)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(original);
            }
        }
    }

    @Override
    public void onMessage(ReqT message) {
        withAuthentication(() -> super.onMessage(message));
    }

    @Override
    public void onHalfClose() {
        withAuthentication(super::onHalfClose);
    }

    @Override
    public void onCancel() {
        withAuthentication(super::onCancel);
    }

    @Override
    public void onComplete() {
        withAuthentication(super::onComplete);
    }

    @Override
    public void onReady() {
        withAuthentication(super::onReady);
    }
}
