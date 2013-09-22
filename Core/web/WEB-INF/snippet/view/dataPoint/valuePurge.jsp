<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
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
      show("purgeNowWarn");
      startImageFader("purgeNowImg");
      DataPointEditDwr.purgeNow($get("purgeNowType"), $get("purgeNowPeriod"), all, purgeNowCB);
  }
  
  function purgeNowCB(result) {
      setDisabled("purgeNowBtn", false);
      stopImageFader("purgeNowImg");
      hide("purgeNowWarn");
      alert(""+ result +" <fmt:message key="pointEdit.purge.result"/>");
  }
</script>

<div class="borderDiv marB marR">
  <table>
    <tr><td colspan="3">
      <span class="smallTitle"><fmt:message key="pointEdit.purge.purgeNow"/></span>
      <tag:help id="pointValueLogPurging"/>
    </td></tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.purge.olderThan"/></td>
      <td class="formField">
        <input id="purgeNowPeriod" type="text" value="${form.purgePeriod}" class="formShort"/>
        <tag:timePeriods id="purgeNowType" value="${form.purgeType}" min="true" h="true" d="true" w="true" mon="true" y="true"/>
      </td>
    </tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.purge.all"/></td>
      <td class="formField">
        <input type="checkbox" id="purgeNowAll" onclick="purgeNowAllChanged()">
        <label for="purgeNowAll"><fmt:message key="pointEdit.purge.allData"/></label>
      </td>
    </tr>
    
    <tr>
      <td colspan="2" align="center">
        <input id="purgeNowBtn" type="button" value="<fmt:message key="pointEdit.purge.purgeNow"/>" onclick="purgeNow();"/>
      </td>
    </tr>
    
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