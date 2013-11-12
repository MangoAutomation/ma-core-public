<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.vo.DataPointVO"%>
<%@page import="com.serotonin.m2m2.DataTypes"%>

<script type="text/javascript">


	/**
	 * Set the input values on the page using this vo
	 */
	 function setLoggingProperties(vo){
		
		dojo.byId("loggingType").value = vo.loggingType;
		dojo.byId("intervalLoggingPeriod").value = vo.intervalLoggingPeriod;
        dojo.byId("intervalLoggingPeriodType").value = vo.intervalLoggingPeriodType;
        dojo.byId("intervalLoggingType").value = vo.intervalLoggingType;
        dojo.byId("tolerance").value = vo.tolerance;
        dojo.byId("discardExtremeValues").value = vo.discardExtremeValues;
        dojo.byId("discardHighLimit").value = vo.discardHighLimit;
        dojo.byId("discardLowLimit").value = vo.discardLowLimit;
        dojo.byId("purgeOverride").value = vo.purgeOverride;
        dojo.byId("purgeType").value = vo.purgeType;
        dojo.byId("purgePeriod").value = vo.purgePeriod;
        dojo.byId("defaultCacheSize").value = vo.defaultCacheSize;
		
		 if (vo.pointLocator.dataTypeId == <%= DataTypes.NUMERIC %>){
			 show("toleranceSection");
		     show("discardSection");
		 }else {
	        $("intervalLoggingType").disabled = true;
	        $set("intervalLoggingType", <%= DataPointVO.IntervalLoggingTypes.INSTANT %>);
		 }
		    changeLoggingType();
		    changeDiscard();
	 }

	
	/*
	 * Get the values and put into the vo
	 */
	function getLoggingProperties(vo){
		
		vo.loggingType = dojo.byId("loggingType").value; 
        vo.intervalLoggingPeriod = dojo.byId("intervalLoggingPeriod").value;         
        vo.intervalLoggingPeriodType = dojo.byId("intervalLoggingPeriodType").value;
        vo.intervalLoggingType = dojo.byId("intervalLoggingType").value;
        vo.tolerance = dojo.byId("tolerance").value;
        vo.discardExtremeValues = dojo.byId("discardExtremeValues").value;
        vo.discardHighLimit = dojo.byId("discardHighLimit").value;
        vo.discardLowLimit = dojo.byId("discardLowLimit").value;
        vo.purgeOverride = dojo.byId("purgeOverride").value;
        vo.purgeType = dojo.byId("purgeType").value;
        vo.purgePeriod = dojo.byId("purgePeriod").value;
        vo.defaultCacheSize = dojo.byId("defaultCacheSize").value;
        
        //Store the logging properties for later save by module
        DataPointDwr.storeEditLoggingProperties(
        		vo.loggingType,
                vo.intervalLoggingPeriod,        
                vo.intervalLoggingPeriodType,
                vo.intervalLoggingType,
                vo.tolerance,
                vo.discardExtremeValues,
                vo.discardHighLimit,
                vo.discardLowLimit,
                vo.purgeOverride,
                vo.purgeType,
                vo.purgePeriod,
                vo.defaultCacheSize);
        
        
	}

  function changeLoggingType() {
      var loggingType = $get("loggingType");
      var tolerance = $("tolerance");
      var purgeOverride = $("purgeOverride");
      var purgePeriod = $("purgePeriod");
      var purgeType = $("purgeType");
      
      if ($("toleranceSection") && loggingType == <%= DataPointVO.LoggingTypes.ON_CHANGE %>)
          // On change logging for a numeric requires a tolerance setting.
          tolerance.disabled = false;
      else
          tolerance.disabled = true;
      
      if (loggingType == <%= DataPointVO.LoggingTypes.NONE %>) {
          purgeOverride.disabled = true;
          purgePeriod.disabled = true;
          purgeType.disabled = true;
      }
      else {
          purgeOverride.disabled = false;
          changePurgeOverride();
      }
      
      if (loggingType == <%= DataPointVO.LoggingTypes.INTERVAL %>)
          show("intervalLoggingSection");
      else
          hide("intervalLoggingSection");
  }
  
  function changePurgeOverride() {
      var purgePeriod = $("purgePeriod");
      var purgeType = $("purgeType");
      if ($get("purgeOverride")) {
          purgePeriod.disabled = false;
          purgeType.disabled = false;
      }
      else {
          purgePeriod.disabled = true;
          purgeType.disabled = true;
      }
  }
  
  function changeDiscard() {
      var discard = $get("discardExtremeValues");
      if (discard) {
          $("discardLowLimit").disabled = false;
          $("discardHighLimit").disabled = false;
      }
      else {
          $("discardLowLimit").disabled = true;
          $("discardHighLimit").disabled = true;
      }
  }
  
  function clearPointCache() {
      setDisabled("clearCacheBtn", true);
      DataPointEditDwr.clearPointCache(function() {
          setDisabled("clearCacheBtn", false);
      });
  }
  

