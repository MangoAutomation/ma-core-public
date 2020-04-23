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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;
import com.infiniteautomation.mango.spring.db.FileStoreTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableIllegalArgumentException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.FileStoreDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.UserFileStoreCreatePermissionDefinition;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

/**
 * @author Terry Packer
 *
 */
@Service
public class FileStoreService extends AbstractBasicVOService<FileStore, FileStoreTableDefinition, FileStoreDao> {

    private final UserFileStoreCreatePermissionDefinition createPermission;

    /**
     * @param dao
     * @param permissionService
     * @param createPermissionDefinition
     */
    @Autowired
    public FileStoreService(FileStoreDao dao, PermissionService permissionService, UserFileStoreCreatePermissionDefinition createPermission) {
        super(dao, permissionService);
        this.createPermission = createPermission;
    }

    @Override
    public PermissionDefinition getCreatePermission() {
        return createPermission;
    }

    @Override
    public ProcessResult validate(FileStore vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readPermission", user, false, null, vo.getReadPermission());
        permissionService.validateVoRoles(result, "writePermission", user, false, null, vo.getWritePermission());
        return result;
    }

    @Override
    public ProcessResult validate(FileStore existing, FileStore vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readPermission", user, false, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validateVoRoles(result, "writePermission", user, false, existing.getWritePermission(), vo.getWritePermission());
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
        //Since file stores can be accessed publicly and some of the definitions allow this
        // we must check for a potentially null user.
        if(user == null) {
            return  false;
        }else {
            return permissionService.hasPermission(user, vo.getWritePermission());
        }
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, FileStore vo) {
        //Since file stores can be accessed publicly and some of the definitions allow this
        // we must check for a potentially null user.
        if(user == null) {
            return  false;
        }else {
            return permissionService.hasPermission(user, vo.getReadPermission());
        }
    }

    /**
     * List all filestore names that the user has read permission for
     *
     * @return
     * @throws PermissionException
     */
    public List<String> getStoreNames() throws PermissionException {
        List<FileStore> fileStores = getAll();
        Collection<FileStoreDefinition> moduleDefs = ModuleRegistry.getFileStoreDefinitions().values();

        PermissionHolder user = Common.getUser();

        List<String> names = new ArrayList<>(fileStores.size() + moduleDefs.size());
        for(FileStore fs : fileStores) {
            names.add(fs.getStoreName());
        }
        for(FileStoreDefinition def : moduleDefs) {
            if(def.hasStoreReadPermission(user)) {
                names.add(def.getStoreName());
            }
        }
        return names;
    }

    /**
     * Get a user file store from the database
     *  this does not check the module defined stores.
     *
     * @param name
     * @return
     */
    public FileStore getByName(String name) {
        return getByName(name, false);
    }

    /**
     * Helper to get stores based on permission
     * @param name
     * @param write
     * @return
     */
    protected FileStore getByName(String name, boolean write) {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        FileStore store = dao.getByName(name);
        if(store == null) {
            throw new NotFoundException();
        }
        if(write) {
            ensureEditPermission(user, store);
        }else {
            ensureReadPermission(user, store);
        }
        return store;
    }

    /**
     * Get root path and ensure write access
     * <p>Deprecated, prefer {@link #getPathForWrite(String, String)} instead</p>
     * @param name
     * @return
     * @throws PermissionException
     */
    @Deprecated
    public Path getFileStoreRootForWrite(String name) throws PermissionException {
        FileStoreDefinition fsd = ModuleRegistry.getFileStoreDefinition(name);
        //This user can be null
        PermissionHolder user = Common.getUser();
        if(fsd != null) {
            fsd.ensureStoreWritePermission(user);
            return fsd.getRootPath();
        }else {
            return getByName(name, true).getRootPath();
        }
    }

    /**
     * Get root path and ensure read access
     * <p>Deprecated, prefer {@link #getPathForRead(String, String)} instead</p>
     *
     * @param name
     * @return
     * @throws PermissionException
     */
    @Deprecated
    public Path getFileStoreRootForRead(String name) throws PermissionException {
        FileStoreDefinition fsd = ModuleRegistry.getFileStoreDefinition(name);
        //This user can be null
        PermissionHolder user = Common.getUser();
        if(fsd != null) {
            fsd.ensureStoreReadPermission(user);
            return fsd.getRootPath();
        }else {
            return getByName(name, false).getRootPath();
        }
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has write permission for the file store.
     *
     * @param fileStoreName
     * @param path
     * @return
     * @throws PermissionException
     */
    public Path getPathForWrite(String fileStoreName, String path) throws PermissionException {
        Path root = getFileStoreRootForWrite(fileStoreName);
        Path filePath = root.resolve(path);
        if (!filePath.startsWith(root)) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.belowRoot", path));
        }
        return filePath;
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has read permission for the file store.
     *
     * @param fileStoreName
     * @param path
     * @return
     * @throws PermissionException
     */
    public Path getPathForRead(String fileStoreName, String path) throws PermissionException {
        Path root = getFileStoreRootForRead(fileStoreName);
        Path filePath = root.resolve(path);
        if (!filePath.startsWith(root)) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.belowRoot", path));
        }
        return filePath;
    }

    /**
     *
     * @param fs
     * @param purgeFiles
     * @throws IOException
     * @throws PermissionException
     * @throws NotFoundException
     */
    public void deleteFileStore(FileStore fs, boolean purgeFiles) throws IOException, PermissionException, NotFoundException {
        fs = delete(fs.getId());
        if(purgeFiles) {
            FileUtils.deleteDirectory(fs.getRootPath().toFile());
        }
    }

    public File moveFileOrFolder(String fileStoreName, File root, File fileOrFolder, String moveTo) throws TranslatableException, IOException, URISyntaxException {
        Path srcPath = fileOrFolder.toPath();

        File dstFile = new File(fileOrFolder.getParentFile(), moveTo).getCanonicalFile();
        Path dstPath = dstFile.toPath();
        if (!dstPath.startsWith(root.toPath())) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.belowRoot", moveTo));
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
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.belowRoot", dst));
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
