/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module;

import freemarker.cache.TemplateLoader;

import java.io.IOException;

/**
 * @author Jared Wiltshire
 */
public abstract class FreemarkerTemplateLoaderDefinition extends ModuleElementDefinition {
    public abstract TemplateLoader getTemplateLoader() throws IOException;
}
