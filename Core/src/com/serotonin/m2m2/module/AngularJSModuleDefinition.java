/**
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.serotonin.m2m2.Constants;

/**
 * Define a JavaScript file for loading into the UI web app (typically an AngularJS Module)
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public abstract class AngularJSModuleDefinition extends ModuleElementDefinition {

    /**
     * <p>Override {@link #getJavaScriptPath getJavaScriptPath()} instead. This method will be removed in a future release.</p>
     *
     * <p>Get the path and filename of the AngularJS module file,
     * relative to the module's web directory.</p>
     *
     * @return path relative to web directory of module (contains starting slash)
     */
    public abstract String getJavaScriptFilename();

    /**
     * <p>This method will become abstract in a future release</p>
     *
     * <p>Get the path and filename of the AngularJS module file,
     * can be relative to the module's web directory or an absolute path.</p>
     *
     * @return path relative to web directory of module
     */
    public Path getJavaScriptPath() {
        String fileName = Objects.requireNonNull(this.getJavaScriptFilename());
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        return Paths.get(fileName);
    }

    public Path getAbsoluteJavaScriptPath() {
        return this.getModule().modulePath().resolve(Constants.DIR_WEB).resolve(this.getJavaScriptPath());
    }

    /**
     * @return list of AngularJS module names to load into the UI web app after the bundle is loaded
     */
    public List<String> angularJsModuleNames() {
        return Collections.emptyList();
    }

    /**
     * @return list of AMD module names to require() using RequireJS after the bundle is loaded
     */
    public List<String> amdModuleNames() {
        return Collections.emptyList();
    }
}
