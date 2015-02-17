/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.DataPointNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.util.TimeZoneUtils;

public class UsersDwr extends BaseDwr {
    //    @DwrPermission(user = true)
    //    public int usersAllowed() {
    //        if (cntMax == 0)
    //            cntMax = Integer.parseInt(SystemSettingsDao.getValue(SystemSettingsDao.USER_CNT));
    //        return cntMax;
    //    }
    //
    @DwrPermission(user = true)
    public Map<String, Object> getInitData() {
        Map<String, Object> initData = new HashMap<>();

        User user = Common.getUser();
        if (Permissions.hasAdmin(user)) {
            //            initData.put("allowed", usersAllowed());

            // Users
            initData.put("admin", true);
            initData.put("users", new UserDao().getUsers());

            // Data sources
            List<DataSourceVO<?>> dataSourceVOs = new DataSourceDao().getDataSources();
            List<Map<String, Object>> dataSources = new ArrayList<>(dataSourceVOs.size());
            Map<String, Object> ds, dp;
            List<Map<String, Object>> points;
            DataPointDao dataPointDao = new DataPointDao();
            for (DataSourceVO<?> dsvo : dataSourceVOs) {
                ds = new HashMap<>();
                ds.put("id", dsvo.getId());
                ds.put("name", dsvo.getName());
                points = new LinkedList<>();
                for (DataPointVO dpvo : dataPointDao.getDataPoints(dsvo.getId(), DataPointNameComparator.instance,
                        false)) {
                    dp = new HashMap<>();
                    dp.put("id", dpvo.getId());
                    dp.put("name", dpvo.getName());
                    dp.put("deviceName", dpvo.getDeviceName());
                    dp.put("settable", dpvo.getPointLocator().isSettable());
                    points.add(dp);
                }
                ds.put("points", points);
                dataSources.add(ds);
            }
            initData.put("dataSources", dataSources);
        }
        else
            initData.put("user", user);

        initData.put("timezones", TimeZoneUtils.getTimeZoneIds());

        return initData;
    }

    @DwrPermission(admin = true)
    public User getUser(int id) {
        if (id == Common.NEW_ID)
            return new User();
        return new UserDao().getUser(id);
    }

    @DwrPermission(admin = true)
    public ProcessResult saveUserAdmin(int id, String username, String password, String email, String phone,
            boolean admin, boolean disabled, int receiveAlarmEmails, boolean receiveOwnAuditEvents, String timezone,
            String permissions) {
        // Validate the given information. If there is a problem, return an appropriate error message.
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        User currentUser = Common.getUser(request);
        UserDao userDao = new UserDao();

        User user;
        if (id == Common.NEW_ID)
            user = new User();
        else
            user = userDao.getUser(id);
        user.setUsername(username);
        if (!StringUtils.isBlank(password))
            user.setPassword(Common.encrypt(password));
        user.setEmail(email);
        user.setPhone(phone);
        user.setAdmin(admin);
        user.setDisabled(disabled);
        user.setReceiveAlarmEmails(receiveAlarmEmails);
        user.setReceiveOwnAuditEvents(receiveOwnAuditEvents);
        user.setTimezone(timezone);
        user.setPermissions(permissions);

        ProcessResult response = new ProcessResult();
        user.validate(response);

        // Check if the username is unique.
        User dupUser = userDao.getUser(username);
        if (id == Common.NEW_ID && dupUser != null)
            response.addMessage(new TranslatableMessage("users.validate.usernameUnique"));
        else if (dupUser != null && id != dupUser.getId())
            response.addMessage(new TranslatableMessage("users.validate.usernameInUse"));

        // Cannot make yourself disabled or not admin
        if (currentUser.getId() == id) {
            if (!admin)
                response.addMessage(new TranslatableMessage("users.validate.adminInvalid"));
            if (disabled)
                response.addMessage(new TranslatableMessage("users.validate.adminDisable"));
        }

        if (!response.getHasMessages()) {
            userDao.saveUser(user);

            if (currentUser.getId() == id)
                // Update the user object in session too. Why not?
                Common.setUser(request, user);

            response.addData("userId", user.getId());
        }

        return response;
    }

    //    /**
    //     * Disable user based on number of allowed users
    //     * 
    //     * @param user
    //     *            the user to check
    //     * @return true to enable false to disable
    //     */
    //    private boolean checkDisable(User user) {
    //        cntMax = Integer.parseInt(SystemSettingsDao.getValue(SystemSettingsDao.USER_CNT));
    //        if (user.isDefaultUser())
    //            return false;
    //
    //        return userCnt > cntMax;
    //
    //        /***********************
    //         * String userCntHash = SystemSettingsDao.getValue(SystemSettingsDao.USER_CNT);
    //         * if (userCntHash.equals(CoreLicenseDefinition.UNLIMITED_USER_CNT))
    //         * return false;
    //         * 
    //         * cntMax = 1;
    //         * if (userCntHash.equals(CoreLicenseDefinition.FIVE_USER_CNT))
    //         * cntMax = 5;
    //         * if (userCntHash.equals(CoreLicenseDefinition.TEN_USER_CNT))
    //         * cntMax = 10;
    //         * if (userCntHash.equals(CoreLicenseDefinition.TWENTY_USER_CNT))
    //         * cntMax = 20;
    //         * return userCnt > cntMax;
    //         ***************************/
    //    }

