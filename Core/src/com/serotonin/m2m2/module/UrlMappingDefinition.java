/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.web.mvc.UrlHandler;

/**
 * A URL mapping definition creates a page - and optionally a menu item - accessible by m2m2 users.
 * 
 * @author Matthew Lohbihler
 */
abstract public class UrlMappingDefinition extends ModuleElementDefinition {
    /**
     * The available permission types for the URL. This value determines whether the URL link is displayed to a given
     * user, and also whether access is provided to a user when a request is made (in case the user manually constructs
     * a link to the resource).
     */
    public enum Permission {
        ANONYMOUS, USER, DATA_SOURCE, ADMINISTRATOR;
    }

    public final String getMenuImagePath() {
        return "/" + Constants.DIR_MODULES + "/" + getModule().getName() + "/" + getMenuImage();
    }

    /**
     * The absolute URL path. Required.
     * 
     * @return the absolute URL path.
     */
    abstract public String getUrlPath();

    /**
     * An instance of the handler for the URL. Called once upon startup, so the instance must be reusable and thread
     * safe. If null, a default handler will be created which forwards to the the JSP path.
     * 
     * @return an instance of the URL handler
     */
    abstract public UrlHandler getHandler();

    /**
     * The path to the JSP file that renders the page at this URL. The path is relative to the module directory.
     * Required if the UrlHandler is null.
     * 
     * @return the path to the JSP file.
     */
    abstract public String getJspPath();

    /**
     * If not null, this is the reference key to the description that will be provided in the menu for this URL.
     * 
     * @return the reference key to the description.
     */
    abstract public String getMenuKey();

    /**
     * If the menu key is not null, this is a path relative to the module to the image to use for the URL in the menu.
     * 
     * @return the path to the menu image
     */
    abstract public String getMenuImage();

    /**
     * The permission to use when allowing access to the URL, or displaying the URL to a user.
     * 
     * @return the URL's permission level
     */
    abstract public Permission getPermission();

    /**
     * The value of the HTML target attribute that will appear in the menu link. If null, no target attribute will be
     * written.
     */
    public String getTarget() {
        return null;
    }
}
