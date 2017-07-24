package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.web.dwr.EmportDwr;

public class DataSourceJsonImportExclusion extends JsonImportExclusion {

	public DataSourceJsonImportExclusion(String key, String value) {
		super(key, value);
	}

	@Override
	public String getImporterType() {
		return EmportDwr.DATA_SOURCES;
	}

}
