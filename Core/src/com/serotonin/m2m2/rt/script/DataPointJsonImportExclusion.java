package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.web.dwr.EmportDwr;

public class DataPointJsonImportExclusion extends JsonImportExclusion {

	public DataPointJsonImportExclusion(String key, String value) {
		super(key, value);
	}

	@Override
	public String getImporterType() {
		return EmportDwr.DATA_POINTS;
	}

}
