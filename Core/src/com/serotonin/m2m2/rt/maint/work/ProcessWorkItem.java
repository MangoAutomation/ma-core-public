/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint.work;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.SystemEventType;

/**
 * @author Matthew Lohbihler
 */
public class ProcessWorkItem implements WorkItem {
    static final Log LOG = LogFactory.getLog(ProcessWorkItem.class);

    public static void queueProcess(String command, int timeoutSeconds) {
        ProcessWorkItem item = new ProcessWorkItem(command, timeoutSeconds);
        Common.backgroundProcessing.addWorkItem(item);
    }

    final String command;
    final int timeoutSeconds;

    public ProcessWorkItem(String command, int timeoutSeconds) {
        this.command = command;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public void execute() {
        try {
            executeProcessCommand(command, timeoutSeconds);
        }
        catch (IOException e) {
            SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_PROCESS_FAILURE),
                    System.currentTimeMillis(), false,
                    new TranslatableMessage("event.process.failure", command, e.getMessage()));
        }
    }

    public static void executeProcessCommand(String command, int timeoutSeconds) throws IOException {
        Process process = Runtime.getRuntime().exec(command);

        InputReader out = new InputReader(process.getInputStream());
        InputReader err = new InputReader(process.getErrorStream());

        Common.backgroundProcessing.addWorkItem(out);
        Common.backgroundProcessing.addWorkItem(err);

        try {
            ProcessTimeout timeout = new ProcessTimeout(process, command, timeoutSeconds);
            Common.backgroundProcessing.addWorkItem(timeout);

            process.waitFor();
            out.join();
            err.join();
            process.destroy();

            // If we've made it this far, the process exited properly, so kill the timeout thread if it exists.
            timeout.interrupt();

            String input = out.getInput();
            if (!StringUtils.isBlank(input))
                LOG.info("Process output: '" + input + "'");

            input = err.getInput();
            if (!StringUtils.isBlank(input))
                LOG.warn("Process error: '" + input + "'");
        }
        catch (InterruptedException e) {
            throw new IOException("Timeout while running command: '" + command + "'");
        }
    }

    @Override
    public int getPriority() {
        return WorkItem.PRIORITY_HIGH;
    }

    static class ProcessTimeout implements WorkItem {
        private final Process process;
        private final String command;
        private final int timeoutSeconds;
        private volatile boolean interrupted;

        ProcessTimeout(Process process, String command, int timeoutSeconds) {
            this.process = process;
            this.command = command;
            this.timeoutSeconds = timeoutSeconds;
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_HIGH;
        }

        public void interrupt() {
            synchronized (this) {
                interrupted = true;
                notifyAll();
            }
        }

        @Override
        public void execute() {
            try {
                synchronized (this) {
                    wait(timeoutSeconds * 1000);
                }

                if (!interrupted) {
                    // If the sleep time has expired, destroy the process.
                    LOG.warn("Timeout waiting for process to end. command=" + command);
                    process.destroy();
                }
            }
            catch (InterruptedException e) { /* no op */
            }
        }
    }

    static class InputReader implements WorkItem {
        private final InputStreamReader reader;
        private final StringWriter writer = new StringWriter();
        private boolean done;

        InputReader(InputStream is) {
            reader = new InputStreamReader(is);
        }

        public String getInput() {
            return writer.toString();
        }

        public void join() {
            synchronized (this) {
                if (!done) {
                    try {
                        wait();
                    }
                    catch (InterruptedException e) {
                        // no op
                    }
                }
            }
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_HIGH;
        }

        @Override
        public void execute() {
            try {
                StreamUtils.transfer(reader, writer);
            }
            catch (IOException e) {
                LOG.warn("Error in process input reader", e);
            }
            finally {
                synchronized (this) {
                    done = true;
                    notifyAll();
                }
            }
        }
    }
    //    
    // public static void main(String[] args) throws Exception {
    // // ServletContext ctx = new DummyServletContext();
    // BackgroundProcessing bp = new BackgroundProcessing();
    // bp.initialize();
    // // ctx.setAttribute(Common.ContextKeys.BACKGROUND_PROCESSING, bp);
    // // Common.ctx = new ContextWrapper(ctx);
    // // ProcessWorkItem.queueProcess("");
    // // bp.terminate();
    //        
    // // //ProcessBuilder pb = new ProcessBuilder("cmd /c dir");
    // // ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "dir");
    // // pb.redirectErrorStream(true);
    // // Process process = pb.start();
    // Process process = Runtime.getRuntime().exec("cmd /c java -version");
    //        
    // InputReader out = new InputReader(process.getInputStream());
    // InputReader err = new InputReader(process.getErrorStream());
    //        
    // bp.addWorkItem(out);
    // bp.addWorkItem(err);
    //        
    // process.waitFor();
    // out.join();
    // err.join();
    // process.destroy();
    // bp.terminate();
    //        
    // System.out.println("out: "+ out.getInput());
    // System.out.println("err: "+ err.getInput());
    // }
}