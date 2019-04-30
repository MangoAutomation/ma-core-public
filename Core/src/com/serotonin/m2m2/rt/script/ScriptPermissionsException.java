package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * 
 * @author Terry Packer
 *
 */
public class ScriptPermissionsException extends ScriptError {
	
    private static final long serialVersionUID = -1L;
	
	public ScriptPermissionsException(TranslatableMessage tm) {
	    super(tm);
	}
}
