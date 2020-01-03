/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.db.dao.FileStoreDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.UserFileStoreCreatePermissionDefinition;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.validation.StringValidation;

/**
 * @author Terry Packer
 *
 */
@Service
public class FileStoreService extends AbstractBasicVOService<FileStore, FileStoreDao> {

    /**
     * @param dao
     * @param permissionService
     * @param createPermissionDefinition
     */
    @Autowired
    public FileStoreService(FileStoreDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }
    
    @Override
    public Set<Role> getCreatePermissionRoles() {
        return ModuleRegistry.getPermissionDefinition(UserFileStoreCreatePermissionDefinition.TYPE_NAME).getRoles();
    }
    
    @Override
    public ProcessResult validate(FileStore vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readRoles", user, false, null, vo.getReadRoles());
        permissionService.validateVoRoles(result, "writeRoles", user, false, null, vo.getWriteRoles());
        return result;
    }
    
    @Override
    public ProcessResult validate(FileStore existing, FileStore vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readRoles", user, false, existing.getReadRoles(), vo.getReadRoles());
        permissionService.validateVoRoles(result, "writeRoles", user, false, existing.getWriteRoles(), vo.getWriteRoles());
        return result;
    }
    
    protected ProcessResult commonValidation(FileStore vo, PermissionHolder holder) {
        ProcessResult result = new ProcessResult();
        if (StringUtils.isBlank(vo.getStoreName()))
            result.addContextualMessage("storeName", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getStoreName(), 100))
            result.addMessage("storeName", new TranslatableMessage("validate.notLongerThan", 100));
        return result;
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, FileStore vo) {
        return permissionService.hasAnyRole(user, vo.getWriteRoles());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, FileStore vo) {
        return permissionService.hasAnyRole(user, vo.getReadRoles());
    }

    public void deleteFileStore(FileStore fs, boolean purgeFiles) throws IOException, PermissionException, NotFoundException {
        fs = delete(fs.getId());
        if(purgeFiles) {
            File root = fs.toDefinition().getRoot();
            FileUtils.deleteDirectory(root);
        }
    }

    public File moveFileOrFolder(String fileStoreName, File root, File fileOrFolder, String moveTo) throws TranslatableException, IOException, URISyntaxException {
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

    public File copyFileOrFolder(String fileStoreName, File root, File srcFile, String dst) throws TranslatableException, IOException, URISyntaxException {
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

    public File findUniqueFileName(File directory, String filename, boolean overwrite) throws TranslatableException, IOException {
        File file = new File(directory, filename).getCanonicalFile();
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

    public String relativePath(File relativeTo, File file) {
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
    public String removeToRoot(File root, File file) throws UnsupportedEncodingException {
        Path relativePath = root.toPath().relativize(file.toPath());
        String relativePathStr = relativePath.toString().replace(File.separatorChar, '/');

        if (file.isDirectory() && relativePathStr.endsWith("/")) {
            relativePathStr = relativePathStr.substring(0, relativePathStr.length() - 1);
        }
        return relativePathStr;
    }

}
