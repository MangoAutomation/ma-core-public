<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<input type="hidden" id="setPointValue${idSuffix}" value=""/>
<input type="radio"${text == point.textRenderer.zeroLabel ? " checked=\"checked\"" : ""} 
        name="setPointValueRB${idSuffix}" id="setPointValueRBF${idSuffix}" 
        onclick="$('setPointValue${idSuffix}').value = 'false'"/>
<label for="setPointValueRBF${idSuffix}">${point.textRenderer.zeroLabel}</label>
<input type="radio"${text == point.textRenderer.oneLabel ? " checked=\"checked\"" : ""} 
        name="setPointValueRB${idSuffix}" id="setPointValueRBT${idSuffix}" 
        onclick="$('setPointValue${idSuffix}').value = 'true'"/>
<label for="setPointValueRBT${idSuffix}">${point.textRenderer.oneLabel}</label>