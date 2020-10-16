/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.io.File;
import java.util.Comparator;

/**
 * @author Matthew Lohbihler
 */
public class DirectoryUtils {
    public static final Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
        public int compare(File f1, File f2) {
            return String.CASE_INSENSITIVE_ORDER.compare(f1.getPath(), f2.getPath());
        }
    };

    public static DirectoryInfo getSize(File file) {
        DirectoryInfo info = new DirectoryInfo();
        getSizeImpl(info, file);
        return info;
    }

    private static void getSizeImpl(DirectoryInfo info, File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subfile : files)
                    getSizeImpl(info, subfile);
            }
        }
        else {
            info.count++;
            info.size += file.length();
        }
    }


}
