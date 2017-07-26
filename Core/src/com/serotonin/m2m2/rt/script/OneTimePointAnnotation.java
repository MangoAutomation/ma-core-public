package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;

public class OneTimePointAnnotation implements SetPointSource {

	final String annotation;
	final SetPointSource source;
	
	public OneTimePointAnnotation(SetPointSource source, String annotation) {
		this.source = source;
		this.annotation = annotation;
	}
	@Override
	public String getSetPointSourceType() {
		return source.getSetPointSourceType();
	}
	@Override
	public int getSetPointSourceId() {
		return source.getSetPointSourceId();
	}
	@Override
	public TranslatableMessage getSetPointSourceMessage() {
		return new TranslatableMessage("literal", annotation);
	}
	@Override
	public void raiseRecursionFailureEvent() {
		source.raiseRecursionFailureEvent();
	}
}
