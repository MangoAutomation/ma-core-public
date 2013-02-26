/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.web.mvc.UrlHandler;

/**
 * A URL mapping definition creates a page - and optionally a menu item - accessible by MA users.
 * 
 * @author Matthew Lohbihler
 * @deprecated Use {@link MenuItemDefinition} to create menu entries. Use {@link UriMappingDefinition} to create mapping
 *             definitions.
 */
@Deprecated
abstract public class UrlMappingDefinition extends MenuItemDefinition {
    public enum Permission {
        ANONYMOUS, USER, DATA_SOURCE, ADMINISTRATOR;
    }

    abstract public Permission getPermission();

    @Override
    public Visibility getVisibility() {
        switch (getPermission()) {
        case ANONYMOUS:
            return Visibility.ANONYMOUS;
        case USER:
            return Visibility.USER;
        case DATA_SOURCE:
            return Visibility.DATA_SOURCE;
        case ADMINISTRATOR:
            return Visibility.ADMINISTRATOR;
        }
        return null;
    }

    public String getMenuImagePath() {
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
     * The value of the HTML target attribute that will appear in the menu link. If null, no target attribute will be
     * written.
     */
    public String getTarget() {
        return null;
    }

    @Override
    public String getHref(HttpServletRequest request, HttpServletResponse response) {
        return getUrlPath();
    }

    @Override
    public String getTextKey(HttpServletRequest request, HttpServletResponse response) {
        return getMenuKey();
    }

    @Override
    public String getImage(HttpServletRequest request, HttpServletResponse response) {
        return getMenuImage();
    }

    @Override
    public String getTarget(HttpServletRequest request, HttpServletResponse response) {
        return getTarget();
    }
}
