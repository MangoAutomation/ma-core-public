package com.serotonin.epoll;

import java.io.IOException;

import com.serotonin.epoll.ProcessHandler.DoneCause;

public class ProcessEpollUtils {
    public static void executeAndWait(ProcessEPoll pep, long timeout, String... command) throws IOException {
        executeAndWait(pep, timeout, new ProcessBuilder(command));
    }

    public static void executeAndWait(ProcessEPoll pep, long timeout, ProcessBuilder pb) throws IOException {
        long id = pep.add(pb, timeout, null);
        pep.waitFor(id);
    }

    public static String getProcessInput(ProcessEPoll pep, long timeout, String... command) throws IOException {
        return getProcessInput(pep, timeout, new ProcessBuilder(command));
    }

    /**
     * Runs the process and blocks the thread (up to the timeout)
     * 
     * @param pep
     * @param timeout
     * @param command
     * @return
     * @throws IOException
     */
    public static String getProcessInput(ProcessEPoll pep, long timeout, ProcessBuilder pb) throws IOException {
        ResultHandler h = new ResultHandler();

        long id = pep.add(pb, timeout, h);
        pep.waitFor(id);

        if (h.cause != DoneCause.FINISHED)
            throw new ProcessEpollException(h.e, h.cause, h.exitValue);

        return h.input;
    }

    static class ResultHandler extends BufferingHandler {
        DoneCause cause;
        int exitValue;
        Exception e;
        String input;
        String error;

        @Override
        public void done(DoneCause cause, int exitValue, Exception e) {
            this.cause = cause;
            this.exitValue = exitValue;
            this.e = e;
            input = getInput();
            error = getError();
        }
    }
}
