package com.serotonin.m2m2.web;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Constants;

public class OverridingWebAppContext extends WebAppContext {
    public OverridingWebAppContext(ClassLoader classLoader) {
        OverridingFileResource ofr;
        try {
            ofr = new OverridingFileResource(Resource.newResource(Common.MA_HOME + "/overrides/" + Constants.DIR_WEB),
                    Resource.newResource(Common.MA_HOME + "/" + Constants.DIR_WEB));
        }
        catch (IOException e) {
            throw new ShouldNeverHappenException(e);
        }

        setBaseResource(ofr);
        setContextPath("/");
        setDescriptor("/WEB-INF/web.xml");
        setParentLoaderPriority(true);
        setClassLoader(classLoader);
        // Disallow directory listing
        setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        
        //Temp and JSP Compilation Settings (The order of these is important)
        String tempDirPath = Common.MA_HOME + "/work";
        File file = new File(tempDirPath);
        if (!file.exists())
            file.mkdirs();
        setAttribute("javax.servlet.context.tempdir", tempDirPath);
        setPersistTempDirectory(true);
        setTempDirectory(file);

    }
}
