package com.serotonin.epoll;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;

import com.serotonin.io.StreamUtils;

/**
 * Synchronous process monitoring. The constructor blocks until the process
 * completes or times out.
 * 
 * @author Matthew Lohbihler
 */
public class ProcessMonitor {
    private final String out;
    private final String err;

    public ProcessMonitor(ProcessBuilder pb, ExecutorService executorService, long timeout)
            throws InterruptedException, IOException {
        this(pb.start(), executorService, timeout);
    }

    public ProcessMonitor(Process process, ExecutorService executorService, long timeout) throws InterruptedException {
        InputReader out = new InputReader(process.getInputStream());
        InputReader err = new InputReader(process.getErrorStream());

        executorService.execute(out);
        executorService.execute(err);

        ProcessTimeout processTimeout = null;
        if (timeout > 0) {
            processTimeout = new ProcessTimeout(process, timeout);
            executorService.execute(processTimeout);
        }

        process.waitFor();
        out.join();
        err.join();
        process.destroy();

        // If we've made it this far, the process exited properly, so kill the
        // timeout thread if it exists.
        if (processTimeout != null)
            processTimeout.interrupt();

        this.out = out.getInput();
        this.err = err.getInput();
    }

    public String getOut() {
        return out;
    }

    public String getErr() {
        return err;
    }

    class InputReader implements Runnable {
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

        public void run() {
            try {
                StreamUtils.transfer(reader, writer);
            }
            catch (IOException e) {
                e.printStackTrace(new PrintWriter(writer));
            }
            finally {
                synchronized (this) {
                    done = true;
                    notifyAll();
                }
            }
        }
    }

    class ProcessTimeout implements Runnable {
        private final Process process;
        private final long timeout;
        private volatile boolean interrupted;

        ProcessTimeout(Process process, long timeout) {
            this.process = process;
            this.timeout = timeout;
        }

        public void interrupt() {
            synchronized (this) {
                interrupted = true;
                notifyAll();
            }
        }

        public void run() {
            try {
                synchronized (this) {
                    wait(timeout);
                }

                if (!interrupted) {
                    // If the sleep time has expired, destroy the process.
                    process.destroy();
                }
            }
            catch (InterruptedException e) {
                /* no op */
            }
        }
    }
}
