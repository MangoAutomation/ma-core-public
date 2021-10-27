/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableIllegalArgumentException;
import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.infiniteautomation.mango.util.exception.ValidationException;
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
public class FileStoreService extends AbstractVOService<FileStore, FileStoreDao> {

    private final UserFileStoreCreatePermissionDefinition createPermission;
    private final Path fileStoreRoot = Common.getFileStorePath();;

    public static final Pattern INVALID_XID_CHARACTERS = Pattern.compile("[./\\\\]");

    @Autowired
    public FileStoreService(FileStoreDao dao,
                            ServiceDependencies dependencies,
                            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UserFileStoreCreatePermissionDefinition createPermission) {
        super(dao, dependencies);
        this.createPermission = createPermission;
    }

    @Override
    protected PermissionDefinition getCreatePermission() {
        return createPermission;
    }

    @Override
    public ProcessResult validate(FileStore vo) {
        ProcessResult result = super.validate(vo);
        validateXid(result, vo);

        PermissionHolder user = Common.getUser();
        permissionService.validatePermission(result, "readPermission", user, vo.getReadPermission());
        permissionService.validatePermission(result, "writePermission", user, vo.getWritePermission());
        return result;
    }

    @Override
    public ProcessResult validate(FileStore existing, FileStore vo) {
        ProcessResult result = super.validate(vo);
        validateXid(result, vo);

        PermissionHolder user = Common.getUser();
        permissionService.validatePermission(result, "readPermission", user, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validatePermission(result, "writePermission", user, existing.getWritePermission(), vo.getWritePermission());
        return result;
    }

    private void validateXid(ProcessResult result, FileStore vo) {
        String xid = vo.getXid();
        if (xid != null) {
            if (INVALID_XID_CHARACTERS.matcher(xid).find()) {
                result.addContextualMessage("xid", "validate.containsInvalidCharacters");
            } else if (ModuleRegistry.getFileStoreDefinitions().containsKey(xid)) {
                result.addContextualMessage("xid", "filestore.reservedXid");
            }
        }
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
                (item) -> stores.add(item));

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

    @Override
    public FileStore insert(FileStore vo) throws PermissionException, ValidationException {
        FileStore newFilestore = super.insert(vo);
        Path newPath = getFileStoreRoot(newFilestore);
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            throw new FileStoreException(new TranslatableMessage("filestore.failedToCreateRoot", newFilestore.getXid()), e);
        }
        return newFilestore;
    }

    @Override
    protected FileStore update(FileStore existing, FileStore vo) throws PermissionException, ValidationException {
        if (ModuleRegistry.getFileStoreDefinitions().containsKey(existing.getXid())) {
            throw new UnsupportedOperationException("Updating a built in filestore is not supported");
        }

        FileStore updated = super.update(existing, vo);

        // move files to the new location if the XID changed
        if (!updated.getXid().equals(existing.getXid())) {
            Path existingPath = getFileStoreRoot(existing);
            Path newPath = getFileStoreRoot(updated);
            if (Files.exists(existingPath)) {
                try {
                    Files.move(existingPath, newPath);
                } catch (IOException e) {
                    throw new FileStoreException(new TranslatableMessage("filestore.failedToMoveFiles", updated.getXid()), e);
                }
            }
        }

        return updated;
    }

    @Override
    protected FileStore delete(FileStore vo) throws PermissionException, NotFoundException {
        if (ModuleRegistry.getFileStoreDefinitions().containsKey(vo.getXid())) {
            throw new UnsupportedOperationException("Deleting a built in filestore is not supported");
        }

        FileStore deleted = super.delete(vo);

        Path root = getFileStoreRoot(deleted);
        if (Files.exists(root)) {
            try {
                FileUtils.deleteDirectory(root.toFile());
            } catch (IOException e) {
                throw new FileStoreException(new TranslatableMessage("filestore.failedToDeleteFiles", deleted.getXid()), e);
            }
        }

        return deleted;
    }

