/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.AlphanumericRegexStateDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public class AlphanumericRegexStateDetectorVO extends TimeoutDetectorVO<AlphanumericRegexStateDetectorVO> {

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private String state;
	
	public AlphanumericRegexStateDetectorVO(DataPointVO vo) {
		super(vo, new int[] { DataTypes.ALPHANUMERIC });
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public AbstractEventDetectorRT<AlphanumericRegexStateDetectorVO> createRuntime() {
		return new AlphanumericRegexStateDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage durationDesc = getDurationDescription();
        if (durationDesc == null)
            return new TranslatableMessage("event.detectorVo.state", dataPoint.getTextRenderer().getText(
                    state, TextRenderer.HINT_SPECIFIC));
        return new TranslatableMessage("event.detectorVo.statePeriod", dataPoint.getTextRenderer().getText(
                    state, TextRenderer.HINT_SPECIFIC), durationDesc);	
    }
	
	@Override
	public void validate(ProcessResult response, PermissionService service, PermissionHolder user) {
		super.validate(response, service, user);
		
		if(StringUtils.isEmpty(state))
			response.addContextualMessage("state", "validate.cannotContainEmptyString");
		try {
		    Pattern.compile(state);
		} catch(PatternSyntaxException e) {
		    response.addContextualMessage("state", "validate.invalidRegex", e.getMessage());
		}
	}
}
