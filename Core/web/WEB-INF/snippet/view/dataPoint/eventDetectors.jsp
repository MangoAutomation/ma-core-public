<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.AlphanumericRegexStateEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.AlphanumericStateEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.AnalogChangeEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.AnalogHighLimitEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.AnalogLowLimitEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.AnalogRangeEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.BinaryStateEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.MultistateStateEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.NegativeCusumEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.NoChangeEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.NoUpdateEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.PointChangeEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.PositiveCusumEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.SmoothnessEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.StateChangeCountEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.module.definitions.event.detectors.AnalogChangeEventDetectorDefinition"%>
<%@page import="com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO"%>

<div>
  <table>
    <tr><td colspan="2">
      <span class="smallTitle"><fmt:message key="pointEdit.detectors.eventDetectors"/></span>
      <tag:help id="eventDetectors"/>
    </td></tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.type"/></td>
      <td class="formField">
        <input id="eventDetectorSelect"></input>
        <tag:img png="add" title="common.add" onclick="pointEventDetectorEditor.addEventDetector();"/>
      </td>
    </tr>
    
    <tr><td colspan="2">
      <div id="emptyListMessage" style="color:#888888;padding:10px;text-align:center;">
        <fmt:message key="pointEdit.detectors.empty"/>
      </div>
    </td></tr>
   
  </table>

  <table id="eventDetectorTable" style="width:95%;"></table>

  <table style="display:none;">
    <tbody id="detectorType<%= AnalogHighLimitEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.highLimitDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.state"/></td>
        <td class="formField">
          <select id="eventDetector_TEMPLATE_State">
            <option value="false"><fmt:message key="pointEdit.detectors.higher"/></option>
            <option value="true"><fmt:message key="pointEdit.detectors.notHigher"/></option>
          </select>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.highLimit"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Limit" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.useResetLimit"/></td>
        <td class="formField">
            <sst:checkbox id="eventDetector_TEMPLATE_UseReset" onclick="changeUseResetLimit(this.checked, getPedId(this));"/>
        </td>
      </tr>
      <tr id="eventDetector_TEMPLATE_ResetRow" style="display:none">
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.resetLimit"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Weight" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= AnalogLowLimitEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.lowLimitDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.state"/></td>
        <td class="formField">
          <select id="eventDetector_TEMPLATE_State">
            <option value="false"><fmt:message key="pointEdit.detectors.lower"/></option>
            <option value="true"><fmt:message key="pointEdit.detectors.notLower"/></option>
          </select>
        </td>
      </tr>
      
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.lowLimit"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Limit" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.useResetLimit"/></td>
        <td class="formField">
            <sst:checkbox id="eventDetector_TEMPLATE_UseReset" onclick="changeUseResetLimit(this.checked, getPedId(this));"/>
        </td>
      </tr>
      <tr id="eventDetector_TEMPLATE_ResetRow" style="display:none">
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.resetLimit"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Weight" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= AnalogChangeEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.analogChangeDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.state"/></td>
        <td class="formField">
          <select id="eventDetector_TEMPLATE_State">
            <option value="3"><fmt:message key="pointEdit.detectors.change"/></option>
            <option value="2"><fmt:message key="pointEdit.detectors.analogChange.increase"/></option>
            <option value="1"><fmt:message key="pointEdit.detectors.analogChange.decrease"/></option>
          </select>
        </td>
      </tr>
      
      <tr>
        <td class="formLabelRequired"><fmt:message key="publisherEdit.updateEvent"/></td>
        <td class="formField">
        	<tag:exportCodesOptions id="eventDetector_TEMPLATE_UpdateEvent" optionList="<%= AnalogChangeDetectorVO.UPDATE_EVENT_TYPE_CODES.getIdKeys() %>"/>
      </tr>
      
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.analogChangeLimit"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Limit" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= BinaryStateEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.stateDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.state"/></td>
        <td class="formField">
          <select id="eventDetector_TEMPLATE_State">
            <option value="false"><fmt:message key="pointEdit.detectors.zero"/></option>
            <option value="true"><fmt:message key="pointEdit.detectors.one"/></option>
          </select>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= MultistateStateEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.stateDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.state"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_State" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointChangeEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.changeDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= StateChangeCountEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.changeCounter"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.changeCount"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_ChangeCount" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= NoChangeEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.noChange"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= NoUpdateEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.noUpdate"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= AlphanumericStateEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.stateDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.state"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_State" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= AlphanumericRegexStateEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.regexStateDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.regexState"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_State" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PositiveCusumEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.posCusumDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.posLimit"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Limit" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.weight"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Weight" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= NegativeCusumEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.negCusumDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.negLimit"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Limit" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.weight"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Weight" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    <tbody id="detectorType<%= AnalogRangeEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.rangeDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.state"/></td>
        <td class="formField">
          <select id="eventDetector_TEMPLATE_State">
            <option value="true"><fmt:message key="pointEdit.detectors.withinRange"/></option>
            <option value="false"><fmt:message key="pointEdit.detectors.outsideRange"/></option>
          </select>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.rangeLow"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Weight" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.rangeHigh"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Limit" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= SmoothnessEventDetectorDefinition.TYPE_NAME %>">
      <tr><td class="horzSeparator" colspan="2"></td></tr>
      <tr>
        <td class="formLabelRequired">
          <tag:img png="delete" title="common.delete" onclick="pointEventDetectorEditor.deleteDetector(getPedId(this))"/>
          <fmt:message key="pointEdit.detectors.type"/>
        </td>
        <td class="formField"><fmt:message key="pointEdit.detectors.smoothnessDet"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.xid"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Xid" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.alias"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Alias" type="text" class="formFullLength"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.alarmLevel"/></td>
        <td class="formField">
          <tag:alarmLevelOptions id="eventDetector_TEMPLATE_AlarmLevel"
                  onchange="pointEventDetectorEditor.updateAlarmLevelImage(this.value, getPedId(this))"/>
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_grey" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.lowLimit"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_Limit" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.smoothnessBoxcar"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_ChangeCount" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.quiescentDuration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_QuiescentDuration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_QuiescentDurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
  </table>
