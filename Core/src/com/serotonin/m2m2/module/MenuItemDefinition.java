package com.serotonin.m2m2.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.m2m2.Constants;

abstract public class MenuItemDefinition extends ModuleElementDefinition {
    /**
     * The available visibility types for the menu item. This value determines whether the item is displayed to a given
     * user.
     */
    public enum Visibility {
        ANONYMOUS, USER, DATA_SOURCE, ADMINISTRATOR;
    }

    /**
     * Prepends the getImage result with the module path. This should normally not be overridden.
     * 
     * @param request
     * @param response
     * @return
     */
    public String getImagePath(HttpServletRequest request, HttpServletResponse response) {
        return "/" + Constants.DIR_MODULES + "/" + getModule().getName() + "/" + getImage(request, response);
    }

    /**
     * The permission to use when displaying the menu item to a user.
     * 
     * @return the URL's permission level
     */
    abstract public Visibility getVisibility();

    /**
     * If not null, this is the reference key to the description that will be provided in the menu for this URL.
     * 
     * @return the reference key to the description.
     */
    abstract public String getTextKey(HttpServletRequest request, HttpServletResponse response);

    /**
     * The path relative to the module to the image to use for the menu item.
     * 
     * @return the path to the menu image
     */
    abstract public String getImage(HttpServletRequest request, HttpServletResponse response);

    /**
     * Allows the mapping to determine its visibility at runtime based upon the request and response.
     * 
     * @param request
     *            the current request
     * @param response
     *            the current response
     * @return if true the menu item is visible
     */
    public boolean isVisible(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    /**
     * The value of the HTML href attribute to use in the menu item. If null, no attribute will be written.
     * 
     * @param request
     *            the current request
     * @param response
     *            the current response
     * @return the href value to use
     */
    public String getHref(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * The value of the HTML onclick attribute to use in the menu item. If null, no attribute will be written.
     * 
     * @param request
     *            the current request
     * @param response
     *            the current response
     * @return the onclick value to use
     */
    public String getOnclick(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * The value of the HTML id attribute to use in the menu item. If null, no attribute will be written.
     * 
     * @param request
     *            the current request
     * @param response
     *            the current response
     * @return the id value to use
     */
    public String getId(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * The value of the HTML target attribute to use in the menu item. If null, no attribute will be written.
     * 
     * @param request
     *            the current request
     * @param response
     *            the current response
     * @return the target value to use
     */
    public String getTarget(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * The relative ordering of the menu item from left to right compared to other menu items, from 0 to 100. Items are
     * first categorized by their visibility, and then ordered within. Items added by the core have a value of 50.
     * 
     * @return the relative order of the menu item
     */
    public int getOrder() {
        return 50;
    }
}
