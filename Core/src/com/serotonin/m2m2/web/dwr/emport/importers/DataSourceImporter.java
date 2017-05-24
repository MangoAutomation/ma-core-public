package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class DataSourceImporter extends Importer {
    public DataSourceImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");

        if (StringUtils.isBlank(xid))
            xid = ctx.getDataSourceDao().generateUniqueXid();

        DataSourceVO<?> vo = ctx.getDataSourceDao().getDataSource(xid);
        if (vo == null) {
            String typeStr = json.getString("type");
            if (StringUtils.isBlank(typeStr))
                addFailureMessage("emport.dataSource.missingType", xid, ModuleRegistry.getDataSourceDefinitionTypes());
            else {
                DataSourceDefinition def = ModuleRegistry.getDataSourceDefinition(typeStr);
                if (def == null)
                    addFailureMessage("emport.dataSource.invalidType", xid, typeStr,
                            ModuleRegistry.getDataSourceDefinitionTypes());
                else {
                    vo = def.baseCreateDataSourceVO();
                    vo.setXid(xid);
                }
            }
        }

        if (vo != null) {
            try {
                // The VO was found or successfully created. Finish reading it in.
                ctx.getReader().readInto(vo, json);

                // Now validate it. Use a new response object so we can distinguish errors in this vo from
                // other errors.
                ProcessResult voResponse = new ProcessResult();
                vo.validate(voResponse);
                if (voResponse.getHasMessages())
                    setValidationMessages(voResponse, "emport.dataSource.prefix", xid);
                else {
                    // Sweet. Save it.
                    boolean isnew = vo.isNew();
                    if(Common.runtimeManager.getState() == RuntimeManager.RUNNING){
                    	Common.runtimeManager.saveDataSource(vo);
                    	addSuccessMessage(isnew, "emport.dataSource.prefix", xid);
                    }else{
                    	addFailureMessage(new ProcessMessage("Runtime manager not running, data source with xid: " + xid + "not saved."));
                    }
                }
            }
            catch (TranslatableJsonException e) {
                addFailureMessage("emport.dataSource.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                addFailureMessage("emport.dataSource.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
}
