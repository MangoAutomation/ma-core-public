<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.vo.event.PointEventDetectorVO"%>
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
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_ANALOG_HIGH_LIMIT %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_ANALOG_LOW_LIMIT %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_BINARY_STATE %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_MULTISTATE_STATE %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_POINT_CHANGE %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_STATE_CHANGE_COUNT %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.detectors.changeCount"/></td>
        <td class="formField"><input id="eventDetector_TEMPLATE_ChangeCount" type="text" class="formShort"/></td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_NO_CHANGE %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_NO_UPDATE %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.detectors.duration"/></td>
        <td class="formField">
          <input id="eventDetector_TEMPLATE_Duration" type="text" class="formShort"/>
          <tag:timePeriods id="eventDetector_TEMPLATE_DurationType" s="true" min="true" h="true" d="true"/>
        </td>
      </tr>
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_ALPHANUMERIC_STATE %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_ALPHANUMERIC_REGEX_STATE %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_POSITIVE_CUSUM %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_NEGATIVE_CUSUM %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
      <tr><td class="formError" id="eventDetector_TEMPLATE_ErrorMessage" colspan="2"></td></tr>
    </tbody>
    <tbody id="detectorType<%= PointEventDetectorVO.TYPE_ANALOG_RANGE %>">
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
          <tag:img id="eventDetector_TEMPLATE_AlarmLevelImg" png="flag_green" title="common.alarmLevel.none" style="display:none;"/>
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
                  label: mangoMsg[response.data.options[i].nameKey],
                  value: response.data.options[i].id,
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
      pointEventDetectorEditor.save(callback);
  }
  
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
          
          DataPointEditDwr.addEventDetector(value, this.addEventDetectorCB);
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
          if (detector.detectorType == <%= PointEventDetectorVO.TYPE_ANALOG_HIGH_LIMIT %>) {
              $set("eventDetector"+ detector.id +"State", detector.binaryState ? "true" : "false");
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
              $set("eventDetector"+ detector.id +"Weight", detector.weight);
              if(detector.multistateState == 1){
                  $set("eventDetector" + detector.id + "UseReset", true);
                  changeUseResetLimit(true, detector.id);
              }
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_ANALOG_LOW_LIMIT %>) {
              $set("eventDetector"+ detector.id +"State", detector.binaryState ? "true" : "false");
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
              $set("eventDetector"+ detector.id +"Weight", detector.weight);
              if(detector.multistateState == 1){
                  $set("eventDetector" + detector.id + "UseReset", true);
                  changeUseResetLimit(true, detector.id);
              }
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_BINARY_STATE %>) {
              $set("eventDetector"+ detector.id +"State", detector.binaryState ? "true" : "false");
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_MULTISTATE_STATE %>) {
              $set("eventDetector"+ detector.id +"State", detector.multistateState);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_POINT_CHANGE %>) {}
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_STATE_CHANGE_COUNT %>) {
              $set("eventDetector"+ detector.id +"ChangeCount", detector.changeCount);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_NO_CHANGE %>) {
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_NO_UPDATE %>) {
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_ALPHANUMERIC_STATE %>) {
              $set("eventDetector"+ detector.id +"State", detector.alphanumericState);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_ALPHANUMERIC_REGEX_STATE %>) {
              $set("eventDetector"+ detector.id +"State", detector.alphanumericState);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_POSITIVE_CUSUM %>) {
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Weight", detector.weight);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_NEGATIVE_CUSUM %>) {
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Weight", detector.weight);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
          }
          else if (detector.detectorType == <%= PointEventDetectorVO.TYPE_ANALOG_RANGE %>) {
              $set("eventDetector"+ detector.id +"Limit", detector.limit);
              $set("eventDetector"+ detector.id +"Weight", detector.weight);
              $set("eventDetector"+ detector.id +"Duration", detector.duration);
              $set("eventDetector"+ detector.id +"DurationType", detector.durationType);
              $set("eventDetector"+ detector.id +"State", detector.binaryState ? "true" : "false");
          }
          
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
              
              if (pedType == <%= PointEventDetectorVO.TYPE_ANALOG_HIGH_LIMIT %>) {
                  var state = $get("eventDetector"+ pedId +"State");
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
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else if(isNaN(weight))
                      errorMessage = "<fmt:message key='pointEdit.detectors.errorParsingResetLimit'/>";
                  else if((multistateState==1)&&(state)&&(limit < weight)){
                       //Is not higher, so reset limit must be >= limit
                       errorMessage = "<fmt:message key='pointEdit.detectors.resetLimitMustBeGreaterThanLimit'/>"
                  }else if((multistateState==1)&&(!state)&&(limit > weight)){
                      //Is higher, so reset limit must be <= limit
                      errorMessage = "<fmt:message key='pointEdit.detectors.resetLimitMustBeLessThanLimit'/>"
                  }
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateHighLimitDetector(pedId, xid, alias, limit, state, multistateState,
                              weight, duration, durationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_ANALOG_LOW_LIMIT %>) {
                  var state = $get("eventDetector"+ pedId +"State");
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
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else if(isNaN(weight))
                      errorMessage = "<fmt:message key='pointEdit.detectors.errorParsingResetLimit'/>";
                  else if((multistateState==1)&&(state)&&(limit > weight)){
                      //Is not lower, so reset limit must be <= limit
                      errorMessage = "<fmt:message key='pointEdit.detectors.resetLimitMustBeLessThanLimit'/>"
                  }else if((multistateState==1)&&(!state)&&(limit < weight)){
                     //Is lower, so reset limit must be >= limit
                     errorMessage = "<fmt:message key='pointEdit.detectors.resetLimitMustBeGreaterThanLimit'/>"
                  }else {
                      saveCBCount++;
                      DataPointEditDwr.updateLowLimitDetector(pedId, xid, alias, limit, state, multistateState,
                              weight, duration, durationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_BINARY_STATE %>) {
                  var state = $get("eventDetector"+ pedId +"State");
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateBinaryStateDetector(pedId, xid, alias, state, duration, durationType,
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_MULTISTATE_STATE %>) {
                  var state = parseInt($get("eventDetector"+ pedId +"State"));
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(state))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingState"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateMultistateStateDetector(pedId, xid, alias, state, duration, durationType,
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_POINT_CHANGE %>) {
                  saveCBCount++;
                  DataPointEditDwr.updatePointChangeDetector(pedId, xid, alias, alarmLevel, saveCB);
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_STATE_CHANGE_COUNT %>) {
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
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateStateChangeCountDetector(pedId, xid, alias, count, duration, durationType, 
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_NO_CHANGE %>) {
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 1)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateNoChangeDetector(pedId, xid, alias, duration, durationType, alarmLevel,
                              saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_NO_UPDATE %>) {
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 1)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateNoUpdateDetector(pedId, xid, alias, duration, durationType, alarmLevel,
                              saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_ALPHANUMERIC_STATE %>) {
                  var state = $get("eventDetector"+ pedId +"State");
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (state && state.length > 128)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidState"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateAlphanumericStateDetector(pedId, xid, alias, state, duration, durationType, 
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_ALPHANUMERIC_REGEX_STATE %>) {
                  var state = $get("eventDetector"+ pedId +"State");
                  var duration = parseInt($get("eventDetector"+ pedId +"Duration"));
                  var durationType = parseInt($get("eventDetector"+ pedId +"DurationType"));
                  
                  if (state && state.length > 128)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidState"/>";
                  else if (isNaN(duration))
                      errorMessage = "<fmt:message key="pointEdit.detectors.errorParsingDuration"/>";
                  else if (duration < 0)
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateAlphanumericRegexStateDetector(pedId, xid, alias, state, duration, durationType, 
                              alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_POSITIVE_CUSUM %>) {
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
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updatePositiveCusumDetector(pedId, xid, alias, limit, weight, duration,
                              durationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_NEGATIVE_CUSUM %>) {
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
                      errorMessage = "<fmt:message key="pointEdit.detectors.invalidDuration"/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateNegativeCusumDetector(pedId, xid, alias, limit, weight, duration,
                              durationType, alarmLevel, saveCB);
                  }
              }
              else if (pedType == <%= PointEventDetectorVO.TYPE_ANALOG_RANGE %>) {
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
                      errorMessage = "<fmt:message key='pointEdit.detectors.invalidDuration'/>";
                  else {
                      saveCBCount++;
                      DataPointEditDwr.updateAnalogRangeDetector(pedId, xid, alias, limit, weight, state, duration,
                              durationType, alarmLevel, saveCB);
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