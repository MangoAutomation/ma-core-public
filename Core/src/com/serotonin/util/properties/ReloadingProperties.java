/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Matthew Lohbihler
 */
public class ReloadingProperties extends AbstractProperties {
    private final Log LOG = LogFactory.getLog(this.getClass());

    private ClassLoader classLoader;

    private Properties properties = new Properties();
    private String sourceFilename;
    private File sourceFile;
    private File[] sourceFiles;
    private long lastTimestamp = 0;
    private long lastRecheck = 0;
    private long recheckDeadbandPeriod = 5000; // 5 seconds
    private ReloadCallback reloadCallback;
    private final Map<String, List<PropertyChangeCallback>> propertyChangeCallbacks = new HashMap<String, List<PropertyChangeCallback>>();

    private final Map<String, String> defaultValues = new HashMap<String, String>();

    public ReloadingProperties(String basename) {
        this(basename, ReloadingProperties.class.getClassLoader());
    }

    public ReloadingProperties(String basename, ClassLoader classLoader) {
        super(basename);
        sourceFilename = basename.replace('.', '/') + ".properties";
        this.classLoader = classLoader;
        checkForReload();
    }

    public ReloadingProperties(File file) {
        super(file.getName());
        sourceFile = file;
        checkForReload();
    }

    public void setDefaultValue(String key, String value) {
        defaultValues.put(key, value);
    }

    public long getRecheckDeadbandPeriod() {
        return recheckDeadbandPeriod;
    }

    public void setRecheckDeadbandPeriod(long recheckDeadbandPeriod) {
        this.recheckDeadbandPeriod = recheckDeadbandPeriod;
    }

    public ReloadCallback getReloadCallback() {
        return reloadCallback;
    }

    public void setReloadCallback(ReloadCallback reloadCallback) {
        this.reloadCallback = reloadCallback;
    }

    public void addPropertyChangeCallback(String key, PropertyChangeCallback callback) {
        List<PropertyChangeCallback> list = propertyChangeCallbacks.get(key);
        if (list == null) {
            list = new ArrayList<PropertyChangeCallback>();
            propertyChangeCallbacks.put(key, list);
        }
        list.add(callback);
    }

    public void removePropertyChangeCallback(String key, PropertyChangeCallback callback) {
        List<PropertyChangeCallback> list = propertyChangeCallbacks.get(key);
        if (list != null) {
            list.remove(callback);
            if (list.isEmpty())
                propertyChangeCallbacks.remove(key);
        }
    }

    @Override
    protected String getStringImpl(String key) {
        checkForReload();

        String value = properties.getProperty(key);
        if (value == null)
            value = defaultValues.get(key);

        return value;
    }

    private void checkForReload() {
        if (lastRecheck + recheckDeadbandPeriod > System.currentTimeMillis()) {
            if (LOG.isDebugEnabled())
                LOG.debug("(" + getDescription() + ") In do not check period. Not rechecking");
            // Still in the do not check period.
            return;
        }
        lastRecheck = System.currentTimeMillis();

        if (LOG.isDebugEnabled())
            LOG.debug("(" + getDescription() + ") Checking for updated files");

        findFiles();
        if (sourceFiles == null)
            return;

        // Determine the latest time stamp of all of the source files.
        long latestTimestamp = -1;
        for (File sourceFile : sourceFiles) {
            if (!sourceFile.exists())
                LOG.error("(" + getDescription() + ") Property file " + sourceFile + " does not exist");
            else {
                if (latestTimestamp < sourceFile.lastModified())
                    latestTimestamp = sourceFile.lastModified();
            }
        }

        // Check if we need to reload.
        if (latestTimestamp > lastTimestamp) {
            if (LOG.isInfoEnabled())
                LOG.info("(" + getDescription() + ") Found updated file(s) at " + Arrays.toString(sourceFiles)
                        + ". Reloading properties");

            // Time to reload. Create the new backing properties file.
            Properties newProps = new Properties();

            for (File sourceFile : sourceFiles) {
                Properties fileProps = new Properties();

                InputStream in = null;
                try {
                    // Load the properties in.
                    in = new FileInputStream(sourceFile);
                    fileProps.load(in);

                    // Overwrite previous values with these values.
                    for (Map.Entry<Object, Object> entry : fileProps.entrySet())
                        newProps.put(entry.getKey(), entry.getValue());
                }
                catch (IOException e) {
                    LOG.error("(" + getDescription() + ") Exception while loading property file " + sourceFile, e);
                }
                finally {
                    try {
                        if (in != null)
                            in.close();
                    }
                    catch (IOException e) {
                        // ignore
                    }
                }
            }

            if (reloadCallback != null)
                reloadCallback.propertiesReload(properties, newProps);

            for (Map.Entry<String, List<PropertyChangeCallback>> entry : propertyChangeCallbacks.entrySet()) {
                String oldProp = (String) properties.get(entry.getKey());
                String newProp = (String) newProps.get(entry.getKey());

                if (!ObjectUtils.equals(oldProp, newProp)) {
                    List<PropertyChangeCallback> cbs = entry.getValue();
                    if (cbs != null) {
                        for (PropertyChangeCallback cb : cbs)
                            cb.propertyChanged(newProp);
                    }
                }
            }

            // Set these properties as the actual backing object.
            properties = newProps;

            lastTimestamp = latestTimestamp;
        }
    }

    private void findFiles() {
        if (sourceFile != null)
            sourceFiles = new File[] { sourceFile };
        else {
            try {
                Enumeration<URL> urls = classLoader.getResources(sourceFilename);
                if (!urls.hasMoreElements())
                    sourceFiles = new File[] { new File(sourceFilename) };
                else {
                    List<File> files = new ArrayList<File>();
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        String uri = url.toString();
                        uri = uri.replaceAll(" ", "%20");
                        if (uri != null) {
                            try {
                                files.add(new File(new URI(uri)));
                            }
                            catch (URISyntaxException e) {
                                LOG.error("(" + getDescription() + ") ", e);
                            }
                        }
                    }

                    sourceFiles = files.toArray(new File[files.size()]);
                }
            }
            catch (IOException e) {
                LOG.error("(" + getDescription() + ") Error while finding properties files", e);
            }
        }
    }
}
