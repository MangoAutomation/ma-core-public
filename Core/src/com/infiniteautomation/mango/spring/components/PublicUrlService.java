/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;

/**
 * @author Jared Wiltshire
 */
@Service
public class PublicUrlService {

    private final SystemSettingsDao systemSettingsDao;
    private final Environment env;

    @Autowired
    private PublicUrlService(SystemSettingsDao systemSettingsDao, Environment env) {
        this.systemSettingsDao = systemSettingsDao;
        this.env = env;
    }

    /**
     * Tries to get the public hostname for this Mango instance in this order:
     * <ol>
     * <li>The "publiclyResolvableBaseUrl" system setting</li>
     * <li>InetAddress.getLocalHost()</li>
     * <li>Hardcoded "localhost"</li>
     * </ol>
     * @param skipBaseUrl
     * @return
     */
    public String getHostname() {
        return this.getHostname(false);
    }

    private String getHostname(boolean skipBaseUrl) {
        String hostname = null;

        if (!skipBaseUrl) {
            try {
                String baseUrl = this.systemSettingsDao.getValue(SystemSettingsDao.PUBLICLY_RESOLVABLE_BASE_URL);
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
                hostname = builder.build().getHost();
            } catch (IllegalArgumentException e) {
            }
        }

        if (hostname == null) {
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
            }

            if (hostname == null) {
                hostname = "localhost";
            }
        }

        return hostname;
    }

    /**
     * Tries to get the public URL for this Mango instance in this order:
     * <ol>
     * <li>The "publiclyResolvableBaseUrl" system setting</li>
     * <li>Generated from the hostname and SSL/port settings</li>
     * </ol>
     * @return
     */
    public UriComponentsBuilder getUriComponentsBuilder() {
        return this.getUriComponentsBuilder(null);
    }

    /**
     * Tries to get the public URL for this Mango instance in this order:
     * <ol>
     * <li>The "publiclyResolvableBaseUrl" system setting</li>
     * <li>The fallback if supplied</li>
     * <li>Generated from the hostname and SSL/port settings</li>
     * </ol>
     *
     * @param fallback
     * @return
     */
    public UriComponentsBuilder getUriComponentsBuilder(UriComponentsBuilder fallback) {
        UriComponentsBuilder builder;
        String baseUrl = this.systemSettingsDao.getValue(SystemSettingsDao.PUBLICLY_RESOLVABLE_BASE_URL);
        if (!StringUtils.isEmpty(baseUrl)) {
            builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
        } else if (fallback != null) {
            builder = fallback;
        } else {
            boolean sslOn = this.env.getProperty("ssl.on", Boolean.class, false);
            int port = sslOn ? this.env.getProperty("ssl.port", Integer.class, 443) : this.env.getProperty("web.port", Integer.class, 8080);

            String hostname = this.getHostname(true);

            builder = UriComponentsBuilder.newInstance()
                    .scheme(sslOn ? "https" : "http")
                    .host(hostname)
                    .port(port);
        }

        return builder;
    }

    public UriComponents getUriComponents() {
        return this.getUriComponentsBuilder().build();
    }

    public URI getUri() {
        return this.getUriComponents().toUri();
    }

}
