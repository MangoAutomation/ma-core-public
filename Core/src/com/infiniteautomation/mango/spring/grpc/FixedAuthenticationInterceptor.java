/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.grpc;

import org.springframework.security.core.Authentication;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Interceptor that sets a fixed {@link Authentication} object into the Spring Security context.
 *
 * @author Jared Wiltshire
 */
public class FixedAuthenticationInterceptor implements ServerInterceptor {

    private final Authentication authentication;

    public FixedAuthenticationInterceptor(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return new AuthenticationServerCallListener<>(next.startCall(call, headers), authentication);
    }
}
