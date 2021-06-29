/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.email;

/**
 * @author Terry Packer
 *
 */
public abstract class PostEmailRunnable implements Runnable {

    private boolean success;
    private Exception failure;

    protected boolean isSuccess(){
        return success;
    }

    protected Exception getFailure(){
        return failure;
    }


    /**
     * When email is finished we save state and run
     * @param success
     * @param failure
     */
    public void emailFinished(boolean success, Exception failure){
        this.success = success;
        this.failure = failure;

        this.run();
    }


}
