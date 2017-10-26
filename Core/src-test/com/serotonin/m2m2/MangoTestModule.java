/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.List;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;

/**
 * 
 * Add in any Test specific module definitions from here
 *
 * @author Terry Packer
 */
public class MangoTestModule extends Module{

    /**
     * @param name
     * @param version
     * @param description
     * @param vendor
     * @param vendorUrl
     * @param dependencies
     * @param loadOrder
     * @param signed
     */
    public MangoTestModule(List<ModuleElementDefinition> definitions) {
        super("MangoTestModule", Version.forIntegers(1, 0, 0), 
                new TranslatableMessage("common.default", "Testing definitions"), 
                "IAS", "https://www.infiniteautomation.com", null, 1, false);
    
        addDefinition(new MockDataSourceDefinition());
        
        for(ModuleElementDefinition def : definitions)
            addDefinition(def);
        
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.Module#getDirectoryPath()
     */
    @Override
    public String getDirectoryPath() {
        return "";
    }
   

}
