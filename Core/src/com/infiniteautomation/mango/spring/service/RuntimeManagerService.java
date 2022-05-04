/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.service;

import org.springframework.stereotype.Service;

import com.radixiot.pi.grpc.MangoDataPointRT;
import com.radixiot.pi.grpc.MangoDataPointVO;
import com.radixiot.pi.grpc.RuntimeManagerGrpc;

import io.grpc.stub.StreamObserver;

@Service
public class RuntimeManagerService extends RuntimeManagerGrpc.RuntimeManagerImplBase {

    @Override
    public void getDataPoint(MangoDataPointVO request, StreamObserver<MangoDataPointRT> responseObserver) {

        responseObserver.onNext(MangoDataPointRT.newBuilder().setId(request.getId()).build());
        responseObserver.onCompleted();

    }

}
