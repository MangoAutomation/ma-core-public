/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandler.AliasCheck;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Lifted from org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker
 * 
 * Symbolic Link AliasChecker.
 * <p>An instance of this class can be registered with {@link ContextHandler#addAliasCheck(AliasCheck)}
 * to check resources that are aliased to other locations.   The checker uses the
 * Java {@link Files#readSymbolicLink(Path)} and {@link Path#toRealPath(java.nio.file.LinkOption...)}
 * APIs to check if a file is aliased with symbolic links.</p>
 * 
 * @author Terry Packer
 *
 */
public class AllowOverridingSymLinkAliasChecker implements AliasCheck
{
    private static final Logger LOG = Log.getLogger(AllowSymLinkAliasChecker.class);
    
    @Override
    public boolean check(String uri, Resource resource)
    {
        // Only support PathResource alias checking
        if (!(resource instanceof PathResource) && !(resource instanceof OverridingFileResource))
            return false;
        
        PathResource pathResource = (PathResource) resource;

        try
        {
            Path path = pathResource.getPath();
            Path alias = pathResource.getAliasPath();

            if (path.equals(alias))
                return false; // Unknown why this is an alias

            if (hasSymbolicLink(path) && Files.isSameFile(path, alias))
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Allow symlink {} --> {}", resource, pathResource.getAliasPath());
                return true;
            }
        }
        catch (Exception e)
        {
            LOG.ignore(e);
        }
        
        return false;
    }

    private boolean hasSymbolicLink(Path path)
    {
        // Is file itself a symlink?
        if (Files.isSymbolicLink(path))
        {
            return true;
        }

        // Lets try each path segment
        Path base = path.getRoot();
        for (Path segment : path)
        {
            base = base.resolve(segment);
            if (Files.isSymbolicLink(base))
            {
                return true;
            }
        }

        return false;
    }

}
