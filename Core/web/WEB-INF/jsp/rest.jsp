<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
--%>
<%@page import="com.serotonin.m2m2.Common"%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<style>
.chart svg {
  height: 350px;
  min-width: 100px;
  min-height: 100px;
/*
  margin: 50px;
  Minimum height and width is a good idea to prevent negative SVG dimensions...
  For example width should be =< margin.left + margin.right + 1,
  of course 1 pixel for the entire chart would not be very useful, BUT should not have errors
*/
}
</style>
  <link href="/resources/charting/nv.d3.css" rel="stylesheet" type="text/css">

<tag:page>
  <tag:versionedJavascript  src="/resources/rest.js" />
  <tag:versionedJavascript  src="/resources/charting/d3.v3.js" />
  <tag:versionedJavascript  src="/resources/charting/nv.d3.js" />
  
  <div id="editView" class="borderDivPadded mangoEditDiv" >
	  
	  <div class="formItem">
	    <label class="formLabelRequired" for="dataPoints">Select Point:</label>  
	    <div class="formField"><div id="dataPoints"></div></div>
	  </div>
	  
	  <div class="formItem">
	    <label class="formLabelRequired" for="name">Name:</label>
	    <div class="formField"><input id="name" type="text" /></div>
	  </div>
      <div class="formItem">
        <label class="formLabelRequired" for="xid">Xid:</label>
        <div class="formField"><input id="xid" type="text" disabled="true"/></div>
      </div>
      <div class="formItem">
        <label class="formLabelRequired" for="deviceName">Device Name:</label>
        <div class="formField"><input id=deviceName type="text" /></div>
      </div>
      <div class="formItem">
        <label class="formLabelRequired" for="enabled">Enabled:</label>
        <div class="formField"><input id=enabled type="checkbox" /></div>
      </div>
      <div class="formItem">
        <div class="formField"><button type="button" onclick="mangoRest.dataPoints.put();" >Update</button></div>
      </div>

      <div class="formItem">
        <label class="formLabelRequired" for="value">Current Value:</label>
        <div class="formField"><input id=value type="text" /><button type="button" onclick="mangoRest.pointValues.put();">Update Value</button></div>
      </div>      
	  
  </div>
  <div id="chart" class='chart with-transitions borderDiv marB marR' style="display:none">
     <svg></svg>
  </div> 
  
  <div data-dojo-type="dijit/Dialog" data-dojo-id="errorDialog" title="Error" style="display: none">
  <form data-dojo-type="dijit/form/Form">
    <div class="dijitDialogPaneContentArea">
      <div id="errorMessage"></div>
    </div>
    <div class="dijitDialogPaneActionBar">
      <button data-dojo-type="dijit/form/Button" type="button" data-dojo-props="onClick:function() {errorDialog.hide();}"><fmt:message key="common.close"/></button>
    </div>
  </form>
</div>
  
 
</tag:page>