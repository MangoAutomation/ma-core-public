package com.serotonin.util;

public class ExceptionUtils {
    public static Throwable getRootCause(Throwable t) {
        if (t == null)
            return null;
        while (t.getCause() != null)
            t = t.getCause();
        return t;
    }

    public static String getNonNullRootMessage(Throwable t) {
        if (t == null)
            return null;

        t = getRootCause(t);
        String message = t.getMessage();

        if (message == null)
            message = t.getClass().getName();

        return message;
    }
}
