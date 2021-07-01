/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Jared Wiltshire
 */
public class CoreUpgrade {

    private final Path installationDirectory;

    public CoreUpgrade(Path installationDirectory) {
        this.installationDirectory = installationDirectory;
    }

    public void upgrade() {
        try {
            Files.list(installationDirectory).filter(f -> {
                String filename = f.getFileName().toString();
                return filename.startsWith("m2m2-core-") && filename.endsWith(".zip");
            }).sorted().forEach(this::extractZip);
        } catch (Exception e) {
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
            throw new UncheckedIOException(e);
        }
    }

    private void deleteOldFiles() {
        try {
            Files.deleteIfExists(installationDirectory.resolve("release.properties"));
            Files.deleteIfExists(installationDirectory.resolve("release.signed"));

            // wont delete work as this is now configurable and no longer used for JSP

            Path lib = installationDirectory.resolve("lib");
            if (Files.isDirectory(lib)) {
                Files.list(lib)
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void processEntry(ZipFile zip, ZipEntry entry) {
        try {
            Path entryPath = installationDirectory.resolve(entry.getName());
            String filename = entryPath.getFileName().toString();

            if (entry.isDirectory()) {
                Files.createDirectories(entryPath);
            } else {
                try (InputStream in = zip.getInputStream(entry)) {
                    // overwrite files in place
                    try (OutputStream out = Files.newOutputStream(entryPath, StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        copy(in, out);
                    }
                }

                Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());
                if (filename.endsWith(".sh")) {
                    //noinspection ResultOfMethodCallIgnored
                    entryPath.toFile().setExecutable(true, false);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static long copy(InputStream in, OutputStream out) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        for (int bytesRead; (bytesRead = in.read(buffer)) != -1; total += bytesRead) {
            out.write(buffer, 0, bytesRead);
        }
        return total;
    }

    public static class CoreUpgradeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public CoreUpgradeException(Exception e) {
            super(e);
        }
    }
}
