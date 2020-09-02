/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
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

/**
 * @author Terry Packer
 *
 */
@Service
public class FileStoreService extends AbstractVOService<FileStore, FileStoreTableDefinition, FileStoreDao> {

    private final UserFileStoreCreatePermissionDefinition createPermission;
    private final Path fileStoreRoot;

    /**
     * @param dao
     * @param permissionService
     * @param createPermissionDefinition
     */
    @Autowired
    public FileStoreService(FileStoreDao dao, PermissionService permissionService, UserFileStoreCreatePermissionDefinition createPermission,
            Environment env) {
        super(dao, permissionService);
        this.createPermission = createPermission;

        String location = env.getProperty(FileStoreDefinition.FILE_STORE_LOCATION_ENV_PROPERTY, FileStoreDefinition.ROOT);
        this.fileStoreRoot = Common.MA_HOME_PATH.resolve(location).toAbsolutePath().normalize();
    }

    @Override
    protected PermissionDefinition getCreatePermission() {
        return createPermission;
    }

    @Override
    public ProcessResult validate(FileStore vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        permissionService.validateVoRoles(result, "readPermission", user, false, null, vo.getReadPermission());
        permissionService.validateVoRoles(result, "writePermission", user, false, null, vo.getWritePermission());
        return result;
    }

    @Override
    public ProcessResult validate(FileStore existing, FileStore vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        permissionService.validateVoRoles(result, "readPermission", user, false, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validateVoRoles(result, "writePermission", user, false, existing.getWritePermission(), vo.getWritePermission());
        return result;
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, FileStore vo) {
        return permissionService.hasPermission(user, vo.getWritePermission());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, FileStore vo) {
        return permissionService.hasPermission(user, vo.getReadPermission());
    }

    /**
     * List all file-stores that the user has read permission for
     */
    public List<FileStore> getStores() {
        List<FileStore> stores = new ArrayList<>();

        this.customizedQuery(new ConditionSortLimit(null, null, null, null),
                (item, row) -> stores.add(item));

        PermissionHolder user = Common.getUser();
        Collection<FileStoreDefinition> moduleDefs = ModuleRegistry.getFileStoreDefinitions().values();
        for (FileStoreDefinition def : moduleDefs) {
            if (this.permissionService.hasPermission(user, def.getReadPermission())) {
                stores.add(def.toFileStore());
            }
        }
        return stores;
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
     *
     * @param name
     * @param write
     * @return
     */
    protected FileStore getByName(String name, boolean write) {
        PermissionHolder user = Common.getUser();

        FileStore store;
        FileStoreDefinition definition = ModuleRegistry.getFileStoreDefinitions().get(name);
        if (definition != null) {
            store = definition.toFileStore();
        } else {
            store = dao.getByXid(name);
            if (store == null) {
                throw new NotFoundException();
            }
        }

        if (write) {
            ensureEditPermission(user, store);
        } else {
            ensureReadPermission(user, store);
        }
        return store;
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has write permission for the file store.
     *
     * @param fileStoreName
     * @param path
     * @return
     * @throws PermissionException
     */
    public Path getPathForWrite(String fileStoreName, String path) throws PermissionException, NotFoundException, TranslatableIllegalArgumentException {
        PermissionHolder user = Common.getUser();

        Path root;
        FileStoreDefinition fsd = ModuleRegistry.getFileStoreDefinition(fileStoreName);
        if(fsd != null) {
            this.permissionService.ensurePermission(user, fsd.getWritePermission());
            root = fsd.getRootPath();
        }else {
            FileStore fs = getByName(fileStoreName, true);
            root = this.fileStoreRoot.resolve(fs.getXid());
        }

        Path filePath = root.resolve(path).toAbsolutePath().normalize();
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
    public Path getPathForRead(String fileStoreName, String path) throws PermissionException, NotFoundException, TranslatableIllegalArgumentException {
        PermissionHolder user = Common.getUser();

        Path root;
        FileStoreDefinition fsd = ModuleRegistry.getFileStoreDefinition(fileStoreName);
        if(fsd != null) {
            this.permissionService.ensurePermission(user, fsd.getReadPermission());
            root = fsd.getRootPath();
        }else {
            FileStore fs = getByName(fileStoreName, false);
            root = this.fileStoreRoot.resolve(fs.getXid());
        }

        Path filePath = root.resolve(path).toAbsolutePath().normalize();
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
            Path root = this.fileStoreRoot.resolve(fs.getXid());
            FileUtils.deleteDirectory(root.toFile());
        }
    }

    public File moveFileOrFolder(String fileStoreName, File root, File fileOrFolder, String moveTo) throws TranslatableException, IOException {
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

    public File copyFileOrFolder(String fileStoreName, File root, File srcFile, String dst) throws TranslatableException, IOException {
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

    public File findUniqueFileName(File directory, String filename, boolean overwrite) throws IOException {
        File file = new File(directory, filename).getCanonicalFile();
        if (overwrite) {
            return file;
        }

        File parent = file.getParentFile();

        int lastIndex = filename.lastIndexOf('.');
        String originalName = lastIndex < 0 ? filename : filename.substring(0, lastIndex);
        String extension = lastIndex < 0 ? "" : filename.substring(lastIndex + 1);

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
    public String removeToRoot(File root, File file) {
        Path relativePath = root.toPath().relativize(file.toPath());
        String relativePathStr = relativePath.toString().replace(File.separatorChar, '/');

        if (file.isDirectory() && relativePathStr.endsWith("/")) {
            relativePathStr = relativePathStr.substring(0, relativePathStr.length() - 1);
        }
        return relativePathStr;
    }

    /**
     * @param path
     * @return path relative to the file store root
     * @throws IllegalArgumentException if the path is not a file store path
     */
    public Path relativize(Path path) throws IllegalArgumentException {
        Path absolutePath = path.toAbsolutePath().normalize();
        if (!absolutePath.startsWith(fileStoreRoot)) {
            throw new IllegalArgumentException("Supplied path is outside the file store");
        }
        Path relative = fileStoreRoot.relativize(absolutePath);
        if (relative.getNameCount() == 0) {
            throw new IllegalArgumentException("File store name not present");
        }
        return relative;
    }
}
