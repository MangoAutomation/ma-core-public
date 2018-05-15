/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved. 
 *
 */
package com.serotonin.m2m2.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;

import com.google.common.io.Files;
import com.infiniteautomation.mango.rest.v2.exception.GenericRestException;
import com.serotonin.m2m2.db.dao.FileStoreDao;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.FileStore;

/**
 *
 * @author Phillip Dunlap
 */
public class FileStoreUtils {
    public static boolean fileStoreExists(String fileStoreName) {
        FileStoreDefinition fsd = ModuleRegistry.getFileStoreDefinition(fileStoreName);
        if(fsd == null)
            fsd = FileStoreDao.instance.getFileStoreDefinition(fileStoreName);
        if(fsd == null)
            return false;
        return fsd.getRoot().exists();
    }
    
    public static boolean ensureFileStoreExists(String fileStoreName) {
        FileStoreDefinition fsd = ModuleRegistry.getFileStoreDefinition(fileStoreName);
        if(fsd == null)
            fsd = FileStoreDao.instance.getFileStoreDefinition(fileStoreName);
        if(fsd == null)
            return false;
        if(!fsd.getRoot().exists())
            return fsd.getRoot().mkdirs();
        return fsd.getRoot().isDirectory();
    }
    
    public static void deleteFileStore(FileStore fs, boolean purgeFiles) throws IOException {
        FileStoreDao.instance.deleteFileStore(fs.getStoreName());
        if(purgeFiles) {
            File root = fs.toDefinition().getRoot();
            FileUtils.deleteDirectory(root);
        }
    }
    
    public static File moveFileOrFolder(String fileStoreName, File root, File fileOrFolder, String moveTo) throws TranslatableException, IOException, URISyntaxException {
        Path srcPath = fileOrFolder.toPath();
        
        File dstFile = new File(fileOrFolder.getParentFile(), moveTo).getCanonicalFile();
        Path dstPath = dstFile.toPath();
        if (!dstPath.startsWith(root.toPath())) {
            throw new TranslatableException(new TranslatableMessage("filestore.belowRoot", moveTo));
        }
        
        if (dstFile.isDirectory()) {
            dstPath = dstPath.resolve(srcPath.getFileName());
        }
    
        Path movedPath;
        try {
            movedPath = java.nio.file.Files.move(srcPath, dstPath);
        } catch (FileAlreadyExistsException e) {
            throw new TranslatableException(new TranslatableMessage("filestore.fileExists", dstPath.getFileName()));
        }
        return new File(movedPath.toUri());
    }
    
    public static File copyFileOrFolder(String fileStoreName, File root, File srcFile, String dst) throws TranslatableException, IOException, URISyntaxException {
        if (srcFile.isDirectory()) {
            throw new TranslatableException(new TranslatableMessage("filestore.cantCopyDirectory"));
        }

        Path srcPath = srcFile.toPath();
        
        File dstFile = new File(srcFile.getParentFile(), dst).getCanonicalFile();
        Path dstPath = dstFile.toPath();
        if (!dstPath.startsWith(root.toPath())) {
            throw new TranslatableException(new TranslatableMessage("filestore.belowRoot", dst));
        }
        
        if (dstFile.isDirectory()) {
            dstPath = dstPath.resolve(srcPath.getFileName());
        }

        Path copiedPath;
        try {
            copiedPath = java.nio.file.Files.copy(srcPath, dstPath);
        } catch (FileAlreadyExistsException e) {
            throw new TranslatableException(new TranslatableMessage("filestore.fileExists", dstPath.getFileName()));
        }
        return new File(copiedPath.toUri());
    }
    
    public static File findUniqueFileName(String storeName, String filename, boolean overwrite) throws TranslatableException, IOException {
        FileStoreDefinition fsd = FileStoreDao.instance.getFileStoreDefinition(storeName);
        if(fsd == null)
            throw new TranslatableException(new TranslatableMessage("filestore.noSuchFileStore"));
        
        File file = new File(fsd.getRoot(), filename).getCanonicalFile();
        if (overwrite) {
            return file;
        }
        
        File parent = file.getParentFile();
        
        String originalName = Files.getNameWithoutExtension(filename);
        String extension = Files.getFileExtension(filename);
        int i = 1;
        
        while (file.exists()) {
            if (extension.isEmpty()) {
                file = new File(parent, String.format("%s_%03d", originalName, i++));
            } else {
                file = new File(parent, String.format("%s_%03d.%s", originalName, i++, extension));
            }
        }
        
        return file;
    }
    
    public static String relativePath(File relativeTo, File file) {
        Path relativePath = relativeTo.toPath().relativize(file.toPath());
        return relativePath.toString().replace(File.separatorChar, '/');
    }
    
    /**
     * Remove the path up to the root folder
     * @param root
     * @param file
     * @return
     * @throws UnsupportedEncodingException 
     */
    public static String removeToRoot(File root, File file) throws UnsupportedEncodingException {
        Path relativePath = root.toPath().relativize(file.toPath());
        String relativePathStr = relativePath.toString().replace(File.separatorChar, '/');

        if (file.isDirectory() && relativePathStr.endsWith("/")) {
            relativePathStr = relativePathStr.substring(0, relativePathStr.length() - 1);
        }
        return relativePathStr;
    }
}
