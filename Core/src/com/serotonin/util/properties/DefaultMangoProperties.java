/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.util.properties;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.text.StringSubstitutor;

/**
 * Properties are loaded from the following sources in order:
 * <ol>
 *     <li>Java system properties</li>
 *     <li>Environment variables</li>
 *     <li>User supplied config file</li>
 *     <li>Mango built-in config file</li>
 * </ol>
 *
 * <p>
 *     System properties must be prefixed with "mango." and environment variables must prefixed with "mango_" and
 *     use an underscore in place of the dots.
 * </p>
 *
 * <p>{@link com.infiniteautomation.mango.spring.MangoPropertySource MangoPropertySource} is used to supply these properties to Spring.
 * Note: The Spring property resolver attempts interpolation again when getting properties from Environment.</p>
 *
 * @author Jared Wiltshire
 */
public class DefaultMangoProperties implements MangoProperties {

    private final StringSubstitutor interpolator = createInterpolator();
    private final Path envPropertiesPath;
    private final Map<String, String> environment;
    protected volatile Properties properties;

    public DefaultMangoProperties(Path envPropertiesPath) {
        this(envPropertiesPath, MangoProperties.loadFromEnvironment());
    }

    public DefaultMangoProperties(Path envPropertiesPath, Map<String, String> environment) {
        this.envPropertiesPath = Objects.requireNonNull(envPropertiesPath);
        this.environment = environment;
        reload();
    }

    void reload() {
        // Load the environment properties
        Properties properties;
        try {
            properties = MangoProperties.loadFromResources("mango.properties");
            if (Files.isReadable(envPropertiesPath)) {
                try (Reader reader = Files.newBufferedReader(envPropertiesPath)) {
                    properties.load(reader);
                }
            }
            properties.putAll(environment);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.properties = properties;
    }

    Path getEnvPropertiesPath() {
        return envPropertiesPath;
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public String interpolateProperty(String value) {
        return interpolator.replace(value);
    }

}
