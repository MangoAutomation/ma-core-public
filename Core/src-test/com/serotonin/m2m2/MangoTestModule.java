/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.rt.event.type.MockEventTypeDefinition;

/**
 * 
 * Default testing module, used to add testing module definitions
 * and allow proper access to file paths during testing.
 *
 * @author Terry Packer
 */
public class MangoTestModule extends Module {
    public MangoTestModule(String name) {
        super(name, Version.forIntegers(1, 0, 0), 
                new TranslatableMessage("common.default", name), 
                "IAS", "https://www.infiniteautomation.com", null, 1, false);
        this.addDefinition(new MockEventTypeDefinition());
    }

    @Override
    public Path modulePath() {
        return Paths.get(".").toAbsolutePath().normalize();
    }

    @Override
    public Path resourcesPath() {
        return modulePath().resolve("file-resources");
    }
}
