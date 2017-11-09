package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public class ScriptPermissionsException extends RuntimeException {
	private static final long serialVersionUID = -1L;
	private TranslatableMessage tm;
	public ScriptPermissionsException(TranslatableMessage tm) {
		this.tm = tm;
	}
	public TranslatableMessage getTranslatableMessage() {
		return tm;
	}
	
	@Override
	public String getMessage() {
	    if(tm != null)
	        return tm.translate(Common.getTranslations());
	    return null;
	}
}
