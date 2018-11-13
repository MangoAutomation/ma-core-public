/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * @author Terry Packer
 *
 */
@Constraint(validatedBy = DataPointXidExistsValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPointXidExists {

    String message() default "validate.invalidValue";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
    
}
