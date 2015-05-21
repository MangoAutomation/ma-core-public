package com.serotonin.m2m2.web.taglib;

import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * Useful to output information if a module is not available
 * @author Terry Packer
 *
 */
public class ModuleDoesNotExistTag extends ConditionalTagSupport {
    private static final long serialVersionUID = 1L;

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected boolean condition() {
        return !ModuleRegistry.hasModule(name);
    }
}
