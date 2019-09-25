/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.internet.AddressException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.definitions.permissions.UserEditSelfPermission;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionDetails;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

import freemarker.template.TemplateException;

/**
 * Service to access Users
 * 
 * NOTES:
 *  Users are cached by username
 * 
 *  by using any variation of the get(String, user) methods you are returned 
 *   a cached user, any modifications to this will result in changes to a session user
 *   to avoid this use the get(Integer, user) variations 
 *
 * @author Terry Packer
 *
 */
@Service
public class UsersService extends AbstractVOService<User, UserDao> {

    private final SystemSettingsDao systemSettings;
    
    @Autowired
    public UsersService(UserDao dao, SystemSettingsDao systemSettings) {
        super(dao);
        this.systemSettings = systemSettings;
    }

    /*
     * Nice little hack since Users don't have an XID.
     */
    @Override
    protected User get(String username, PermissionHolder user, boolean full)
            throws NotFoundException, PermissionException {
        User vo = dao.getUser(username);
        if(vo == null)
            throw new NotFoundException();
        ensureReadPermission(user, vo);
        return vo;
    }

    /**
     * 
     * Get a user by their email address
     * 
     * @param emailAddress
     * @return
     */
    public User getUserByEmail(String emailAddress, PermissionHolder holder) throws NotFoundException, PermissionException {
        User vo =  dao.getUserByEmail(emailAddress);
        if(vo == null)
            throw new NotFoundException();
        ensureReadPermission(holder, vo);
        return vo;
    }
    
    @Override
    protected User insert(User vo, PermissionHolder user, boolean full)
            throws PermissionException, ValidationException {
        //Ensure they can create
        ensureCreatePermission(user, vo);

        //Ensure id is not set
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        ensureValid(vo, user);
        dao.saveUser(vo);
        return vo;
    }

    @Override
    protected User update(User existing, User vo, PermissionHolder user, boolean full)
            throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());

        String newPassword = vo.getPassword();
        if (StringUtils.isBlank(newPassword)) {
            // just use the old password
            vo.setPassword(existing.getPassword());
        }

        ensureValid(existing, vo, user);
        dao.saveUser(vo);
        return vo;
    }

    @Override
    public User delete(String xid, PermissionHolder user) throws PermissionException, NotFoundException {
        User vo = get(xid, user);

        //You cannot delete yourself
        if (user instanceof User && ((User) user).getId() == vo.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.badDelete"), user);

        //Only admin can delete
        user.ensureHasAdminPermission();

        dao.deleteUser(vo.getId());
        return vo;
    }

    /**
     * Lock a user's password
     * @param username
     * @param user
     * @throws PermissionException
     * @throws NotFoundException
     */
    public void lockPassword(String username, PermissionHolder user)
            throws PermissionException, NotFoundException {
        user.ensureHasAdminPermission();
        User toLock = get(username, user);
        if (user instanceof User && ((User) user).getId() == toLock.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.cannotLockOwnPassword"), user);
        dao.lockPassword(toLock);
    }


    /**
     * Get User Permissions Information for all users, exclude provided groups in query
     * @param query
     * @param user
     * @return
     */
    public Set<PermissionDetails> getPermissionDetails(String query, PermissionHolder user) {
        Set<PermissionDetails> details = new TreeSet<>();
        for (User u : dao.getActiveUsers()){
            PermissionDetails deets = Permissions.getPermissionDetails(user, query, u);
            if(deets != null)
                details.add(deets);
        }
        return details;
    }

    /**
     * Get All User Groups that a user can 'see', exclude any groups listed
     * @param exclude
     * @param user
     * @return
     */
    public Set<String> getUserGroups(Collection<String> exclude, PermissionHolder user) {
        Set<String> groups = new TreeSet<>();
        groups.addAll(user.getPermissionsSet());

        if (user.hasAdminPermission()) {
            for (User u : this.dao.getActiveUsers())
                groups.addAll(u.getPermissionsSet());
        }

        if (exclude != null) {
            for (String part : exclude)
                groups.remove(part);
        }

        return groups;
    }


    @Override
    public ProcessResult validate(User vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        vo.validate(result);
        return result;
    }

    @Override
    public ProcessResult validate(User existing, User vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();

        //Things we cannot do to ourselves
        if (user instanceof User && ((User) user).getId() == existing.getId()) {

            //Cannot disable
            if(vo.isDisabled()) {
                result.addContextualMessage("disabled", "users.validate.adminDisable");
            }else {
                //If we are disabled this check will throw an exception, we are invalid anyway so 
                // don't check
                //Cannot remove admin permission
                if(existing.hasAdminPermission())
                    if(!vo.hasAdminPermission())
                        result.addContextualMessage("permissions", "users.validate.adminInvalid");
            }
        }

        //Things we cannot do as non-admin
        if (!user.hasAdminPermission()) {
            if (!vo.getPermissionsSet().equals(existing.getPermissionsSet())) {
                result.addContextualMessage("permissions", "users.validate.cannotChangePermissions");
            }
        }
//For now this is done in the VO.validate() method as it already validates the existing VO
//        //Cannot Rename a User to an existing Username
//        if(!StringUtils.equals(vo.getUsername(), existing.getUsername())){
//            User existingUser = this.dao.getUser(vo.getUsername());
//            if(existingUser != null){
//                result.addContextualMessage("username", "users.validate.usernameInUse");
//            }
//        }

        vo.validate(result);
        return result;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, User vo) {
        if(user.hasAdminPermission()) {
            return true;
        }else {
            return false;
        }
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, User vo) {
        if(user.hasAdminPermission())
            return true;
        else if (user instanceof User && ((User) user).getId()  == vo.getId() && Permissions.hasAnyPermission(user, Permissions.explodePermissionGroups(SystemSettingsDao.instance.getValue(UserEditSelfPermission.PERMISSION))))
            return true;
        else
            return false;
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, User vo) {
        if(user.hasAdminPermission())
            return true;
        else if (user instanceof User && ((User) user).getId()  == vo.getId())
            return true;
        else
            return false;
    }

    /**
     * @param username
     * @param sendEmail
     * @param user
     * @return
     * @throws IOException 
     * @throws TemplateException 
     * @throws AddressException 
     */
    public User approveUser(String username, boolean sendEmail, PermissionHolder user) throws PermissionException, NotFoundException, TemplateException, IOException, AddressException {
        User existing = get(username, user);
        User approved = existing.copy();
        approved.setDisabled(false);
        update(existing, approved, user);
        
        Translations translations = existing.getTranslations();
        Map<String, Object> model = new HashMap<>();
        TranslatableMessage subject = new TranslatableMessage("ftl.userApproved.subject", this.systemSettings.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        MangoEmailContent content = new MangoEmailContent("accountApproved", model, translations, subject.translate(translations), Common.UTF8);
        EmailWorkItem.queueEmail(existing.getEmail(), content);
        
        return approved;
    }

}
