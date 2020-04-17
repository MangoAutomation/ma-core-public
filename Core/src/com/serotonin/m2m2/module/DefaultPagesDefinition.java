package com.serotonin.m2m2.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.User;

/**
 * Used for overriding the default MA pages.
 *
 * @author Matthew Lohbihler
 */
abstract public class DefaultPagesDefinition extends ModuleElementDefinition {

    /**
     * Container for login URI info
     * @author Terry Packer
     *
     */
    public static class LoginUriInfo {

        //Default uri for a user to login to
        private String uri;
        //Does this URI need to be accessed to update a license agreement or other setting
        private boolean required;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }

    /**
     * Get the information about the default login URI for a user
     * @param request
     * @param response
     * @param user
     * @return
     */
    public static LoginUriInfo getDefaultUriInfo(HttpServletRequest request, HttpServletResponse response, User user) {

        LoginUriInfo info = new LoginUriInfo();

        if (user == null)
            info.uri = getLoginUri(request, response);
        else {
            PermissionService service = Common.getBean(PermissionService.class);
            // If this is the first login to the instance by an admin...
            if (service.hasAdminRole(user) && SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.NEW_INSTANCE)) {
                // Remove the flag
                SystemSettingsDao.instance.removeValue(SystemSettingsDao.NEW_INSTANCE);

                // If there is a page to which to forward, do so. This could be null.
                info.uri = DefaultPagesDefinition.getFirstLoginUri(request, response);
                info.required = true;

            }else if(service.hasAdminRole(user) && (SystemSettingsDao.instance.getIntValue(SystemSettingsDao.LICENSE_AGREEMENT_VERSION) != Common.getLicenseAgreementVersion())) {
                //When a new license version has been released but it is NOT the first login.
                info.uri = DefaultPagesDefinition.getAdminLicenseUpgradeLoginUri(request, response);
                info.required = true;
            }

            if (info.uri == null) {
                if (user.isFirstLogin())
                    info.uri = DefaultPagesDefinition.getFirstUserLoginUri(request, response, user);
                if (StringUtils.isBlank(info.uri))
                    info.uri = DefaultPagesDefinition.getLoggedInUriPreHome(request, response, user);
                if (StringUtils.isBlank(info.uri))
                    info.uri = user.getHomeUrl();
                if (StringUtils.isBlank(info.uri))
                    info.uri = DefaultPagesDefinition.getLoggedInUri(request, response, user);
            }
        }

        return info;
    }

    /**
     * Deprecated, to be removed with legacy UI.
     * @param request
     * @param response
     * @param user
     * @return
     */
    @Deprecated
    public static String getDefaultUri(HttpServletRequest request, HttpServletResponse response, User user) {
        LoginUriInfo info = getDefaultUriInfo(request, response, user);
        return info.uri;
    }

    public static String getUnauthorizedUri(HttpServletRequest request, HttpServletResponse response, User user) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getUnauthorizedPageUri(request, response, user);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    public static String getLoginUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getLoginPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    public static String getPasswordResetUri() {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getPasswordResetPageUri();
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    public static String getEmailVerificationUri() {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getEmailVerificationPageUri();
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

    private static String getAdminLicenseUpgradeLoginUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getAdminLicenseUpgradeLoginPageUri(request, response);
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

    public static String getNotFoundUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getNotFoundPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    public static String getErrorUri(HttpServletRequest request, HttpServletResponse response) {
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getErrorPageUri(request, response);
            if (!StringUtils.isBlank(uri))
                break;
        }
        return uri;
    }

    public static String getStartupUri(HttpServletRequest request, HttpServletResponse response){
        String uri = null;
        for (DefaultPagesDefinition def : ModuleRegistry.getDefinitions(DefaultPagesDefinition.class)) {
            uri = def.getStartupPageUri(request, response);
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
    public String getLoginPageUri(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * Returns the URI of the password reset page to use. The default value is "/ui/login". If this method returns null, the
     * next definition (if available) will be used.
     *
     * @return the URI of the login page to use, or null.
     */
    public String getPasswordResetPageUri() {
        return null;
    }

    /**
     * Returns the URI of the email verification page to use. The default value is "/ui/verify-email". If this method returns null, the
     * next definition (if available) will be used.
     *
     * @return the URI of the login page to use, or null.
     */
    public String getEmailVerificationPageUri() {
        return null;
    }

    /**
     * Returns the URI of the page to use following the first login to the instance. If this method returns null, the
     * next definition (if available) will be used. This method will only be called *once*, so if any manner of
     * redirect is used the implementation must do its own handling accordingly.
     *
     * @return the URI of the page to use following the first login, or null.
     */
    public String getFirstLoginPageUri(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * Returns the URI of the page to use following an upgraded license. If this method returns null, the
     * next definition (if available) will be used. This method will only be called *once*, so if any manner of
     * redirect is used the implementation must do its own handling accordingly.
     *
     * @return the URI of the page to use following license upgrades, or null.
     */
    public String getAdminLicenseUpgradeLoginPageUri(HttpServletRequest request, HttpServletResponse response) {
        return getFirstLoginPageUri(request, response);
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
    public String getFirstUserLoginPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
        return null;
    }

    /**
     * Returns the URI of the page to which to direct a user who attempts to access a page to which they have
     * insufficient authority. The default value is "unauthorized.htm". If this method returns null, the next
     * definition (if available) will be used. Results are not cached, so the definition can vary its response
     * contextually.
     *
     * @param request
     * @param response
     * @param user - can be null if this resource is accessed before login
     * @return the URI of the default logged in page to use, or null.
     */
    public String getUnauthorizedPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
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
    public String getLoggedInPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
        return null;
    }


    /**
     * Returns the URI of a custom error page.
     *
     * @see org.eclipse.jetty.server.handler.ErrorHandler for how to get information out of
     * the response about the error
     *
     * @param request
     * @param response
     * @return URI of page or null
     */
    public String getErrorPageUri(HttpServletRequest request, HttpServletResponse response){
        return null;
    }

    /**
     * Return the 404 Error page
     * @param request
     * @param response
     * @return URI of page or null
     */
    public String getNotFoundPageUri(HttpServletRequest request, HttpServletResponse response){
        return null;
    }

    /**
     * Return the startup page.  This call cannot depend on the database or any other Mango subsystems
     * since it is called before any systems are initialized
     * @param request
     * @param response
     * @return URI of page or null
     */
    public String getStartupPageUri(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }
}
