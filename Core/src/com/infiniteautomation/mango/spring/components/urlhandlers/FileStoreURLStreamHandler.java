/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components.urlhandlers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.service.FileStoreService;

/**
 * @author Jared Wiltshire
 */
@Component
@SupportedProtocols({"filestore"})
public class FileStoreURLStreamHandler extends URLStreamHandler {

    private final FileStoreService fileStoreService;

    @Autowired
    public FileStoreURLStreamHandler(FileStoreService fileStoreService) {
        this.fileStoreService = fileStoreService;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String fileStoreName = u.getHost();
        String fileStorePath = u.getPath().length() > 0 ? u.getPath().substring(1) : u.getPath();
        Path path = fileStoreService.getPathForRead(fileStoreName, fileStorePath);
        return path.toUri().toURL().openConnection();
    }

    /**
     * Prevents DNS resolution of hostnames when calculating the hashcode. Note: This is not synchronized, unlike the super method.
     *
     * @param u
     * @return
     */
    @Override
    protected InetAddress getHostAddress(URL u) {
        return null;
    }
}
