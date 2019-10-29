/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * @author Terry Packer
 *
 */
public class MangoBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner {
    
    private boolean ranBeforeClass;
    
    /**
     * @param klass
     * @throws InitializationError
     */
    public MangoBlockJUnit4ClassRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Statement withBeforeClasses(Statement statement) {
        if(ranBeforeClass) {
            return statement;
        }else {
            ranBeforeClass = true;
            List<FrameworkMethod> befores = getTestClass()
                    .getAnnotatedMethods(BeforeClass.class);
            return befores.isEmpty() ? statement :
                    new RunBefores(statement, befores, null);
        }
    }
    
    @Override
    protected Statement withAfterClasses(Statement statement) {
        return statement;
        
//        List<FrameworkMethod> afters = getTestClass()
//                .getAnnotatedMethods(AfterClass.class);
//        return afters.isEmpty() ? statement :
//                new RunAfters(statement, afters, null);
    }
    
}
