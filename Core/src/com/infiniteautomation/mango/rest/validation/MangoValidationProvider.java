/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.validation;

import javax.validation.ValidatorFactory;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.internal.engine.ConfigurationImpl;

/**
 * @author Terry Packer
 *
 */
public class MangoValidationProvider extends HibernateValidator {

    @Override
    public HibernateValidatorConfiguration createSpecializedConfiguration(BootstrapState state) {
        return HibernateValidatorConfiguration.class.cast( new ConfigurationImpl( this ) );
    }

    @Override
    public ValidatorFactory buildValidatorFactory(ConfigurationState configurationState) {
        return new MangoValidatorFactory( configurationState );
    }
    
}
