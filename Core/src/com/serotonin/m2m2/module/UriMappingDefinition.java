package com.serotonin.m2m2.module;

import com.serotonin.m2m2.web.mvc.UrlHandler;

abstract public class UriMappingDefinition extends ModuleElementDefinition {
    public enum Permission {
        ANONYMOUS, USER, DATA_SOURCE, ADMINISTRATOR;
    }

    /**
     * The user authority required to access the handler.
     * 
     * @return the required authority level.
     */
    abstract public Permission getPermission();

    /**
     * The URI path to which this controller responds. Required.
     * 
     * @return the controller's URI path.
     */
    abstract public String getPath();

    /**
     * An instance of the handler for the URL. Called once upon startup, so the instance must be reusable and thread
     * safe. If null, a default handler will be created which forwards to the the JSP path.
     * 
     * TODO should reference a UriHandler instead
     * 
     * @return an instance of the URL handler
     */
    abstract public UrlHandler getHandler();

    /**
     * The path to the JSP file that renders the page at this URI. The path is relative to the module directory.
     * Required if the UrlHandler is null.
     * 
     * @return the path to the JSP file.
     */
    abstract public String getJspPath();
}
