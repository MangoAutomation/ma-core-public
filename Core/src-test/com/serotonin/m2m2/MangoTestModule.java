/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleElementDefinition;
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
        this(name, null, 1);
    }

    public MangoTestModule(String name, String dependencies, int loadOrder) {
        super(name, Common.getVersion(),
                new Date(Common.START_TIME),
                new TranslatableMessage("common.default", name),
                "Radix IoT, LLC", "https://radixiot.com/", dependencies, loadOrder, false);
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

    public void overrideDefinition(ModuleElementDefinition override) {
        // cant access override.setModule(this), this is a hacky workaround
        addDefinition(override);
        definitions.remove(override);

        var it = definitions.listIterator();
        while (it.hasNext()) {
            var existing = it.next();
            if (existing.getClass().isInstance(override)) {
                it.set(override);
                break;
            }
        }
    }
}
