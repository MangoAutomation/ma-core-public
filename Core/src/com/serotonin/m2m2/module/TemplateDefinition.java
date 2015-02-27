/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.template.BaseTemplateVO;

/**
 * @author Terry Packer
 *
 */
abstract public class TemplateDefinition extends ModuleElementDefinition{

    /**
     * Used by MA core code to create a new data source instance as required. Should not be used by client code.
     */
    public final BaseTemplateVO<?> baseCreateTemplateVO() {
        BaseTemplateVO<?> vo = createTemplateVO();
        vo.setDefinition(this);
        return vo;
    }
	
    /**
     * An internal identifier for this type of template. Must be unique within an MA instance, and is recommended
     * to be unique inasmuch as possible across all modules.
     * 
     * @return the data source type name.
     */
    abstract public String getTemplateTypeName();
    
    /**
     * Create and return an instance of the template.
     * 
     * @return a new instance of the template.
     */
    abstract protected BaseTemplateVO<?> createTemplateVO();
    
    /**
     * If the module is uninstalled, delete any templates of this type. If this method is overridden, be sure to call
     * super.uninstall so that this code still runs.
     */
    @Override
    public void uninstall() {
        TemplateDao.instance.deletTemplateType(getTemplateTypeName());
    }
}
