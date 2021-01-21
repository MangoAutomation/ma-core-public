package com.serotonin.m2m2.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.m2m2.vo.User;

/**
 * Used for overriding the default Mango pages.
 *
 * @author Matthew Lohbihler
 */
abstract public class DefaultPagesDefinition extends ModuleElementDefinition {

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
