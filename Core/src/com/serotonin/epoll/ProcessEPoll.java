package com.serotonin.epoll;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;

/**
 * Asynchronous process execution. An arbitrary number of processes can be added to an instance of this class. The
 * associated callback instance will be notified of process events when they occur.
 * 
 * @author Matthew Lohbihler
 */
public class ProcessEPoll extends Task {
    /**
	 * @param name
	 */
	public ProcessEPoll() {
		super("ProcessEPoll");
	}

	static final Log LOG = LogFactory.getLog(ProcessEPoll.class);
    private static final Charset UTF8_CS = Charset.forName("UTF-8");

    private final List<ProcessWrapper> processes = new CopyOnWriteArrayList<ProcessWrapper>();
    private long nextId = 0;
    private volatile boolean terminated;
    private Thread thread;

    // Reusable buffers
    private final byte[] byteBuffer = new byte[1028];
    private final StringBuilder stringBuffer = new StringBuilder();

    public long add(ProcessBuilder processBuilder, long timeout, ProcessCallback callback) throws IOException {
        checkReentrance();

        if (callback == null)
            callback = new NullCallback();

        long id = -1;
        synchronized (this) {
            id = nextId++;
            processes.add(new ProcessWrapper(id, processBuilder, timeout, callback));
            // Ensure that the thread is notified since it may be waiting.
            notify();
        }

        return id;
    }

    /**
     * This utility must be started with a thread. To avoid race conditions where other threads may try to add processes
     * before this class is properly running, this method should be called.
     */
    public void waitUntilStarted() {
        synchronized (this) {
            try {
                if (!terminated && thread == null)
                    wait();
            }
            catch (InterruptedException e) {
                // no op
            }
        }
    }

    public void terminate() {
        checkReentrance();

        synchronized (this) {
            terminated = true;
            notify();
        }
    }

    public ProcessCallback getCallback(long id) {
        checkReentrance();

        synchronized (this) {
            for (ProcessWrapper pw : processes) {
                if (pw.id == id)
                    return pw.callback;
            }
        }
        return null;
    }

