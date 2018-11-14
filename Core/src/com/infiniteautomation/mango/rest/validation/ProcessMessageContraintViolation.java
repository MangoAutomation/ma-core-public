/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.validation;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

import com.serotonin.m2m2.i18n.ProcessMessage;

/**
 * @author Terry Packer
 *
 */
public class ProcessMessageContraintViolation<T> implements ConstraintViolation<T> {

    private final ProcessMessage message;
    private final T root;
    
    ProcessMessageContraintViolation(ProcessMessage message, T root){
        this.message =  message;
        this.root = root;
    }

    public ProcessMessage getProcessMessage() {
        return message;
    }
    
    @Override
    public String getMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMessageTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public T getRootBean() {
        return root;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getRootBeanClass() {
        return (Class<T>) root.getClass();
    }

    @Override
    public Object getLeafBean() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object[] getExecutableParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getExecutableReturnValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path getPropertyPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getInvalidValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <U> U unwrap(Class<U> type) {
        // TODO Auto-generated method stub
        return null;
    }
}
