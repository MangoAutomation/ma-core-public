<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<script type="text/javascript">

  function purgeNowAllChanged() {
      var all = $get("purgeNowAll");
      setDisabled("purgeNowPeriod", all);
      setDisabled("purgeNowType", all);
  }
  
  function purgeNow() {
      var all = $get("purgeNowAll");
      if (all && !confirm("<fmt:message key="pointEdit.purge.confirm"/>"))
          return;
  
      setDisabled("purgeNowBtn", true);
      setDisabled("specificTimeRange", true);
      show("purgeNowWarn");
      startImageFader("purgeNowImg");
      DataPointEditDwr.purgeNow($get("purgeNowType"), $get("purgeNowPeriod"), all, purgeNowCB);
  }
  
  function purgeNowSpecific() {
	  var specificFrom = new Date($get("purgeSpecificFrom")).getTime();
	  var specificTo = new Date($get("purgeSpecificTo")).getTime();
	  if(isNaN(specificFrom) || isNaN(specificTo) || specificTo < specificFrom) {
		  alert("<fmt:message key="pointEdit.purge.invalidTimeRange"/>");
		  return;
	  }
	  if(!confirm("<fmt:message key="pointEdit.purge.confirmSpecific"/>" + specificFrom + " <= TS < " + specificTo))
		  return;
	  
	  setDisabled("purgeSpecificNowBtn", true)
	  setDisabled("specificTimeRange", true)
	  show("purgeNowWarn");
	  startImageFader("purgeNowImg");
	  DataPointEditDwr.purgeBetween(specificFrom, specificTo, purgeNowCB)
  }
  
  function purgeNowCB(result) {
      setDisabled("purgeNowBtn", false);
      setDisabled("purgeSpecificNowBtn", false);
      setDisabled("specificTimeRange", false);
      stopImageFader("purgeNowImg");
      hide("purgeNowWarn");
      alert(""+ result +" <fmt:message key="pointEdit.purge.result"/>");
  }
  
  function specificRangeToggle() {
	  if($get("specificTimeRange")) {
		  hide("purgeRelativeSettings");
		  show("purgeSpecificSettings");
	  } else {
		  show("purgeRelativeSettings");
		  hide("purgeSpecificSettings");
	  }
  }
</script>

<div>
  <table>
    <tr><td colspan="3">
      <span class="smallTitle"><fmt:message key="pointEdit.purge.purgeNow"/></span>
      <tag:help id="pointValueLogPurging"/>
    </td></tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.purge.specificTimeRange"/></td>
      <td class="formField"><input type="checkbox" id="specificTimeRange" onchange="specificRangeToggle();"/></td>
    </tr>
    
    <tbody id="purgeRelativeSettings">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.purge.olderThan"/></td>
        <td class="formField">
          <input id="purgeNowPeriod" type="text" value="1" class="formShort"/>
          <tag:timePeriods id="purgeNowType" value="7" min="true" h="true" d="true" w="true" mon="true" y="true"/>
        </td>
      </tr>
    
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.purge.all"/></td>
        <td class="formField">
          <input data-dojo-type="dijit/form/CheckBox" id="purgeNowAll" onclick="purgeNowAllChanged();">
          <label for="purgeNowAll"><fmt:message key="pointEdit.purge.allData"/></label>
        </td>
      </tr>
    
      <tr>
        <td colspan="2" align="center">
          <input id="purgeNowBtn" type="button" value="<fmt:message key="pointEdit.purge.purgeNow"/>" onclick="purgeNow();"/>
        </td>
      </tr>
    </tbody>
    
    <tbody id="purgeSpecificSettings" style="display:none">
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.dateRangeFrom"/></td>
        <td class="formField"><input type="date" id="purgeSpecificFrom"/></td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="common.dateRangeTo"/></td>
        <td class="formField"><input type="date" id="purgeSpecificTo"/></td>
      </tr>
      <tr>
        <td colspan="2" align="center">
          <input id="purgeSpecificNowBtn" type="button" value="<fmt:message key="pointEdit.purge.purgeNow"/>" onclick="purgeNowSpecific();"/>
        </td>
      </tr>
    </tbody>
    
    <tbody id="purgeNowWarn" style="display:none">
      <tr>
        <td colspan="2" align="center" class="formError">
          <img id="purgeNowImg" src="images/warn.png"/>
          <fmt:message key="pointEdit.purge.warn"/>
        </td>
      </tr>
    </tbody>
  </table>
</div>