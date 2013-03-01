package com.serotonin.m2m2.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.User;

/**
 * Used for overriding the default MA pages.
 * 
 * @author Matthew Lohbihler
 */
abstract public class DefaultPagesDefinition extends ModuleElementDefinition {
    public static String getDefaultUri(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;

        if (user == null)
            uri = getLoginUri(request, response);
        else {
            // If this is the first login to the instance by an admin...
            if (user.isAdmin() && SystemSettingsDao.getBooleanValue(SystemSettingsDao.NEW_INSTANCE, false)) {
                // Remove the flag
                new SystemSettingsDao().removeValue(SystemSettingsDao.NEW_INSTANCE);

                // If there is a page to which to forward, do so. This could be null.
                uri = DefaultPagesDefinition.getFirstLoginUri(request, response);
            }

            if (uri == null) {
                if (user.isFirstLogin())
                    uri = DefaultPagesDefinition.getFirstUserLoginUri(request, response, user);
                if (StringUtils.isBlank(uri))
                    uri = DefaultPagesDefinition.getLoggedInUriPreHome(request, response, user);
                if (StringUtils.isBlank(uri))
                    uri = user.getHomeUrl();
                if (StringUtils.isBlank(uri))
                    uri = DefaultPagesDefinition.getLoggedInUri(request, response, user);
            }
        }

        return uri;
    }

    private static String getLoginUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getLoginPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private static String getFirstLoginUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getFirstLoginPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private static String getFirstUserLoginUri(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getFirstUserLoginPageUri(request, response, user);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private static String getLoggedInUriPreHome(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getLoggedInPageUriPreHome(request, response, user);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    private static String getLoggedInUri(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getLoggedInPageUri(request, response, user);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    /**
     * Returns the URI of the login page to use. The default value is "/login.htm". If this method returns null, the
     * next definition (if available) will be used. Results are not cached, so the definition can vary its response
     * contextually.
     * 
     * @return the URI of the login page to use, or null.
     */
    @SuppressWarnings("unused")
    public String getLoginPageUri(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * Returns the URI of the page to use following the first login to the instance. If this method returns null, the
     * next definition (if available) will be used. This method will only be called *once*, so if any manner of
     * redirect is used the implementation must do its own handling accordingly.
     * 
     * @return the URI of the page to use following the first login, or null.
     */
    @SuppressWarnings("unused")
    public String getFirstLoginPageUri(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * Returns the URI of the page to use following the first login of the user. The getFirstLoginPageUri takes
     * precedence to this method if the login was the first for the instance. If this method returns null, the next
     * definition (if available) will be used.
     * 
     * @param user
     *            the user who has just logged in.
     * @return the URI of the page to use following the user's first login, or null.
     */
    @SuppressWarnings("unused")
    public String getFirstUserLoginPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
        return null;
    }

    /**
     * Returns the URI of the default page to use for logged in users, overriding the user's home URL setting. The
     * default value is null. This method should be used with caution, as the user's preference should normally take
     * precedence. If this method returns null, the next definition (if available) will be used. Results are not
     * cached, so the definition can vary its response contextually.
     * 
     * @return the URI of the default logged in page to use, or null.
     */
    @SuppressWarnings("unused")
    public String getLoggedInPageUriPreHome(HttpServletRequest request, HttpServletResponse response, User user) {
        return null;
    }

    /**
     * Returns the URI of the default page to use for logged in users. The default value is "/data_point_details.shtm".
     * If this method returns null, the next definition (if available) will be used. Results are not cached, so the
     * definition can vary its response contextually.
     * 
     * @return the URI of the default logged in page to use, or null.
     */
    @SuppressWarnings("unused")
    public String getLoggedInPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
        return null;
    }
}
