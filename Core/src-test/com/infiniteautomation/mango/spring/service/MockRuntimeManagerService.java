/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.service;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class MockRuntimeManagerService {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder
                .forPort(8002)
                .addService(new RuntimeManagerService()).build();

        server.start();
        server.awaitTermination();

    }
}
