package com.serotonin.m2m2.util;

import java.util.ArrayList;
import java.util.List;

public class ExceptionListWrapper extends RuntimeException {
    private static final long serialVersionUID = -1L;
    
    List<Exception> exceptions;
    
    public ExceptionListWrapper(Exception e) {
        super();
        exceptions = new ArrayList<>(1);
        exceptions.add(e);
    }
    
    public ExceptionListWrapper(String message) {
        super(message);
    }
    
    public void addException(Exception e) {
        if(exceptions == null)
            exceptions = new ArrayList<>();
        exceptions.add(e);
    }
    
    public List<Exception> getExceptions() {
        return exceptions;
    }
}
