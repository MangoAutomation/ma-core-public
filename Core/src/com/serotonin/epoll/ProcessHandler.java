package com.serotonin.epoll;

abstract public class ProcessHandler implements ProcessCallback {
    public enum DoneCause {
        FINISHED, TIMEOUT, CANCELLED, TERMINATED, EXCEPTION;
    }

    abstract public void done(DoneCause cause, int exitValue, Exception e);

    public void finished(int exitValue) {
        done(DoneCause.FINISHED, exitValue, null);
    }

    public void timeout() {
        done(DoneCause.TIMEOUT, -1, null);
    }

    public void cancelled() {
        done(DoneCause.CANCELLED, -1, null);
    }

    public void exception(Exception e) {
        done(DoneCause.EXCEPTION, -1, e);
    }

    public void terminated() {
        done(DoneCause.TERMINATED, -1, null);
    }
}
