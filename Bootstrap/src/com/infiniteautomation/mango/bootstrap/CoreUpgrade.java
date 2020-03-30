/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Jared Wiltshire
 */
public class CoreUpgrade {

    private final Path maHome;

    public CoreUpgrade(Path maHome) {
        this.maHome = maHome;
    }

    public void upgrade() {
        try {
            Files.list(maHome).filter(f -> {
                String filename = f.getFileName().toString();
                return filename.startsWith("m2m2-core-") && filename.endsWith(".zip");
            }).sorted().forEach(this::extractZip);
        } catch (Exception e) {
            if (e instanceof CoreUpgradeException) {
                throw (CoreUpgradeException) e;
            }
            throw new CoreUpgradeException(e);
        }
    }

    private void extractZip(Path zipPath) {
        try {
            this.deleteOldFiles();

            try (ZipFile zip = new ZipFile(zipPath.toFile())) {
                zip.stream().forEach(entry -> {
                    this.processEntry(zip, entry);
                });
            }

            Files.delete(zipPath);
        } catch (IOException e) {
            throw new CoreUpgradeException(e);
        }
    }

    private void deleteOldFiles() {
        try {
            Files.deleteIfExists(maHome.resolve("release.properties"));
            Files.deleteIfExists(maHome.resolve("release.signed"));

            // wont delete work as this is now configurable and no longer used for JSP

            Path lib = maHome.resolve("lib");
            if (Files.isDirectory(lib)) {
                Files.list(lib)
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new CoreUpgradeException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new CoreUpgradeException(e);
        }
    }

    private void processEntry(ZipFile zip, ZipEntry entry) {
        try {
            Path entryPath = maHome.resolve(entry.getName());
            String filename = entryPath.getFileName().toString();

            if (entry.isDirectory()) {
                Files.createDirectories(entryPath);
            } else {
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }

                Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());
                if (filename.endsWith(".sh")) {
                    entryPath.toFile().setExecutable(true, false);
                }
            }
        } catch (IOException e) {
            throw new CoreUpgradeException(e);
        }
    }

    public static class CoreUpgradeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public CoreUpgradeException(Exception e) {
            super(e);
        }
    }
}