</div>

<script type="text/javascript">
  dojo.require("dijit.form.Select");
  
  function setEventDetectors(vo){
      
      DataPointDwr.getEventDetectorOptions(vo.pointLocator.dataTypeId,function(response){
          var options = [];
          for(var i=0; i<response.data.options.length; i++){
              options.push({
                  label: mangoMsg[response.data.options[i].key],
                  value: response.data.options[i].value,
              })
          }
          pointEventDetectorEditor.eventDetectorSelect.options = [];
          pointEventDetectorEditor.eventDetectorSelect.addOption(options);

          //Remove all rows from the table
          dwr.util.removeAllRows("eventDetectorTable");
          show("emptyListMessage");
          //Fill with our event detectors
          DataPointEditDwr.getEventDetectors(pointEventDetectorEditor.initCB);
      });
      
  }
  
  
  /*
   * For now just use a DWR Call to fuse the saving into the current point
   * This is messy but this page is huge
   */
  function getEventDetectors(vo,callback){
	  //TODO Need to know if data type changed so we can delete all the existing Point Event Detectors for this point
      pointEventDetectorEditor.save(callback);
  }
  
  /**
   * Reset the Chart Renderer Input to the default for that data type
   */
  function resetEventDetectorOptions(dataTypeId){
	  DataPointDwr.getEventDetectorOptions(dataTypeId,function(response){

          var options = [];
          for(var i=0; i<response.data.options.length; i++){
              options.push({
                  label: mangoMsg[response.data.options[i].key],
                  value: response.data.options[i].value,
              })
          }
          pointEventDetectorEditor.eventDetectorSelect.options = [];
          pointEventDetectorEditor.eventDetectorSelect.addOption(options);
      });
  }
  //Register for callbacks when the data type is changed
  dataTypeChangedCallbacks.push(resetEventDetectorOptions);
  
  /**
   * Change Reset Limit view
   */
  function changeUseResetLimit(checked, pedId){
      var pedResetRowId = "eventDetector" + pedId + "ResetRow";
      if(checked)
          show(pedResetRowId);
      else
          hide(pedResetRowId);
  }
  
  
  function getPedId(node) {
      while (!(node.pedId))
          node = node.parentNode;
      return node.pedId;
  }

  function PointEventDetectorEditor() {
      var detectorCount = 0;
      var newDetectorId = -1;
      
      this.eventDetectorSelect = new dijit.form.Select({
          name: 'eventDetectorSelect',
          
      },"eventDetectorSelect");
      
      this.init = function() {
           //Nothing for now
          
      }
      
      this.initCB = function(detectorList) {
          if(detectorList != null)
              for (var i=0; i<detectorList.length; i++)
                  pointEventDetectorEditor.addEventDetectorCB(detectorList[i]);

      }
      
      this.addEventDetector = function() {
          var value = this.eventDetectorSelect.value;
          
          DataPointEditDwr.addEventDetector(value, newDetectorId--, this.addEventDetectorCB);
      }
  
      this.addEventDetectorCB = function(detector) {
          detectorCount++;
          hide("emptyListMessage");
          
          // Create the appropriate tbody.
          var content = $("detectorType"+ detector.detectorType).cloneNode(true);
          updateTemplateNode(content, detector.id);
          content.id = "eventDetector"+ detector.id;
          content.pedId = detector.id;
          content.pedType = detector.detectorType;
          $("eventDetectorTable").appendChild(content);
          
          // Set the values in the content controls.
          if (detector.detectorType == '<%= AnalogHighLimitEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"State", detector.notHigher ? "true" : "false");
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
              $set("eventDetector"+ detector.id +"Weight", detector.resetLimit);
              $set("eventDetector" + detector.id + "UseReset", detector.useResetLimit);
              if(detector.useResetLimit === true)
              	changeUseResetLimit(true, detector.id);
          }
          else if (detector.detectorType == '<%= AnalogLowLimitEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"State", detector.notLower ? "true" : "false");
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
              $set("eventDetector"+ detector.id +"Weight", detector.resetLimit);
              $set("eventDetector" + detector.id + "UseReset", detector.useResetLimit);
              if(detector.useResetLimit === true)
                	changeUseResetLimit(true, detector.id);
          }
          else if (detector.detectorType == '<%= AnalogChangeEventDetectorDefinition.TYPE_NAME %>') {
        	  var state = 0;
        	  if(detector.checkIncrease)
        		  state |= 2;
        	  if(detector.checkDecrease)
        		  state |= 1;
        	  
        	  $set("eventDetector"+ detector.id +"State", String(state));
        	  $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
              $set("eventDetector"+ detector.id +"UpdateEvent", detector.updateEvent);
          }
          else if (detector.detectorType == '<%= BinaryStateEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"State", detector.state ? "true" : "false");
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= MultistateStateEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"State", detector.state);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= PointChangeEventDetectorDefinition.TYPE_NAME %>') {}
          else if (detector.detectorType == '<%= StateChangeCountEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"ChangeCount", detector.changeCount);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= NoChangeEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= NoUpdateEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= AlphanumericStateEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"State", detector.state);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= AlphanumericRegexStateEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"State", detector.state);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= PositiveCusumEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Weight", detector.weight);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= NegativeCusumEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Weight", detector.weight);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == '<%= AnalogRangeEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"Limit", detector.high);
              $set("eventDetector"+ detector.id +"Weight", detector.low);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
              $set("eventDetector"+ detector.id +"State", detector.withinRange ? "true" : "false");
          }
          else if (detector.detectorType == '<%= SmoothnessEventDetectorDefinition.TYPE_NAME %>') {
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"ChangeCount", detector.boxcar);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          
          //All types these options...
          $set("eventDetector"+ detector.id +"QuiescentDuration", detector.quiescentPeriods);
          $set("eventDetector"+ detector.id +"QuiescentDurationType", detector.quiescentPeriodType);
          $set("eventDetector"+ detector.id +"Xid", detector.xid);
          $set("eventDetector"+ detector.id +"Alias", detector.alias);
          $set("eventDetector"+ detector.id +"AlarmLevel", detector.alarmLevel);
          pointEventDetectorEditor.updateAlarmLevelImage(detector.alarmLevel, detector.id);
      }
      
      this.updateAlarmLevelImage = function(alarmLevel, pedId) {
          setAlarmLevelImg(alarmLevel, $("eventDetector"+ pedId +"AlarmLevelImg"));
      }
      
      this.deleteDetector = function(pedId) {
          DataPointEditDwr.deleteEventDetector(pedId);
          
          detectorCount--;
          if (detectorCount == 0)
              show("emptyListMessage");
          
          var content = $("eventDetector"+ pedId);
          $("eventDetectorTable").removeChild(content);
      }
      
      var saveCBCount;
      var saveCallback;
      var runSaveCallback;
      this.save = function(callback) {
          var edTableNodes = $("eventDetectorTable").childNodes;
          saveCBCount = 0;
          saveCallback = callback;
          runSaveCallback = true;
          
          dwr.engine.beginBatch();
          for (var i=0; i<edTableNodes.length; i++) {
              if (!edTableNodes[i].pedId)
                  continue;
              
              // Found a detector row.
              var pedId = edTableNodes[i].pedId;
              var pedType = edTableNodes[i].pedType;
              var errorMessage = null;
              var xid = $get("eventDetector"+ pedId +"Xid");
              var alias = $get("eventDetector"+ pedId +"Alias");
              var alarmLevel = parseInt($get("eventDetector"+ pedId +"AlarmLevel"));
              var quiescentDuration = parseInt($get("eventDetector"+ pedId +"QuiescentDuration"));
              var quiescentDurationType = parseInt($get("eventDetector"+ pedId +"QuiescentDurationType"));
              
              if (pedType == '<%= AnalogHighLimitEventDetectorDefinition.TYPE_NAME %>') {
                  var state = $get("eventDetector"+ pedId +"State") === "true";
                  var limit = parseFloat($get("eventDetector"+ pedId +"Limit"));
                  var weight = parseFloat($get("eventDetector"+ pedId +"Weight"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  var useReset = $get("eventDetector" + pedId + "UseReset");
                  var multistateState;
                  if(useReset)
                      multistateState = 1;
                  else
                      multistateState = 0;
                  
                  if (isNaN(limit))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingLimit"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else if(isNaN(weight))
                      errorMessage = "<fmt:message key='pointEdit.detectors.errorParsingResetLimit'/>";
                  else if((multistateState==1)&&(state)&&(limit > weight)){
                       //Is not higher, so reset limit must be >= limit
                       errorMessage = "<fmt:message key='pointEdit.detectors.resetLimitMustBeGreaterThanLimit'/>"
                  }else if((multistateState==1)&&(!state)&&(limit < weight)){
                      //Is higher, so reset limit must be <= limit
                      errorMessage = "<fmt:message key='pointEdit.detectors.resetLimitMustBeLessThanLimit'/>"
                  }
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateHighLimitDetector(pedId, xid, alias, limit, state, useReset,
                              weight, duration, durationType, quiescentDuration, quiescentDurationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= AnalogLowLimitEventDetectorDefinition.TYPE_NAME %>') {
                  var state = $get("eventDetector"+ pedId +"State") === "true";
                  var limit = parseFloat($get("eventDetector"+ pedId +"Limit"));
                  var weight = parseFloat($get("eventDetector"+ pedId +"Weight"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  var useReset = $get("eventDetector" + pedId + "UseReset");
                  var multistateState;
                  if(useReset)
                      multistateState = 1;
                  else
                      multistateState = 0;
                  
                  if (isNaN(limit))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingLimit"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else if(isNaN(weight))
                      errorMessage = "<fmt:message key='pointEdit.detectors.errorParsingResetLimit'/>";
                  else if((multistateState==1)&&(state)&&(limit < weight)){
                      //Is not lower, so reset limit must be <= limit
                      errorMessage = "<fmt:message key='pointEdit.detectors.resetLimitMustBeLessThanLimit'/>"
                  }else if((multistateState==1)&&(!state)&&(limit > weight)){
                     //Is lower, so reset limit must be >= limit
                     errorMessage = "<fmt:message key='pointEdit.detectors.resetLimitMustBeGreaterThanLimit'/>"
                  }else {
                      saveCBCount++;
                      DataPointEditDwr.updateLowLimitDetector(pedId, xid, alias, limit, state, useReset,
                              weight, duration, durationType, quiescentDuration, quiescentDurationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= AnalogChangeEventDetectorDefinition.TYPE_NAME %>') {
            	  var state = parseInt($get("eventDetector"+ pedId +"State"))
            	  var limit = parseFloat($get("eventDetector"+ pedId +"Limit"));
            	  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  var updateEvent = parseInt($get("eventDetector"+pedId+"UpdateEvent"));
            	  
            	  if (isNaN(limit))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingLimit"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
            	  else if(state == 0 || state & 0b11 != state)
            		  errorMessage = "<fmt:message key="pointEdit.detectors.analogChange.invalidState"/>";
           		  else {
           			  saveCBCount++;
           			  DataPointEditDwr.updateAnalogChangeDetector(pedId, xid, alias, limit, (state & 0x2) != 0, (state & 0x1) != 0, duration, durationType,
           			   	quiescentDuration, quiescentDurationType, alarmLevel, updateEvent, saveCB);
           		  }
              }
              else if (pedType == '<%= BinaryStateEventDetectorDefinition.TYPE_NAME %>') {
                  var state = $get("eventDetector"+ pedId +"State");
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateBinaryStateDetector(pedId, xid, alias, state, duration, durationType, quiescentDuration, quiescentDurationType,
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= MultistateStateEventDetectorDefinition.TYPE_NAME %>') {
                  var state = parseInt($get("eventDetector"+ pedId +"State"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(state))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingState"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateMultistateStateDetector(pedId, xid, alias, state, duration, durationType, quiescentDuration, quiescentDurationType,
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= PointChangeEventDetectorDefinition.TYPE_NAME %>') {
                  saveCBCount++;
                  DataPointEditDwr.updatePointChangeDetector(pedId, xid, alias, quiescentDuration, quiescentDurationType, alarmLevel, saveCB);
              }
              else if (pedType == '<%= StateChangeCountEventDetectorDefinition.TYPE_NAME %>') {
                  var count = parseInt($get("eventDetector"+ pedId +"ChangeCount"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(count))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingChangeCount"/>";
                  else if (count < 2)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidChangeCount"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 1)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationOne"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateStateChangeCountDetector(pedId, xid, alias, count, duration, durationType, quiescentDuration, quiescentDurationType,
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= NoChangeEventDetectorDefinition.TYPE_NAME %>') {
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 1)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationOne"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateNoChangeDetector(pedId, xid, alias, duration, durationType, quiescentDuration, quiescentDurationType, alarmLevel,
                              saveCB);
                  }
              }
              else if (pedType == '<%= NoUpdateEventDetectorDefinition.TYPE_NAME %>') {
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 1)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationOne"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateNoUpdateDetector(pedId, xid, alias, duration, durationType, quiescentDuration, quiescentDurationType, alarmLevel,
                              saveCB);
                  }
              }
              else if (pedType == '<%= AlphanumericStateEventDetectorDefinition.TYPE_NAME %>') {
                  var state = $get("eventDetector"+ pedId +"State");
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (state && state.length > 128)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidState"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateAlphanumericStateDetector(pedId, xid, alias, state, duration, durationType, quiescentDuration, quiescentDurationType,
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= AlphanumericRegexStateEventDetectorDefinition.TYPE_NAME %>') {
                  var state = $get("eventDetector"+ pedId +"State");
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (state && state.length > 128)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidState"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateAlphanumericRegexStateDetector(pedId, xid, alias, state, duration, durationType, quiescentDuration, quiescentDurationType,
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= PositiveCusumEventDetectorDefinition.TYPE_NAME %>') {
                  var limit = parseFloat($get("eventDetector"+ pedId +"Limit"));
                  var weight = parseFloat($get("eventDetector"+ pedId +"Weight"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(limit))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingLimit"/>";
                  else if (isNaN(weight))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingWeight"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updatePositiveCusumDetector(pedId, xid, alias, limit, weight, duration,
                              durationType, quiescentDuration, quiescentDurationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= NegativeCusumEventDetectorDefinition.TYPE_NAME %>') {
                  var limit = parseFloat($get("eventDetector"+ pedId +"Limit"));
                  var weight = parseFloat($get("eventDetector"+ pedId +"Weight"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(limit))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingLimit"/>";
                  else if (isNaN(weight))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingWeight"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateNegativeCusumDetector(pedId, xid, alias, limit, weight, duration,
                              durationType, quiescentDuration, quiescentDurationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= AnalogRangeEventDetectorDefinition.TYPE_NAME %>') {
                  var state = $get("eventDetector"+ pedId +"State");
                  var limit = parseFloat($get("eventDetector"+ pedId +"Limit"));
                  var weight = parseFloat($get("eventDetector"+ pedId +"Weight"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(limit))
                      errorMessage = "<fmt:message key='pointEdit.detectors.errorParsingHighLimit'/>";
                  else if (isNaN(weight))
                      errorMessage = "<fmt:message key='pointEdit.detectors.errorParsingLowLimit'/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key='pointEdit.detectors.errorParsingDuration'/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key='pointEdit.detectors.invalidDurationZero'/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateAnalogRangeDetector(pedId, xid, alias, limit, weight, state, duration,
                              durationType, quiescentDuration, quiescentDurationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == '<%= SmoothnessEventDetectorDefinition.TYPE_NAME %>') {
                  var limit = parseFloat($get("eventDetector"+ pedId +"Limit"));
                  var boxcar = parseInt($get("eventDetector"+ pedId +"ChangeCount"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(limit))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingLimit"/>";
                  else if (isNaN(boxcar))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingBoxcar"/>";
                  else if (boxcar < 3)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidBoxcar"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDurationZero"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateSmoothnessDetector(pedId, xid, alias, limit, boxcar, duration,
                              durationType, quiescentDuration, quiescentDurationType, alarmLevel, saveCB);
                  }
              }

              if (errorMessage != null) {
                  runSaveCallback = false;
                  $("eventDetector"+ pedId +"ErrorMessage").innerHTML = errorMessage;
              }
              else
                  $("eventDetector"+ pedId +"ErrorMessage").innerHTML = "";
          }
          dwr.engine.endBatch();
      
          // If no save calls were made, continue by calling the callback.
          if (runSaveCallback && saveCBCount == 0)
              callback();
          else if(!runSaveCallback) {
        	  stopImageFader("pointSaveImg");
        	  showMessage("pointMessage", "<fmt:message key="pointEdit.detectors.validationFailed"/>");
          }
      };
      
      function saveCB() {
          if (--saveCBCount == 0) {
              // We're done with the callbacks. If there were no errors, call the callback.
              if (runSaveCallback)
                  saveCallback();
          }
      }
  }
  var pointEventDetectorEditor = new PointEventDetectorEditor();
  
</script>