    public boolean cancel(long id) {
        checkReentrance();

        synchronized (this) {
            for (ProcessWrapper pw : processes) {
                if (pw.id == id) {
                    if (!pw.cancelled) {
                        pw.cancelled = true;
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    public void waitFor(long id) {
        checkReentrance();
        waitFor(id, 0);
    }

    public void waitFor(long id, long waitTime) {
        checkReentrance();

        ProcessWrapper processWrapper = null;
        synchronized (this) {
            for (ProcessWrapper pw : processes) {
                if (pw.id == id) {
                    processWrapper = pw;
                    break;
                }
            }
        }

        if (processWrapper == null)
            return;

        processWrapper.waitFor(waitTime);
    }

    public void waitForAll() {
        checkReentrance();

        List<ProcessWrapper> pws = new ArrayList<ProcessEPoll.ProcessWrapper>();
        synchronized (this) {
            pws.addAll(processes);
        }

        for (ProcessWrapper pw : pws)
            pw.waitFor(0);
    }

    public int getProcessCount() {
        return processes.size();
    }

    private void checkReentrance() {
        if (thread == null)
            throw new IllegalStateException("ProcessEPoll not started");
        if (thread == Thread.currentThread())
            throw new IllegalStateException("ProcessEPoll methods called directly from callback. Use another thread");
    }

    @Override
    public void run(long runtime) {
        try {
            synchronized (this) {
                thread = Thread.currentThread();
                notifyAll();
            }
            runImpl();
        }
        finally {
            thread = null;
        }
    }

    private void runImpl() {

        while (!terminated) {
            if (processes.isEmpty()) {
                // If there is nothing to do, wait until there is.
                synchronized (this) {
                    try {
                        // Just check again to be sure.
                        if (processes.isEmpty() && !terminated) {
                            wait();

                            if (terminated)
                                break;
                        }
                    }
                    catch (InterruptedException e) {
                        // no op
                    }
                }

            }

            long now = System.currentTimeMillis();
            boolean activity = false;
            for (ProcessWrapper pw : processes) {
                // Check the input streams
                try {
                    try {
                        String s = readStream(pw.process.getInputStream());
                        if (s != null) {
                            try {
                                pw.callback.input(s);
                            }
                            catch (Exception e) {
                                LOG.warn("Callback exception", e);
                            }
                            activity = true;
                        }

                        s = readStream(pw.process.getErrorStream());
                        if (s != null) {
                            try {
                                pw.callback.error(s);
                            }
                            catch (Exception e) {
                                LOG.warn("Callback exception", e);
                            }
                            activity = true;
                        }
                    }
                    catch (Exception e) {
                        pw.process.destroy();
                        try {
                            pw.callback.exception(e);
                        }
                        catch (Exception e2) {
                            LOG.warn("Callback exception", e2);
                        }
                        done(pw);
                        activity = true;
                    }

                    // Check for finished processes.
                    try {
                        int exitValue = pw.process.exitValue();

                        // If the above call did not throw an exception then the
                        // process is done.
                        try {
                            pw.callback.finished(exitValue);
                        }
                        catch (Exception e) {
                            LOG.warn("Callback exception", e);
                        }
                        done(pw);
                        activity = true;
                    }
                    catch (IllegalThreadStateException e) {
                        // Not done. Check for cancellation or timeout.
                        if (pw.cancelled || (pw.timeout > 0 && pw.timeout < now)) {
                            pw.process.destroy();
                            try {
                                if (pw.cancelled)
                                    pw.callback.cancelled();
                                else
                                    pw.callback.timeout();
                            }
                            catch (Exception e2) {
                                LOG.warn("Callback exception", e2);
                            }
                            done(pw);
                            activity = true;
                        }
                    }
                }
                catch (Exception e) {
                    // Perhaps a problem in the callback.
                    LOG.error("Process callback exception", e);
                }
            }

            if (!activity) {
                // If there was no activity, let's just wait for a bit.
                synchronized (this) {
                    try {
                        wait(20);
                    }
                    catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }

        // Destroy any remaining processes
        for (ProcessWrapper pw : processes) {
            pw.process.destroy();
            try {
                pw.callback.terminated();
            }
            catch (Exception e) {
                LOG.warn("Callback exception", e);
            }
            done(pw);
        }
    }

    private void done(ProcessWrapper pw) {
        synchronized (this) {
            processes.remove(pw);
        }
        pw.done();
    }

    private String readStream(InputStream in) throws IOException {
        int avail = in.available();
        if (avail == 0)
            return null;

        while (true) {
            int readCount = in.read(byteBuffer);
            stringBuffer.append(new String(byteBuffer, 0, readCount, UTF8_CS));

            if (in.available() == 0)
                break;
        }

        String result = stringBuffer.toString();
        stringBuffer.delete(0, stringBuffer.length());
        return result;
    }

    private class ProcessWrapper {
        final long id;
        final Process process;
        final long timeout;
        final ProcessCallback callback;
        volatile boolean cancelled;
        volatile boolean done;

        public ProcessWrapper(long id, ProcessBuilder processBuilder, long timeout, ProcessCallback callback)
                throws IOException {
            this.id = id;
            this.process = processBuilder.start();
            if (timeout <= 0)
                this.timeout = 0;
            else
                this.timeout = System.currentTimeMillis() + timeout;
            this.callback = callback;
        }

        void waitFor(long waitTime) {
            synchronized (this) {
                if (done)
                    return;

                try {
                    wait(waitTime);
                }
                catch (InterruptedException e) {
                    // no op
                }
            }
        }

        void done() {
            synchronized (this) {
                done = true;
                notifyAll();
            }
        }
    }

    //    public static void main(String[] args) throws Exception {
    //        ProcessEPoll asyncProcesses = new ProcessEPoll();
    //        new Thread(asyncProcesses).start();
    //
    //        long id = asyncProcesses.add(new ProcessBuilder("script/proc2.bat"), 60000, new BufferingHandler() {
    //            @Override
    //            public void done(DoneCause cause, int exitValue, Exception e) {
    //                System.out.println("Done in callback");
    //            }
    //        });
    //
    //        System.out.println("wait 1");
    //        asyncProcesses.waitFor(id, 3000);
    //        System.out.println("wait 2");
    //        asyncProcesses.waitFor(id);
    //        System.out.println("wait 3");
    //        asyncProcesses.waitFor(id);
    //        System.out.println("Done waiting");
    //
    //        asyncProcesses.terminate();
    //
    //        // Tested with the following batch file (proc.bat):
    //        // echo off
    //        //
    //        // echo Sample process starting
    //        //
    //        // echo Sample process delay #1
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process delay #2
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process delay #3
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process delay #4
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process delay #5
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process ending
    //    }

    //    public static void main(String[] args) throws Exception {
    //        ProcessEPoll asyncProcesses = new ProcessEPoll();
    //        Thread thread = new Thread(asyncProcesses);
    //        thread.start();
    //
    //        asyncProcesses.add(new ProcessBuilder("script/proc.bat"), 60000, new Callback("1", asyncProcesses));
    //        long id = asyncProcesses.add(new ProcessBuilder("script/proc.bat"), 30000, new Callback("2", asyncProcesses));
    //        asyncProcesses.add(new ProcessBuilder("script/proc.bat"), 20000, new Callback("3", asyncProcesses));
    //        asyncProcesses.add(new ProcessBuilder("script/proc.bat"), 10000, new Callback("4", asyncProcesses));
    //
    //        int count = 80;
    //        while (asyncProcesses.getProcessCount() > 0) {
    //            count--;
    //            if (count == 0)
    //                asyncProcesses.cancel(id);
    //            Thread.sleep(200);
    //        }
    //
    //        asyncProcesses.terminate();
    //
    //        // Tested with the following batch file (proc.bat):
    //        // echo off
    //        //
    //        // echo Sample process starting
    //        //
    //        // echo Sample process delay #1
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process delay #2
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process delay #3
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process delay #4
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process delay #5
    //        // PING 1.1.1.1 -n 1 -w 5000 >NUL
    //        //
    //        // echo Sample process ending
    //    }

    static class NullCallback implements ProcessCallback {
        public void input(String s) {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback input: " + s);
        }

        public void error(String s) {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback error: " + s);
        }

        public void finished(int exitValue) {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback finished: " + exitValue);
        }

        public void timeout() {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback timeout");
        }

        public void cancelled() {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback cancelled");
        }

        public void exception(Exception e) {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback exception", e);
        }

        public void terminated() {
            if (LOG.isDebugEnabled())
                LOG.debug("NullCallback terminate");
        }
    }

    static class Callback implements ProcessCallback {
        private final String id;
        private final ProcessEPoll asyncProcesses;

        public Callback(String id, ProcessEPoll asyncProcesses) {
            this.id = id;
            this.asyncProcesses = asyncProcesses;
        }

        public void input(String s) {
            System.out.println(id + ") Input: " + s);
        }

        public void error(String s) {
            System.err.println(id + ") Error: " + s);
        }

        public void finished(int exitValue) {
            System.out.println(id + ") Finished: " + exitValue);
        }

        public void timeout() {
            System.err.println(id + ") Timeout");
            try {
                asyncProcesses.add(new ProcessBuilder("script/proc.bat"), 10000, new Callback("4", asyncProcesses));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancelled() {
            System.err.println(id + ") Cancelled");
        }

        public void exception(Exception e) {
            System.err.println(id + ") Exception");
            e.printStackTrace();
        }

        public void terminated() {
            System.err.println(id + ") Terminated");
        }
    }

	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.backgroundProcessing.rejectedHighPriorityTask(reason);
	}
}
