/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.provider.impl;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

import com.serotonin.epoll.InputStreamEPoll;
import com.serotonin.provider.InputStreamEPollProvider;

/**
 * 
 * Provide access to the shared InputStream ePoll
 *
 */
public class InputStreamEPollProviderImpl implements InputStreamEPollProvider {
    private InputStreamEPoll inputStreamEPoll;

    @Override
    public void initialize() {
        // no op
    }

    @Override
    public void terminate() {
        if (inputStreamEPoll != null)
            inputStreamEPoll.terminate();
    }

    @Override
    public InputStreamEPoll getInputStreamEPoll() {
        if (inputStreamEPoll == null) {
            synchronized (this) {
                if (inputStreamEPoll == null) {
                    inputStreamEPoll = new InputStreamEPoll();

                    Thread thread = new Thread(new DelegatingSecurityContextRunnable(inputStreamEPoll),
                            inputStreamEPoll.getClass().getSimpleName());
                    thread.setPriority(Thread.MAX_PRIORITY);
                    thread.start();
                }
            }
        }

        return inputStreamEPoll;
    }
}
