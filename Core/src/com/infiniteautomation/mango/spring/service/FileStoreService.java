/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.FileStoreDao;
import com.serotonin.m2m2.i18n.ProcessResult;
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
 * @author Jared Wiltshire
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

    private FileStore getWithoutPermissionCheck(String xid) {
        FileStoreDefinition definition = ModuleRegistry.getFileStoreDefinitions().get(xid);
        if (definition != null) {
            return definition.toFileStore();
        } else {
            FileStore vo = dao.getByXid(xid);
            if (vo == null) {
                throw new NotFoundException();
            }
            return vo;
        }
    }

    /**
     * @param xid xid of the user file store, or the storeName of the {@link FileStoreDefinition}
     * @return
     * @throws PermissionException
     * @throws NotFoundException
     */
    @Override
    public FileStore get(String xid) throws PermissionException, NotFoundException {
        FileStore vo = getWithoutPermissionCheck(xid);
        ensureReadPermission(Common.getUser(), vo);
        return vo;
    }

    /**
     * @param xid        xid of the user file store, or the storeName of the {@link FileStoreDefinition}
     * @param purgeFiles
     * @throws IOException
     * @throws PermissionException
     * @throws NotFoundException
     */
    public void delete(String xid, boolean purgeFiles) throws IOException, PermissionException, NotFoundException {
        FileStore deleted = delete(xid);
        if (purgeFiles) {
            Path root = getFileStoreRoot(deleted);
            FileUtils.deleteDirectory(root.toFile());
        }
    }

    public void deleteFileOrFolder(String xid, String toDelete, boolean recursive) {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        Path root = getFileStoreRoot(fileStore);
        Path toDeletePath = getPathWithinFileStore(fileStore, toDelete);

        if (!Files.exists(toDeletePath)) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileDoesNotExist", relativePath(root, toDeletePath)));
        }

        try {
            if (Files.isDirectory(toDeletePath) && recursive) {
                FileUtils.deleteDirectory(toDeletePath.toFile());
            } else {
                Files.delete(toDeletePath);
            }
        } catch (Exception e) {
            throw new FileStoreException(new TranslatableMessage("filestore.errorDeletingFile"));
        }
    }

    public Path moveFileOrFolder(String xid, String src, String dst) {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        Path root = getFileStoreRoot(fileStore);
        Path srcPath = getPathWithinFileStore(fileStore, src);
        Path dstPath = getPathWithinFileStore(fileStore, dst);

        if (!Files.exists(srcPath)) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileDoesNotExist", relativePath(root, srcPath)));
        }

        if (Files.isDirectory(dstPath)) {
            dstPath = dstPath.resolve(srcPath.getFileName());
        }

        try {
            return Files.move(srcPath, dstPath);
        } catch (FileAlreadyExistsException e) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileExists", relativePath(root, dstPath)));
        } catch (Exception e) {
            throw new FileStoreException(new TranslatableMessage("filestore.errorMovingFile"));
        }
    }

    public Path copyFileOrFolder(String xid, String src, String dst) {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        Path root = getFileStoreRoot(fileStore);
        Path srcPath = getPathWithinFileStore(fileStore, src);
        Path dstPath = getPathWithinFileStore(fileStore, dst);

        if (!Files.exists(srcPath)) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileDoesNotExist", relativePath(root, srcPath)));
        }

        if (Files.isDirectory(srcPath)) {
            throw new FileStoreException(new TranslatableMessage("filestore.cantCopyDirectory"));
        }

        if (Files.isDirectory(dstPath)) {
            dstPath = dstPath.resolve(srcPath.getFileName());
        }

        try {
            return Files.copy(srcPath, dstPath);
        } catch (FileAlreadyExistsException e) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileExists", relativePath(root, dstPath)));
        } catch (Exception e) {
            throw new FileStoreException(new TranslatableMessage("filestore.errorCopyingFile"));
        }
    }

    public Path createDirectory(String xid, String toCreate) {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        Path root = getFileStoreRoot(fileStore);
        Path toCreatePath = getPathWithinFileStore(fileStore, toCreate);
        try {
            Files.createDirectories(toCreatePath.getParent());
            return Files.createDirectory(toCreatePath);
        } catch (FileAlreadyExistsException e) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileExists", relativePath(root, toCreatePath)));
        } catch (Exception e) {
            throw new FileStoreException(new TranslatableMessage("filestore.errorCreatingDirectory"));
        }
    }

    private Path getFileStoreRoot(FileStore fileStore) {
        // TODO xids with slash or dots?
        Path test = Paths.get("test/abc");
        System.out.println(test.getNameCount());
        System.out.println(test);
        return this.fileStoreRoot.resolve(fileStore.getXid());
    }

    private Path getPathWithinFileStore(FileStore fileStore, String path) {
        Path root = getFileStoreRoot(fileStore);
        Path filePath = root.resolve(path).toAbsolutePath().normalize();
        if (!filePath.startsWith(root)) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.invalidPath"));
        }
        return filePath;
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has write permission for the file store.
     *
     * @param xid  xid of the user file store, or the storeName of the {@link FileStoreDefinition}
     * @param path
     * @return
     * @throws PermissionException user does not have permission to write to the file store
     * @throws NotFoundException   file store does not exist
     */
    public Path getPathForWrite(String xid, String path) throws PermissionException, NotFoundException {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        return getPathWithinFileStore(fileStore, path);
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has read permission for the file store.
     *
     * @param xid  xid of the user file store, or the storeName of the {@link FileStoreDefinition}
     * @param path
     * @return
     * @throws PermissionException user does not have permission to read from the file store
     * @throws NotFoundException   file store does not exist
     */
    public Path getPathForRead(String xid, String path) throws PermissionException, NotFoundException {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureReadPermission(Common.getUser(), fileStore);
        return getPathWithinFileStore(fileStore, path);
    }

    private FileStore resolveFileStore(Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();
        if (!absolutePath.startsWith(fileStoreRoot)) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.invalidPath"));
        }
        Path relative = fileStoreRoot.relativize(absolutePath);
        if (relative.getNameCount() == 0) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.invalidPath"));
        }
        String fileStoreName = relative.getName(0).toString();
        return getWithoutPermissionCheck(fileStoreName);
    }

    /**
     * @param path
     * @throws IllegalArgumentException if path is not located inside the filestore root
     * @throws NotFoundException        if filestore was not found
     * @throws PermissionException      filestore exists but user does not have read access
     */
    public void ensureReadAccess(Path path) throws IllegalArgumentException, NotFoundException, PermissionException {
        FileStore fileStore = resolveFileStore(path);
        ensureReadPermission(Common.getUser(), fileStore);
    }

    /**
     * @param path
     * @throws IllegalArgumentException if path is not located inside the filestore root
     * @throws NotFoundException        if filestore was not found
     * @throws PermissionException      filestore exists but user does not have write access
     */
    public void ensureWriteAccess(Path path) throws IllegalArgumentException, NotFoundException, PermissionException {
        FileStore fileStore = resolveFileStore(path);
        ensureEditPermission(Common.getUser(), fileStore);
    }

    public String relativePath(Path root, Path file) {
        Path relativePath = root.relativize(file);
        return relativePath.toString().replace(File.separatorChar, '/');
    }

    public static class FileStoreException extends TranslatableRuntimeException {
        public FileStoreException(TranslatableMessage message, Throwable cause) {
            super(message, cause);
        }

        public FileStoreException(TranslatableMessage message) {
            super(message);
        }
    }
}
