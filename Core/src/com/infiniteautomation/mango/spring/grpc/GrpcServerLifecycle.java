/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.grpc;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;

/**
 * @author Jared Wiltshire
 */
@Component
public class GrpcServerLifecycle {

    private final List<BindableService> services;
    private final List<ServerInterceptor> interceptors;
    private final int port;
    private Server server;

    @Autowired
    public GrpcServerLifecycle(List<BindableService> services,
                               List<ServerInterceptor> interceptors,
                               @Value("${grpc.server.port:9090}") int port) {
        this.services = services;
        this.interceptors = interceptors;
        this.port = port;
    }

    @PostConstruct
    synchronized void postConstruct() throws IOException {
        var builder = ServerBuilder.forPort(port);
        builder.intercept(TransmitStatusRuntimeExceptionInterceptor.instance());
        interceptors.forEach(builder::intercept);
        services.forEach(builder::addService);
        this.server = builder.build().start();
    }

    @PreDestroy
    synchronized void preDestroy() throws InterruptedException {
        if (server != null) {
            server.shutdown();
            server.awaitTermination();
            server = null;
        }
    }

}
