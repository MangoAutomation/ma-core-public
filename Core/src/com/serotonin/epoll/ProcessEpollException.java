package com.serotonin.epoll;

import java.io.IOException;

import com.serotonin.epoll.ProcessHandler.DoneCause;

public class ProcessEpollException extends IOException {
    private static final long serialVersionUID = 1L;

    private final DoneCause doneCause;
    private final int exitValue;

    public ProcessEpollException(DoneCause doneCause, int exitValue) {
        super();
        this.doneCause = doneCause;
        this.exitValue = exitValue;
    }

    public ProcessEpollException(Throwable cause, DoneCause doneCause, int exitValue) {
        super(cause);
        this.doneCause = doneCause;
        this.exitValue = exitValue;
    }

    public DoneCause getDoneCause() {
        return doneCause;
    }

    public int getExitValue() {
        return exitValue;
    }

    @Override
    public String getMessage() {
        return "doneCause=" + doneCause + ", exitValue=" + exitValue + ", msg=" + super.getMessage();
    }
}
