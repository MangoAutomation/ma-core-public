package com.serotonin.m2m2.rt.script;

public abstract class JsonImportExclusion {
	private final String key;
	private final String value;
	
	public JsonImportExclusion(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public abstract String getImporterType();
	
	public String getKey() {
		return key;
	}
	public String getValue() {
		return value;
	}
}
