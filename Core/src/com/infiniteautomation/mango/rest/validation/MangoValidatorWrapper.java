/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.validation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;

import org.hibernate.validator.internal.engine.ValidatorImpl;

import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.Validatable;

/**
 * @author Terry Packer
 *
 */
public class MangoValidatorWrapper implements Validator, ExecutableValidator {
    
    private ValidatorImpl delegate;
    
    public MangoValidatorWrapper(ValidatorImpl validator) {
        this.delegate = validator;
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorParameters(
            Constructor<? extends T> constructor, Object[] parameterValues, Class<?>... groups) {
        return delegate.validateConstructorParameters(constructor, parameterValues, groups);
    }
    
    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorReturnValue(
            Constructor<? extends T> constructor, T createdObject, Class<?>... groups) {
        return delegate.validateConstructorReturnValue(constructor, createdObject, groups);
    }
    
    @Override
    public <T> Set<ConstraintViolation<T>> validateParameters(T object, Method method,
            Object[] parameterValues, Class<?>... groups) {
        return delegate.validateParameters(object, method, parameterValues, groups);
    }
    
    @Override
    public <T> Set<ConstraintViolation<T>> validateReturnValue(T object, Method method,
            Object returnValue, Class<?>... groups) {
        return delegate.validateReturnValue(object, method, returnValue, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {

        Set<ConstraintViolation<T>> violations = delegate.validate(object, groups);

        if(object instanceof Validatable) {
            if(violations.isEmpty())
                violations = new HashSet<>();
            Validatable v = (Validatable)object;
            ProcessResult result = new ProcessResult();
            v.validate(result);
            for(ProcessMessage m : result.getMessages()) {
                violations.add(new ProcessMessageContraintViolation<T>(m, object));
            }
        }
        return violations;
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName,
            Class<?>... groups) {
        return delegate.validateProperty(object, propertyName, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName,
            Object value, Class<?>... groups) {
        return delegate.validateValue(beanType, propertyName, value, groups);
    }

    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        return delegate.getConstraintsForClass(clazz);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return delegate.unwrap(type);
    }

    @Override
    public ExecutableValidator forExecutables() {
        return delegate.forExecutables();
    }
    
}
