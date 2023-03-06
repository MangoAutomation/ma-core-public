/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.grpc;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusRuntimeException;

/**
 * Catches exceptions from service methods and downstream interceptors and handles them by closing the server call with
 * an appropriate status and metadata.
 *
 * @author Jared Wiltshire
 */
public class ErrorHandlingInterceptor implements ServerInterceptor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Function<Exception, StatusRuntimeException> errorHandler;

    protected ErrorHandlingInterceptor(Function<Exception, StatusRuntimeException> errorHandler) {
        this.errorHandler = errorHandler;
    }

    private <ReqT, RespT> void handleError(ServerCall<ReqT, RespT> call, Exception e) {
        log.trace("Handling error", e);
        var statusRuntimeException = errorHandler.apply(e);
        var trailers = statusRuntimeException.getTrailers();
        call.close(statusRuntimeException.getStatus(), trailers != null ? trailers : new Metadata());
    }

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        try {
            return new SimpleForwardingServerCallListener<>(next.startCall(call, headers)) {

                @Override
                public void onMessage(ReqT message) {
                    try {
                        super.onMessage(message);
                    } catch (Exception e) {
                        handleError(call, e);
                    }
                }

                @Override
                public void onHalfClose() {
                    try {
                        super.onHalfClose();
                    } catch (Exception e) {
                        handleError(call, e);
                    }
                }
            };
        } catch (Exception e) {
            handleError(call, e);
            return new Listener<>() {};
        }
    }

}
