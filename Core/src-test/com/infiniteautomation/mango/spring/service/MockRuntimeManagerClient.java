/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.service;

import com.radixiot.pi.grpc.MangoDataPointRT;
import com.radixiot.pi.grpc.MangoDataPointVO;
import com.radixiot.pi.grpc.RuntimeManagerGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class MockRuntimeManagerClient {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8002)
                .usePlaintext()
                .build();

        RuntimeManagerGrpc.RuntimeManagerBlockingStub stub
                = RuntimeManagerGrpc.newBlockingStub(channel);

        MangoDataPointVO request = MangoDataPointVO.newBuilder().setId(1).build();
        MangoDataPointRT dprt = stub.getDataPoint(request);

        channel.shutdown();
    }

}