</script>

<div >
  <table>
    <tr><td colspan="3">
      <span class="smallTitle"><fmt:message key="pointEdit.logging.props"/></span>
      <tag:help id="pointValueLogging"/>
    </td></tr>
    
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.logging.type"/></td>
        <td class="formField">
          <sst:select id="loggingType" name="loggingType" onchange="changeLoggingType();" value="${status.value}">
            <sst:option value="<%= Integer.toString(DataPointVO.LoggingTypes.ON_CHANGE) %>"><fmt:message key="pointEdit.logging.type.change"/></sst:option>
            <sst:option value="<%= Integer.toString(DataPointVO.LoggingTypes.ALL) %>"><fmt:message key="pointEdit.logging.type.all"/></sst:option>
            <sst:option value="<%= Integer.toString(DataPointVO.LoggingTypes.NONE) %>"><fmt:message key="pointEdit.logging.type.never"/></sst:option>
            <sst:option value="<%= Integer.toString(DataPointVO.LoggingTypes.INTERVAL) %>"><fmt:message key="pointEdit.logging.type.interval"/></sst:option>
            <sst:option value="<%= Integer.toString(DataPointVO.LoggingTypes.ON_TS_CHANGE) %>"><fmt:message key="pointEdit.logging.type.tsChange"/></sst:option>
          </sst:select>
        </td>
      </tr>
    
    <tbody id="intervalLoggingSection" style="display:none;">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.logging.period"/></td>
        <td class="formField">
          <fmt:message key="pointEdit.logging.every"/> <input id="intervalLoggingPeriod" type="text" name="intervalLoggingPeriod" class="formShort"/>
          <tag:timePeriods id="intervalLoggingPeriodType" name="intervalLoggingPeriodType"  s="true" min="true" h="true" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
      
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.logging.valueType"/></td>
          <td class="formField">
            <sst:select id="intervalLoggingType" name="intervalLoggingType" >
              <sst:option value="<%= Integer.toString(DataPointVO.IntervalLoggingTypes.INSTANT) %>"><fmt:message key="pointEdit.logging.valueType.instant"/></sst:option>
              <sst:option value="<%= Integer.toString(DataPointVO.IntervalLoggingTypes.MAXIMUM) %>"><fmt:message key="pointEdit.logging.valueType.maximum"/></sst:option>
              <sst:option value="<%= Integer.toString(DataPointVO.IntervalLoggingTypes.MINIMUM) %>"><fmt:message key="pointEdit.logging.valueType.minimum"/></sst:option>
              <sst:option value="<%= Integer.toString(DataPointVO.IntervalLoggingTypes.AVERAGE) %>"><fmt:message key="pointEdit.logging.valueType.average"/></sst:option>
            </sst:select>
          </td>
        </tr>
    </tbody>
    
    <tbody id="toleranceSection" style="display:none;">
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.logging.tolerance"/></td>
          <td class="formField">
            <input id="tolerance" type="text" name="tolerance" class="formShort"/>
          </td>
        </tr>
    </tbody>
      
    <tbody id="discardSection" style="display:none;">
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.logging.discard"/></td>
          <td class="formField">
            <sst:checkbox id="discardExtremeValues" name="discardExtremeValues" onclick="changeDiscard()"/>
          </td>
        </tr>
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.logging.discardLow"/></td>
          <td class="formField">
            <input id="discardLowLimit" type="text" name="discardLowLimit" class="formShort"/>
          </td>
        </tr>
        <tr>
          <td class="formLabelRequired"><fmt:message key="pointEdit.logging.discardHigh"/></td>
          <td class="formField">
            <input id="discardHighLimit" type="text" name="discardHighLimit" class="formShort"/>
          </td>
        </tr>
    </tbody>
      
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.logging.purge"/></td>
      <td class="formField">
        <div>
          <input data-dojo-type="dijit/form/CheckBox" id="purgeOverride" name="purgeOverride"
                  onclick="changePurgeOverride()"/>
          <label for="purgeOverride"><fmt:message key="pointEdit.logging.purgeOverride"/></label>
        </div>
        <div>
          <fmt:message key="pointEdit.logging.after"/> <input id="purgePeriod" type="text" name="purgePeriod" class="formShort"/>
          <tag:timePeriods id="purgeType" name="purgeType" d="true" w="true" mon="true" y="true"/>
        </div>
      </td>
    </tr>
      
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.logging.defaultCache"/></td>
        <td class="formField">
          <input id="defaultCacheSize" type="text" name="defaultCacheSize" class="formShort"/>
          <input id="clearCacheBtn" type="button" value="<fmt:message key="pointEdit.logging.clearCache"/>" onclick="clearPointCache();"/>
        </td>
      </tr>
  </table>
</div>