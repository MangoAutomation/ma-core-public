/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import com.serotonin.io.StreamUtils;

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

    public static String bytesDescription(long size) {
        String sizeStr;
        if (size < 1028)
            sizeStr = size + " B";
        else {
            size /= 1028;
            if (size < 1000)
                sizeStr = size + " KB";
            else {
                size /= 1000;
                if (size < 1000)
                    sizeStr = size + " MB";
                else {
                    size /= 1000;
                    if (size < 1000)
                        sizeStr = size + " GB";
                    else {
                        size /= 1000;
                        sizeStr = size + " TB";
                    }
                }
            }
        }

        return sizeStr;
    }

    public static List<String> listDirectories(String startPath, String namePattern) throws IOException {
        return listDirectories(new File(startPath), namePattern);
    }

    public static List<String> listDirectories(File startPath, String namePattern) throws IOException {
        List<String> result = new ArrayList<String>();
        _listDirectories(result, startPath, namePattern);
        return result;
    }

    private static void _listDirectories(List<String> list, File currentPath, String namePattern) throws IOException {
        File[] files = currentPath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().matches(namePattern))
                    list.add(file.getCanonicalPath());
                if (file.isDirectory())
                    _listDirectories(list, file, namePattern);
            }
        }
    }

    public static void deleteFiles(String startPath, String namePattern) throws IOException {
        deleteFiles(new File(startPath), namePattern);
    }

    public static void deleteFiles(File startPath, String namePattern) throws IOException {
        _deleteFiles(startPath, namePattern);
    }

    private static void _deleteFiles(File currentPath, String namePattern) throws IOException {
        File[] files = currentPath.listFiles();
        for (File file : files) {
            if (file.getName().matches(namePattern)) {
                if (file.isDirectory())
                    // Delete the contents of the directory before attempting to delete the directory.
                    _deleteFiles(file, ".*");
                if (!file.delete())
                    throw new IOException("Failed to delete " + file);
            }
            else if (file.isDirectory())
                _deleteFiles(file, namePattern);
        }
    }

    public static boolean deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                boolean deleted;
                if (files[i].isDirectory())
                    deleted = deleteDirectory(files[i]);
                else
                    deleted = files[i].delete();

                if (!deleted)
                    // Fast fail in case any delete operation failed.
                    return false;
            }
        }

        return dir.delete();
    }

    public static void copyDirectory(File from, File to) throws IOException {
        if (!from.exists())
            return;

        if (!to.exists())
            to.mkdirs();

        String[] filenames = from.list();
        if (filenames != null) {
            File file, dest;
            FileInputStream in = null;
            FileOutputStream out = null;

            for (int i = 0; i < filenames.length; i++) {
                file = new File(from, filenames[i]);
                dest = new File(to, filenames[i]);

                if (file.isDirectory())
                    copyDirectory(file, dest);
                else {
                    try {
                        in = new FileInputStream(file);
                        out = new FileOutputStream(dest);
                        StreamUtils.transfer(in, out);
                    }
                    finally {
                        if (in != null)
                            in.close();
                        if (out != null)
                            out.close();
                    }
                }
            }
        }
    }

    public static void copyFile(File from, File to) throws IOException {
        if (!from.exists())
            return;

        if (!to.getParentFile().exists())
            to.getParentFile().mkdirs();

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);
            StreamUtils.transfer(in, out);
        }
        finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }

    /**
     * Attempts to use the rename method to move a file. If this fails (returns false), then the files are copied
     * instead, and the old is then deleted.
     * 
     * @param from
     * @param to
     * @throws IOException
     */
    public static void move(File from, File to) throws IOException {
        boolean moved = from.renameTo(to);
        if (!moved) {
            if (from.isDirectory()) {
                copyDirectory(from, to);
                deleteDirectory(from);
            }
            else {
                copyFile(from, to);
                from.delete();
            }
        }
    }

    //
    //
    // Zip files
    //
    public static void zip(File input, File output) throws IOException {
        FileOutputStream fileos = null;
        ZipOutputStream zipos = null;
        try {
            int relativity = input.getPath().length() + 1;

            output.getParentFile().mkdirs();

            fileos = new FileOutputStream(output);
            zipos = new ZipOutputStream(fileos);

            zip(input, zipos, relativity);
        }
        finally {
            IOUtils.closeQuietly(zipos);
            IOUtils.closeQuietly(fileos);
        }
    }

    private static void zip(File file, ZipOutputStream zip, int relativity) throws IOException {
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list != null) {
                for (File child : list)
                    zip(child, zip, relativity);
            }
        }
        else
            addZipEntry(file, zip, relativity);
    }

    public static void addZipEntry(File file, ZipOutputStream zip, int relativity) throws IOException {
        String name = file.getPath().substring(relativity);
        name = name.replaceAll("\\\\", "/");

        ZipEntry e = new ZipEntry(name);
        zip.putNextEntry(e);

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            StreamUtils.transfer(in, zip);
        }
        finally {
            IOUtils.closeQuietly(in);
        }

        zip.closeEntry();
    }

    public static void unzip(File input, File outputDir) throws IOException {
        outputDir.mkdirs();

        ZipFile zip = null;
        InputStream in = null;
        FileOutputStream out = null;

        try {
            zip = new ZipFile(input);

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                try {
                    File file = new File(outputDir, entry.getName());
                    file.getParentFile().mkdirs();
                    out = new FileOutputStream(file);
                    in = zip.getInputStream(entry);
                    StreamUtils.transfer(in, out);
                }
                finally {
                    IOUtils.closeQuietly(out);
                    IOUtils.closeQuietly(in);
                }
            }

        }
        finally {
            if (zip != null)
                zip.close();
        }
    }

    public static void main(String[] args) throws Exception {
        //deleteFiles("/dev/DCM", "\\.svn");
        //copyDirectory(new File("src"), new File("src_copy"));
        //        System.out.println(listDirectories("C:/", "hasplms.*"));
        unzip(new File("dgbox.13.zip"), new File("dgboxUpgrade"));
    }
}
