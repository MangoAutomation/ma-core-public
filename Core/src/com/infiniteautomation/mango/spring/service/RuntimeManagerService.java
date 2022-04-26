/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.service;

import org.springframework.stereotype.Service;

import com.radixiot.pi.grpc.DataPointRT;
import com.radixiot.pi.grpc.DataPointVO;
import com.radixiot.pi.grpc.RuntimeManagerGrpc;

import io.grpc.stub.StreamObserver;

@Service
public class RuntimeManagerService extends RuntimeManagerGrpc.RuntimeManagerImplBase {

    @Override
    public void getDataPoint(DataPointVO request, StreamObserver<DataPointRT> responseObserver) {

        responseObserver.onNext(DataPointRT.newBuilder().setId(request.getId()).build());
        responseObserver.onCompleted();

    }

}