    @DwrPermission(user = true)
    public ProcessResult saveUser(int id, String password, String email, String phone, int receiveAlarmEmails,
            boolean receiveOwnAuditEvents, String timezone) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        User user = Common.getUser(request);
        if (user.getId() != id)
            throw new PermissionException("Cannot update a different user", user);

        UserDao userDao = new UserDao();
        User updateUser = userDao.getUser(id);
        if (!StringUtils.isBlank(password))
            updateUser.setPassword(Common.encrypt(password));
        updateUser.setEmail(email);
        updateUser.setPhone(phone);
        updateUser.setReceiveAlarmEmails(receiveAlarmEmails);
        updateUser.setReceiveOwnAuditEvents(receiveOwnAuditEvents);
        updateUser.setTimezone(timezone);

        ProcessResult response = new ProcessResult();
        updateUser.validate(response);

        if (!response.getHasMessages()) {
            userDao.saveUser(updateUser);

            // Update the user object in session too. Why not?
            Common.setUser(request, updateUser);
        }

        return response;
    }

    @DwrPermission(admin = true)
    public Map<String, Object> sendTestEmail(String email, String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            Translations translations = Common.getTranslations();
            Map<String, Object> model = new HashMap<>();
            model.put("message", new TranslatableMessage("ftl.userTestEmail", username));
            MangoEmailContent cnt = new MangoEmailContent("testEmail", model, translations,
                    translations.translate("ftl.testEmail"), Common.UTF8);
            EmailWorkItem.queueEmail(email, cnt);
            result.put("message", new TranslatableMessage("common.testEmailSent", email));
        }
        catch (Exception e) {
            result.put("exception", e.getMessage());
        }
        return result;
    }

    @DwrPermission(admin = true)
    public ProcessResult deleteUser(int id) {
        ProcessResult response = new ProcessResult();
        User currentUser = Common.getUser();

        if (currentUser.getId() == id)
            // You can't delete yourself.
            response.addMessage(new TranslatableMessage("users.validate.badDelete"));
        else
            new UserDao().deleteUser(id);

        return response;
    }

    /**
     * Create a copy of a User
     * 
     * @param id
     * @return
     */
    @DwrPermission(admin = true)
    public ProcessResult getCopy(int id) {
        ProcessResult result = new ProcessResult();

        UserDao dao = new UserDao();
        User existing = dao.getUser(id);

        User newUser = new User();
        newUser.setUsername("Copy of " + existing.getUsername());

        newUser.setEmail(existing.getEmail());
        newUser.setAdmin(existing.isAdmin());
        newUser.setPhone(existing.getPhone());
        newUser.setDisabled(existing.isDisabled());
        newUser.setReceiveAlarmEmails(existing.getReceiveAlarmEmails());
        newUser.setReceiveOwnAuditEvents(existing.isReceiveOwnAuditEvents());
        newUser.setTimezone(existing.getTimezone());
        newUser.setPermissions(existing.getPermissions());

        result.addData("vo", newUser);

        return result;

    }

    /**
     * Switch to new user if admin
     * 
     * @param username
     * @return
     */
    @DwrPermission(admin = true)
    public ProcessResult su(String username) {
        ProcessResult result = new ProcessResult();
        UserDao userDao = new UserDao();
        User user = userDao.getUser(username);

        if (user == null) {
            //No User Found
            result.addMessage("suMessage", new TranslatableMessage("login.validation.noSuchUser"));

            return result;
        }

        if (user.isDisabled()) {
            result.addMessage("suMessage", new TranslatableMessage("common.userDisabled", user.getUsername()));
            return result;
        }

        // Update the last login time.
        userDao.recordLogin(user.getId());

        WebContext ctx = WebContextFactory.get();

        // Add the user object to the session. This indicates to the rest of the application whether the user is logged 
        // in or not. Will replace any existing user object.
        Common.setUser(ctx.getHttpServletRequest(), user);

        for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class))
            def.postLogin(user);

        //Add Message now
        //Don't add messages or the page won't refresh: result.addMessage("suMessage", new TranslatableMessage("common.user.suUserComplete",user.getUsername()));
        //If we want to redirect... String uri = DefaultPagesDefinition.getDefaultUri(request, response, user);        
        return result;
    }

}
