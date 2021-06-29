/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web;

import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.util.resource.Resource;

/**
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 *
 */
public class AllowOverridingSymLinkAliasChecker extends AllowSymLinkAliasChecker {
    @Override
    public boolean check(String uri, Resource resource)
    {
        if (resource instanceof OverridingFileResource) {
            Resource override = ((OverridingFileResource) resource).getOverrideResource();

            if (override.exists()) {
                return super.check(uri, override);
            } else {
                Resource base = ((OverridingFileResource) resource).getBaseResource();
                return super.check(uri, base);
            }
        }

        return super.check(uri, resource);
    }
}
