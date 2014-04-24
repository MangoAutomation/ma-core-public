<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
--%>
<%@include file="/WEB-INF/tags/decl.tagf" %>
<%@attribute name="dwr" rtexprvalue="true" required="true" %>

<script type="text/javascript">

  function dsPurgeNowAllChanged() {
      var all = $get("dsPurgeNowAll");
      setDisabled("dsPurgeNowPeriod", all);
      setDisabled("dsPurgeNowType", all);
  }
  
  function dsPurgeNow() {
      var all = $get("dsPurgeNowAll");
      if (all && !confirm("<fmt:message key='dsEdit.purge.confirm'/>"))
          return;
  
      setDisabled("dsPurgeNowBtn", true);
      show("dsPurgeNowWarn");
      startImageFader("dsPurgeNowImg");
      ${dwr}.purgeNow($get("dsPurgeNowType"), $get("dsPurgeNowPeriod"), all, dsPurgeNowCB);
  }
  
  function dsPurgeNowCB(result) {
      setDisabled("dsPurgeNowBtn", false);
      stopImageFader("dsPurgeNowImg");
      hide("dsPurgeNowWarn");
      alert(""+ result +" <fmt:message key='dsEdit.purge.result'/>");
  }
</script>

<div>
  <table>
    <tr><td colspan="3">
      <span class="smallTitle"><fmt:message key="dsEdit.purge.purgeNow"/></span>
      <tag:help id="pointValueLogPurging"/>
    </td></tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="dsEdit.purge.olderThan"/></td>
      <td class="formField">
        <input id="dsPurgeNowPeriod" type="text" value="1" class="formShort"/>
        <tag:timePeriods id="dsPurgeNowType" value="7" min="true" h="true" d="true" w="true" mon="true" y="true"/>
      </td>
    </tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="dsEdit.purge.all"/></td>
      <td class="formField">
        <input data-dojo-type="dijit/form/CheckBox" id="dsPurgeNowAll" onclick="dsPurgeNowAllChanged()">
        <label for="dsPurgeNowAll"><fmt:message key="dsEdit.purge.allData"/></label>
      </td>
    </tr>
    
    <tr>
      <td colspan="2" align="center">
        <input id="dsPurgeNowBtn" type="button" value="<fmt:message key="dsEdit.purge.purgeNow"/>" onclick="dsPurgeNow();"/>
      </td>
    </tr>
    
    <tbody id="dsPurgeNowWarn" style="display:none">
      <tr>
        <td colspan="2" align="center" class="formError">
          <img id="dsPurgeNowImg" src="images/warn.png"/>
          <fmt:message key="dsEdit.purge.warn"/>
        </td>
      </tr>
    </tbody>
  </table>
</div>