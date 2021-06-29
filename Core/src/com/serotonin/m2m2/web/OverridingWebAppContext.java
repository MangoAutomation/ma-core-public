/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;

import com.infiniteautomation.mango.webapp.session.MangoInitializer;
import com.serotonin.m2m2.Common;

/**
 * We can probably move this logic to {@link MangoInitializer}
 */
public class OverridingWebAppContext extends WebAppContext {

    public OverridingWebAppContext() {
        // Ensure we use the JavaC compiler not JDT from Eclipse (Only For Glassfish I think)
        // No longer required - https://github.com/eclipse/jetty.project/issues/706
        // System.setProperty("org.apache.jasper.compiler.disablejsr199","false");

        // WebAppClassLoader.loadConfigurations() loads the MetaInfConfiguration class which
        // results in the context attribute org.eclipse.jetty.tlds being set to an empty array
        // instead of null. Therefore we need to set the container include pattern so that
        // the JettyJasperInitializer creates a TldScanner which picks up the JSTL, Spring and Serotonin .tld files
        this.setAttribute(WebInfConfiguration.CONTAINER_JAR_PATTERN, ".*\\.jar$");

        OverridingFileResource ofr = new OverridingFileResource(new PathResource(Common.OVERRIDES_WEB), new PathResource(Common.WEB));

        this.setBaseResource(ofr);
        this.setContextPath("/");

        // Copy of Jetty webdefault.xml with JSP and default servlet removed so we can configure them programmatically
        // see https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-webapp/src/main/config/etc/webdefault.xml
        Path webdefault = Common.WEB.resolve("WEB-INF").resolve("webdefault.xml");
        if (Files.isRegularFile(webdefault)) {
            this.setDefaultsDescriptor(webdefault.toFile().getAbsolutePath());
        } else {
            this.setDefaultsDescriptor(null);
        }

        //Detect and load any web.xml in the overrides since the baseResource doesn't account for this file
        File overrideDescriptor = Common.OVERRIDES_WEB.resolve("web.xml").toFile();
        if (overrideDescriptor.exists()) {
            this.setDescriptor(overrideDescriptor.getAbsolutePath());
        } else {
            this.setDescriptor("/WEB-INF/web.xml");
        }

        //Detect and load a override-web.xml file if it exists
        File overrideWebXml = Common.OVERRIDES_WEB.resolve("override-web.xml").toFile();
        if (overrideWebXml.exists()) {
            this.setOverrideDescriptor(overrideWebXml.getAbsolutePath());
        }

        this.setParentLoaderPriority(true);

        //Temp and JSP Compilation Settings (The order of these is important)
        //@See http://eclipse.org/jetty/documentation/current/ref-temporary-directories.html
        File tempDirPath = Common.getTempPath().toFile();
        this.setAttribute("javax.servlet.context.tempdir", tempDirPath.getAbsolutePath());
        this.setPersistTempDirectory(true);
        this.setTempDirectory(tempDirPath);

        this.getAliasChecks().clear();
        this.addAliasCheck(new ApproveNonExistentDirectoryAliases());
        if (Common.envProps.getBoolean("web.security.followSymlinks", true)) {
            this.addAliasCheck(new AllowOverridingSymLinkAliasChecker());
        }

    }

}
