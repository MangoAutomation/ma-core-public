package com.serotonin.json.util;

import java.io.IOException;

public class MaxCharacterCountExceededException extends IOException {
    private static final long serialVersionUID = 1L;

    public MaxCharacterCountExceededException() {
        super();
    }

    public MaxCharacterCountExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxCharacterCountExceededException(String message) {
        super(message);
    }

    public MaxCharacterCountExceededException(Throwable cause) {
        super(cause);
    }
}
