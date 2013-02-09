package com.serotonin.m2m2.module;

/**
 * Used for overriding the default MA login page.
 * 
 * @author Matthew Lohbihler
 */
abstract public class LoginPageDefinition extends ModuleElementDefinition {
    /**
     * Returns the URI of the login page to use. The default value is
     * "/login.htm".
     * 
     * @return the URI of the loging page to use.
     */
    abstract public String getLoginPageUri();
}
