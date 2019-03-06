package com.serotonin.m2m2.rt.script;

import javax.script.ScriptException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * 
 * @author Terry Packer
 *
 */
public class ScriptPermissionsException extends ScriptException {
	
    private static final long serialVersionUID = -1L;
	private TranslatableMessage tm;
	
	public ScriptPermissionsException(TranslatableMessage tm) {
	    super("Permissions Exception");
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
