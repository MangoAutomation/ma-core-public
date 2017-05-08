package com.serotonin.epoll;

/**
 * Process handler that buffers the input/error streams and makes them available to the subclass.
 * 
 * @author Matthew Lohbihler
 */
abstract public class BufferingHandler extends ProcessHandler {
    private final StringBuilder input = new StringBuilder();
    private final StringBuilder error = new StringBuilder();

    public void input(String s) {
        input.append(s);
    }

    public void error(String s) {
        error.append(s);
    }

    public String getInput() {
        return input.toString();
    }

    public String getError() {
        return error.toString();
    }
}
