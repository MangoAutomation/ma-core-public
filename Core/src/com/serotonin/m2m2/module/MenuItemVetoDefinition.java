package com.serotonin.m2m2.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

abstract public class MenuItemVetoDefinition extends ModuleElementDefinition {
    /**
     * Allows the vetoing of the display of a menu item. If any instance return true, the menu item display will be
     * vetoed.
     * 
     * @param id
     *            the id of the menu item
     * @param request
     *            the current request
     * @param response
     *            the current response
     * @return if true the menu item is visible
     */
    public boolean isVisible(String id, HttpServletRequest request, HttpServletResponse response) {
        return true;
    }
}
