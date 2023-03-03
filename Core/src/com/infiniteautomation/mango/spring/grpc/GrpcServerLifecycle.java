/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.grpc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.serotonin.m2m2.Common;

import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.TlsServerCredentials;
import io.grpc.TlsServerCredentials.ClientAuth;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;

/**
 * @author Jared Wiltshire
 */
@Component
@ConditionalOnProperty("${grpc.server.enabled}")
public class GrpcServerLifecycle {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<BindableService> services;
    private final List<ServerInterceptor> interceptors;
    private final int port;
    private final @Nullable Path certChain;
    private final @Nullable Path privateKey;
    private final @Nullable Path rootCerts;
    private final ClientAuth clientAuth;
    private final @Nullable String inProcessServer;
    private final boolean enableReflection;
    private Server server;

    @Autowired
    public GrpcServerLifecycle(List<BindableService> services,
                               List<ServerInterceptor> interceptors,
                               @Value("${grpc.server.port}") int port,
                               @Value("${grpc.server.certChain:#{null}}") @Nullable Path certChain,
                               @Value("${grpc.server.privateKey:#{null}}") @Nullable Path privateKey,
                               @Value("${grpc.server.rootCerts:#{null}}") @Nullable Path rootCerts,
                               @Value("${grpc.server.clientAuth}") ClientAuth clientAuth,
                               @Value("${grpc.server.inProcessServer:#{null}}") @Nullable String inProcessServer,
                               @Value("${grpc.server.enableReflection}") boolean enableReflection) {
        this.services = services;
        this.interceptors = interceptors;
        this.port = port;
        this.certChain = certChain == null ? null : Common.MA_DATA_PATH.resolve(certChain).normalize();
        this.privateKey = privateKey == null ? null : Common.MA_DATA_PATH.resolve(privateKey).normalize();
        this.rootCerts = rootCerts == null ? null : Common.MA_DATA_PATH.resolve(rootCerts).normalize();
        this.clientAuth = clientAuth;
        this.inProcessServer = inProcessServer;
        this.enableReflection = enableReflection;
    }

    @PostConstruct
    synchronized void postConstruct() throws IOException {
        ServerCredentials serverCredentials;
        if (certChain != null && privateKey != null) {
            var tlsBuilder = TlsServerCredentials.newBuilder()
                    .keyManager(certChain.toFile(), privateKey.toFile())
                    .clientAuth(clientAuth);
            if (rootCerts != null) {
                tlsBuilder.trustManager(rootCerts.toFile());
            }
            serverCredentials = tlsBuilder.build();
        } else {
            log.warn("gRPC server is running without TLS! " +
                    "You must supply a certificate chain and private key file to enable TLS.");
            serverCredentials = InsecureServerCredentials.create();
        }

        var builder = inProcessServer != null ?
                InProcessServerBuilder.forName(inProcessServer) :
                Grpc.newServerBuilderForPort(port, serverCredentials);
        builder.intercept(TransmitStatusRuntimeExceptionInterceptor.instance());

        // interceptors run in the reverse order of which they were added, reverse the list so the highest priority
        // (via Order annotation, or Ordered interface) interceptor runs first.
        var reverseInterceptors = new ArrayList<>(interceptors);
        Collections.reverse(reverseInterceptors);

        reverseInterceptors.stream()
                .filter(interceptor -> !interceptor.getClass().isAnnotationPresent(InterceptorFor.class))
                .forEach(builder::intercept);

        if (enableReflection) {
            builder.addService(ProtoReflectionService.newInstance());
        }

        services.stream()
                .map(service -> {
                    var matching = reverseInterceptors.stream()
                            .filter(interceptor -> {
                                var annotation = interceptor.getClass().getAnnotation(InterceptorFor.class);
                                if (annotation == null) {
                                    return false;
                                }
                                var classes = annotation.value();
                                return Arrays.stream(classes)
                                        .anyMatch(v -> v.isAssignableFrom(service.getClass()));
                            })
                            .collect(Collectors.toList());
                    return ServerInterceptors.intercept(service, matching);
                })
                .forEach(builder::addService);

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
