package com.serotonin.epoll;

/**
 * A callback interface for processes.
 * 
 * NOTE: if the AsyncProcesses instance is terminated, any running processes will be destroyed without any notification
 * to this callback.
 * 
 * @author Matthew Lohbihler
 */
public interface ProcessCallback {
    /**
     * Called when content is read from the process's input stream.
     * 
     * @param s
     *            the content that was read
     */
    void input(String s);

    /**
     * Called when content is read from the process's error stream.
     * 
     * @param s
     *            the content that was read
     */
    void error(String s);

    /**
     * Called when the process completes on its own.
     * 
     * @param exitValue
     *            the exit value that the process returned
     */
    void finished(int exitValue);

    /**
     * Called when the process times out. In this case the process will have been destroyed.
     */
    void timeout();

    /**
     * Called if the process was cancelled using the AsyncProcesses.cancel method.
     */
    void cancelled();

    /**
     * Called if there is an {@link Exception} while reading the process's input or error streams. In this case the
     * process will have been destroyed.
     * 
     * @param e
     *            the exception that was received
     */
    void exception(Exception e);

    /**
     * Called if the AsyncProcesses instance was terminated while the process was still running. The process will have
     * been destroyed.
     */
    void terminated();
}