    public FileStorePath deleteFileOrFolder(String xid, String toDelete, boolean recursive) {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        FileStorePath toDeletePath = getPathWithinFileStore(fileStore, toDelete);

        if (toDeletePath.absolutePath.equals(toDeletePath.getFileStoreRoot())) {
            throw new FileStoreException(new TranslatableMessage("filestore.deleteRootNotPermitted"));
        }

        if (!Files.exists(toDeletePath.absolutePath)) {
            throw new NotFoundException();
        }

        try {
            if (Files.isDirectory(toDeletePath.absolutePath) && recursive) {
                FileUtils.deleteDirectory(toDeletePath.absolutePath.toFile());
            } else {
                Files.delete(toDeletePath.absolutePath);
            }
            return toDeletePath;
        } catch (NoSuchFileException e) {
            throw new NotFoundException();
        } catch (Exception e) {
            throw new FileStoreException(new TranslatableMessage("filestore.errorDeletingFile"));
        }
    }

    public FileStorePath moveFileOrFolder(String xid, String src, String dst) {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        FileStorePath srcPath = getPathWithinFileStore(fileStore, src);

        if (!Files.exists(srcPath.absolutePath)) {
            throw new NotFoundException();
        }

        FileStorePath dstPath = srcPath.getParent().resolve(Paths.get(dst));
        if (Files.isDirectory(dstPath.absolutePath)) {
            Path pathWithFileName = dstPath.absolutePath.resolve(srcPath.absolutePath.getFileName());
            dstPath = new FileStorePath(fileStore, pathWithFileName);
        }

        try {
            Files.move(srcPath.absolutePath, dstPath.absolutePath);
            return dstPath;
        } catch (FileAlreadyExistsException e) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileExists", dstPath.standardizedPath()));
        } catch (Exception e) {
            throw new FileStoreException(new TranslatableMessage("filestore.errorMovingFile"));
        }
    }

    public FileStorePath copyFileOrFolder(String xid, String src, String dst) {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        FileStorePath srcPath = getPathWithinFileStore(fileStore, src);

        if (!Files.exists(srcPath.absolutePath)) {
            throw new NotFoundException();
        }

        FileStorePath dstPath = srcPath.getParent().resolve(Paths.get(dst));
        if (Files.isDirectory(dstPath.absolutePath)) {
            Path pathWithFileName = dstPath.absolutePath.resolve(srcPath.absolutePath.getFileName());
            dstPath = new FileStorePath(fileStore, pathWithFileName);
        }

        if (Files.exists(dstPath.absolutePath)) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileExists", dstPath.standardizedPath()));
        }

        try {
            if (Files.isDirectory(srcPath.absolutePath)) {
                FileUtils.copyDirectory(srcPath.absolutePath.toFile(), dstPath.absolutePath.toFile());
            } else {
                Files.copy(srcPath.absolutePath, dstPath.absolutePath);
            }
            return dstPath;
        } catch (FileAlreadyExistsException e) {
            throw new FileStoreException(new TranslatableMessage("filestore.fileExists", dstPath.standardizedPath()));
        } catch (Exception e) {
            throw new FileStoreException(new TranslatableMessage("filestore.errorCopyingFile"));
        }
    }

    public FileStorePath createDirectory(String xid, String toCreate) {
        FileStorePath toCreatePath = forWrite(xid, toCreate);
        try {
            // createDirectories fails if the path is a symlink to an existing directory
            // check if it is a directory (with follow symlinks) first to prevent this
            if (!Files.isDirectory(toCreatePath.absolutePath)) {
                Files.createDirectories(toCreatePath.absolutePath);
            }
            return toCreatePath;
        } catch (Exception e) {
            throw new FileStoreException(new TranslatableMessage("filestore.errorCreatingDirectory"));
        }
    }

    private Path getFileStoreRoot(FileStore fileStore) {
        return this.fileStoreRoot.resolve(fileStore.getXid());
    }

    private FileStorePath getPathWithinFileStore(FileStore fileStore, String path) {
        Path root = getFileStoreRoot(fileStore);
        Path filePath = root.resolve(path).toAbsolutePath().normalize();
        if (!filePath.startsWith(root)) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.invalidPath"));
        }
        return new FileStorePath(fileStore, filePath);
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has write permission for the file store.
     * Does not check if the file exists, only that the filestore exists.
     *
     * @param xid  xid of the user file store, or the storeName of the {@link FileStoreDefinition}
     * @param path
     * @return
     * @throws PermissionException user does not have permission to write to the file store
     * @throws NotFoundException   file store does not exist
     */
    public Path getPathForWrite(String xid, String path) throws PermissionException, NotFoundException {
        return forWrite(xid, path).getAbsolutePath();
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has read permission for the file store.
     * Does not check if the file exists, only that the filestore exists.
     *
     * @param xid  xid of the user file store, or the storeName of the {@link FileStoreDefinition}
     * @param path
     * @return
     * @throws PermissionException user does not have permission to read from the file store
     * @throws NotFoundException   file store does not exist
     */
    public Path getPathForRead(String xid, String path) throws PermissionException, NotFoundException {
        return forRead(xid, path).getAbsolutePath();
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has write permission for the file store.
     * Does not check if the file exists, only that the filestore exists.
     *
     * @param xid  xid of the user file store, or the storeName of the {@link FileStoreDefinition}
     * @param path
     * @return
     * @throws PermissionException user does not have permission to write to the file store
     * @throws NotFoundException   file store does not exist
     */
    public FileStorePath forWrite(String xid, String path) throws PermissionException, NotFoundException {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureEditPermission(Common.getUser(), fileStore);
        return getPathWithinFileStore(fileStore, path);
    }

    /**
     * Retrieves the path to a file in the file store, checking that the user has read permission for the file store.
     * Does not check if the file exists, only that the filestore exists.
     *
     * @param xid  xid of the user file store, or the storeName of the {@link FileStoreDefinition}
     * @param path
     * @return
     * @throws PermissionException user does not have permission to read from the file store
     * @throws NotFoundException   file store does not exist
     */
    public FileStorePath forRead(String xid, String path) throws PermissionException, NotFoundException {
        FileStore fileStore = getWithoutPermissionCheck(xid);
        ensureReadPermission(Common.getUser(), fileStore);
        return getPathWithinFileStore(fileStore, path);
    }

    private FileStorePath resolveFileStore(Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();
        if (!absolutePath.startsWith(fileStoreRoot)) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.invalidPath"));
        }
        Path relative = fileStoreRoot.relativize(absolutePath);
        if (relative.getNameCount() == 0) {
            throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.invalidPath"));
        }
        String fileStoreName = relative.getName(0).toString();
        FileStore fileStore = getWithoutPermissionCheck(fileStoreName);
        return new FileStorePath(fileStore, absolutePath);
    }

    /**
     * @param path
     * @throws IllegalArgumentException if path is not located inside the filestore root
     * @throws NotFoundException        if filestore was not found
     * @throws PermissionException      filestore exists but user does not have read access
     */
    public void ensureReadAccess(Path path) throws IllegalArgumentException, NotFoundException, PermissionException {
        FileStore fileStore = resolveFileStore(path).getFileStore();
        ensureReadPermission(Common.getUser(), fileStore);
    }

    /**
     * @param path
     * @throws IllegalArgumentException if path is not located inside the filestore root
     * @throws NotFoundException        if filestore was not found
     * @throws PermissionException      filestore exists but user does not have write access
     */
    public void ensureWriteAccess(Path path) throws IllegalArgumentException, NotFoundException, PermissionException {
        FileStore fileStore = resolveFileStore(path).getFileStore();
        ensureEditPermission(Common.getUser(), fileStore);
    }

    public static class FileStoreException extends TranslatableRuntimeException {
        public FileStoreException(TranslatableMessage message, Throwable cause) {
            super(message, cause);
        }

        public FileStoreException(TranslatableMessage message) {
            super(message);
        }
    }

    public class FileStorePath {
        private final FileStore fileStore;
        private final Path absolutePath;

        private FileStorePath(FileStore fileStore, Path absolutePath) {
            this.fileStore = fileStore;
            this.absolutePath = absolutePath;
        }

        public FileStore getFileStore() {
            return fileStore;
        }

        public Path getAbsolutePath() {
            return absolutePath;
        }

        public Path getRelativePath() {
            return getFileStoreRoot().relativize(this.absolutePath);
        }

        public Path getFileStoreRoot() {
            return FileStoreService.this.getFileStoreRoot(this.fileStore);
        }

        public String standardizedPath() {
            return getRelativePath().toString().replace(File.separatorChar, '/');
        }

        public FileStorePath resolve(Path other) {
            Path newPath = absolutePath.resolve(other).toAbsolutePath().normalize();
            if (!newPath.startsWith(getFileStoreRoot())) {
                throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.invalidPath"));
            }
            return new FileStorePath(fileStore, newPath);
        }

        public FileStorePath getParent() {
            Path newPath = absolutePath.getParent();
            if (!newPath.startsWith(getFileStoreRoot())) {
                throw new TranslatableIllegalArgumentException(new TranslatableMessage("filestore.invalidPath"));
            }
            return new FileStorePath(fileStore, newPath);
        }
    }
}
