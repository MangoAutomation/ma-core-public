/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.util.MangoServiceLoader;

/**
 * The base level of a module definition class. In general this class should not be directly extended. Instead, the sub
 * classes should be extended since it is the sub classes that will be recognized and handled upon startup. Other
 * classes will be ignored.
 *
 * @author Matthew Lohbihler
 */
abstract public class ModuleElementDefinition implements Ordered {

    public static final int DEFAULT_PRECEDENCE = 0;

    private Module module;

    /**
     * Access to the module class that owns this definition.
     *
     * @return the definition's module
     */
    public Module getModule() {
        return module;
    }

    void setModule(Module module) {
        this.module = module;
    }

    /**
     * Called immediately after the module is loaded, before the system is initialized.
     */
    public void preInitialize() {
        // Override as required
    }


    /**
     * Called immediately after the database is initialized, but before the event and runtime managers. Should not be
     * called by client code.
     * @param previousVersion - null if first install
     * @param current version
     */
    public void postDatabase(Version previousVersion, Version current) {
        // Override as required
    }

    /**
     * Called after immediately after the event manager is initialized, but before the runtime managers. Should not be
     * called by client code.
     * @param previousVersion - null if first install
     * @param current version
     */
    public void postEventManager(Version previousVersion, Version current) {
        // Override as required
    }

    /**
     * Called after the system is initialized, i.e. once services like the database, timer, properties, runtime, etc are
     * available. Should not be called by client code.
     *
     * @param previousVersion - null if first install
     * @param current version
     */
    public void postInitialize(Version previousVersion, Version current) {
        // Override as required
    }

    /**
     * Called before the system is terminated, i.e. while services are still available. Should not
     * be called by client code.
     * @param uninstall module will be uninstalled
     */
    public void preTerminate(boolean uninstall) {
        // Override as required
    }

    /**
     * Called upon shutdown after the runtime, but before the event manager, has been terminated. Should not
     * be called by client code.
     * @param uninstall module will be uninstalled
     */
    public void postRuntimeManagerTerminate(boolean uninstall) {
        // Override as required
    }

    /**
     * Called after the system is terminated. Should not
     * be called by client code.
     * @param uninstall module will be uninstalled
     */
    public void postTerminate(boolean uninstall) {
        // Override as required
    }

    @Override
    public int getOrder() {
        return DEFAULT_PRECEDENCE;
    }

    public boolean isEnabled() { return true; }

    /**
     * Loads all the module element definition classes and uses ConditionalDefinition to filter them. Does not return any definitions from parent classloaders.
     * @param classloader
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Set<Class<? extends ModuleElementDefinition>> loadDefinitions(ClassLoader classloader, String moduleName) throws IOException, ClassNotFoundException {
        return MangoServiceLoader.load(ModuleElementDefinition.class, classloader, "META-INF/services/" + moduleName).stream()
                .filter(clazz -> {
                    if (!clazz.isAnnotationPresent(ConditionalDefinition.class)) {
                        return true;
                    }

                    ConditionalDefinition conditional = clazz.getAnnotation(ConditionalDefinition.class);
                    String envProperty = conditional.value();
                    String[] requireClasses = conditional.requireClasses();

                    return conditional.enabled() &&
                            (envProperty.isEmpty() || Common.envProps.getBoolean(envProperty)) &&
                            checkClassesAvailable(requireClasses, classloader);
                }).collect(Collectors.toSet());
    }

    public static boolean checkClassesAvailable(String[] classNames, ClassLoader classLoader) {
        return Arrays.stream(classNames).allMatch(className -> {
            try {
                classLoader.loadClass(className);
            } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                return false;
            }
            return true;
        });
    }
}
