package com.serotonin.m2m2.util;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

/**
 * This class can be used by DWRs when starting an asynchronous process that requires session context, such as
 * knowledge of the user and - in the future - the current translations object.
 * 
 * @author Matthew
 */
abstract public class AsyncUserProcess implements Runnable {
    private final User user;

    public AsyncUserProcess() {
        user = Common.getUser();
    }

    @Override
    public void run() {
        try {
            BackgroundContext.set(user);
            runImpl();
        }
        finally {
            BackgroundContext.remove();
        }
    }

    abstract public void runImpl();
}
