/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.license.LicenseFeature;
import com.serotonin.m2m2.util.license.ModuleLicense;

/**
 * All information regarding a module required by the core.
 * 
 * @author Matthew Lohbihler
 */
public class Module {
    public static final void sortByName(List<Module> modules) {
        Collections.sort(modules, new Comparator<Module>() {
            @Override
            public int compare(Module m1, Module m2) {
                return m1.getName().compareTo(m2.getName());
            }
        });
    }

    private final String name;
    private final String version;
    private final TranslatableMessage description;
    private final String vendor;
    private final String vendorUrl;
    private final String dependencies;
    private boolean markedForDeletion;
    //    private boolean disabled;

    private final List<ModuleElementDefinition> definitions = new ArrayList<ModuleElementDefinition>();

    private final Set<String> locales = new HashSet<String>();
    private String graphics;
    private String emailTemplates;

    /**
     * Module constructor. Should not be used by client code.
     * 
     * @param name
     * @param version
     * @param description
     * @param vendor
     * @param vendorUrl
     */
    public Module(String name, String version, TranslatableMessage description, String vendor, String vendorUrl,
            String dependencies) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.vendor = vendor;
        this.vendorUrl = vendorUrl;
        this.dependencies = dependencies;
    }

    /**
     * @return the path from the MA home to the module's directory. Suitable for creating File objects within Java
     *         code
     */
    public String getDirectoryPath() {
        return "/" + Constants.DIR_WEB + getWebPath();
    }

    /**
     * @return the path from MA's web root to the module's directory. Suitable for creating URLs to module assets.
     */
    public String getWebPath() {
        return "/" + Constants.DIR_MODULES + "/" + name;
    }

    /**
     * Called after the system is initialized, i.e. once services like the database, timer, properties, runtime, etc are
     * available. Should not be used by client code.
     */
    public void preInitialize() {
        for (ModuleElementDefinition df : definitions)
            df.preInitialize();
    }

    /**
     * Called immediately after the database is initialized, but before the event and runtime managers. Should not be
     * used by client code.
     */
    public void postDatabase() {
        for (ModuleElementDefinition df : definitions)
            df.postDatabase();
    }

    /**
     * Called after the system is initialized, i.e. once services like the database, timer, properties, runtime, etc are
     * available. Should not be used by client code.
     */
    public void postInitialize() {
        for (ModuleElementDefinition df : definitions)
            df.postInitialize();
    }

    /**
     * Called before the system is terminated, i.e. while services are still available. Should not be used by client
     * code.
     */
    public void preTerminate() {
        for (ModuleElementDefinition df : definitions)
            df.preTerminate();
    }

    /**
     * Called upon shutdown after the runtime, but before the event manager, has been terminated. Only called on modules
     * that have been marked for deletion. Should not be used by client code.
     */
    public void uninstall() {
        for (ModuleElementDefinition df : definitions)
            df.uninstall();
    }

    /**
     * Called after the system is terminated. Should not be used by client code.
     */
    public void postTerminate() {
        for (ModuleElementDefinition df : definitions)
            df.postTerminate();
    }

    /**
     * @return the module's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the module's version
     */
    public String getVersion() {
        return version;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public String getVendor() {
        return vendor;
    }

    public String getVendorUrl() {
        return vendorUrl;
    }

    public String getDependencies() {
        return dependencies;
    }

    public ModuleLicense license() {
        if (Common.license() == null)
            return null;
        return Common.license().getModuleLicense(name);
    }

    public LicenseFeature licenseFeature(String name) {
        ModuleLicense moduleLicense = license();
        if (moduleLicense != null)
            return moduleLicense.getFeature(name);
        return null;
    }

    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    public void setMarkedForDeletion(boolean markedForDeletion) {
        this.markedForDeletion = markedForDeletion;
    }

    public void addDefinition(ModuleElementDefinition definition) {
        definition.setModule(this);
        definitions.add(definition);
    }

    @SuppressWarnings("unchecked")
    public <T extends ModuleElementDefinition> List<T> getDefinitions(Class<T> clazz) {
        List<T> defs = new ArrayList<T>();
        for (ModuleElementDefinition def : definitions) {
            if (clazz.isAssignableFrom(def.getClass()))
                defs.add((T) def);
        }
        return defs;
    }

    public List<TranslatableMessage> getLicenseErrors() {
        List<TranslatableMessage> errors = new ArrayList<TranslatableMessage>();
        for (LicenseDefinition def : getDefinitions(LicenseDefinition.class))
            def.addLicenseErrors(errors);
        return errors;
    }

    public List<TranslatableMessage> getLicenseWarnings() {
        List<TranslatableMessage> warnings = new ArrayList<TranslatableMessage>();
        for (LicenseDefinition def : getDefinitions(LicenseDefinition.class))
            def.addLicenseWarnings(warnings);
        return warnings;
    }

    public void addLocaleDefinition(String locale) {
        locales.add(locale);
    }

    public Set<String> getLocales() {
        return locales;
    }

    public void setGraphicsDir(String dir) {
        graphics = dir;
    }

    public String getGraphicsDir() {
        return graphics;
    }

    public String getEmailTemplatesDir() {
        return emailTemplates;
    }

    public void setEmailTemplatesDir(String emailTemplates) {
        this.emailTemplates = emailTemplates;
    }
}
