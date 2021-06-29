/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Jared Wiltshire
 */
public class BootstrapUtils {

    public static byte[] toByteArray(InputStream in) throws IOException {
        return toByteArray(in, in.available());
    }

    public static byte[] toByteArray(InputStream in, int size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, size));
        BootstrapUtils.copy(in, out);
        return out.toByteArray();
    }


    public static long copy(InputStream in, OutputStream out) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        for (int bytesRead; (bytesRead = in.read(buffer)) != -1; total += bytesRead) {
            out.write(buffer, 0, bytesRead);
        }
        return total;
    }

    public static Path maHome() {
        String[] possibleMaHomes = new String[] {
                System.getProperty("mango.paths.home"),
                System.getenv("mango_paths_home"),
                "..", // double click jar file
                "." // run java -jar boot/ma-bootstrap.jar from MA_HOME
        };

        Path maHome = null;
        for (String possibleMaHome : possibleMaHomes) {
            if (possibleMaHome == null) {
                continue;
            }

            Path test = Paths.get(possibleMaHome);
            if (isMaHome(test)) {
                maHome = test;
                break;
            }
        }

        if (maHome == null) {
            throw new RuntimeException("Can't find install directory, please set a Java system property -Dmango.paths.home=\"path\\to\\mango\"");
        }

        Path maHomeAbs = maHome.toAbsolutePath().normalize();

        // ensure Mango Core can find Mango home
        System.setProperty("mango.paths.home", maHomeAbs.toString());

        return maHomeAbs;
    }

    public static boolean isMaHome(Path testMaHome) {
        return Files.isRegularFile(testMaHome.resolve("release.properties")) || Files.isRegularFile(testMaHome.resolve("release.signed"));
    }
}
