package com.serotonin.util;

public class ThreadUtils {
    public static void sleep(long millis) {
        if (millis <= 0)
            return;

        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void wait(Object monitor) {
        try {
            monitor.wait();
        }
        catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void wait(Object monitor, long timeout) {
        try {
            monitor.wait(timeout);
        }
        catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void wait(Object monitor, long timeout, int nanos) {
        try {
            monitor.wait(timeout, nanos);
        }
        catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void waitSync(Object monitor) {
        synchronized (monitor) {
            try {
                monitor.wait();
            }
            catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }
    }

    public static void waitSync(Object monitor, long timeout) {
        synchronized (monitor) {
            try {
                monitor.wait(timeout);
            }
            catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }
    }

    public static void waitSync(Object monitor, long timeout, int nanos) {
        synchronized (monitor) {
            try {
                monitor.wait(timeout, nanos);
            }
            catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }
    }

    public static void notifySync(Object monitor) {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public static void notifyAllSync(Object monitor) {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public static void join(Thread thread) {
        try {
            thread.join();
        }
        catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void join(Thread thread, long timeout) {
        try {
            thread.join(timeout);
        }
        catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void join(Thread thread, long timeout, int nanos) {
        try {
            thread.join(timeout, nanos);
        }
        catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    static class UncheckedInterruptedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UncheckedInterruptedException(Throwable cause) {
            super(cause);
        }
    }
}
