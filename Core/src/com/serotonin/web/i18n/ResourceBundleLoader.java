package com.serotonin.web.i18n;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.serotonin.ShouldNeverHappenException;

/**
 * A class loader for loading resource bundles from a directory that is not on the classpath.
 * 
 * @author Matthew Lohbihler
 */
public class ResourceBundleLoader extends ClassLoader {
    private final String directory;

    public ResourceBundleLoader(String directory) {
        this.directory = directory;
    }

    @Override
    protected URL findResource(String name) {
        URL url = super.findResource(name);
        if (url != null)
            return url;

        File file = new File(directory, name);
        if (file.exists()) {
            try {
                return file.toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new ShouldNeverHappenException(e);
            }
        }

        return null;
    }
}
