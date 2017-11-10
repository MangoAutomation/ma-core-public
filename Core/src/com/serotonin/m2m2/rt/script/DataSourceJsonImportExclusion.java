package com.serotonin.m2m2.rt.script;

import com.infiniteautomation.mango.util.ConfigurationExportData;

public class DataSourceJsonImportExclusion extends JsonImportExclusion {

	public DataSourceJsonImportExclusion(String key, String value) {
		super(key, value);
	}

	@Override
	public String getImporterType() {
		return ConfigurationExportData.DATA_SOURCES;
	}

}
