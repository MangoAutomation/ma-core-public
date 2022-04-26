/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.service;

import com.radixiot.pi.grpc.DataPointRT;
import com.radixiot.pi.grpc.DataPointVO;
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

        DataPointVO request = DataPointVO.newBuilder().setId(1).build();
        DataPointRT dprt = stub.getDataPoint(request);

        channel.shutdown();
    }

}
