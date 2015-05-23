<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>



<span class="smallTitle"><fmt:message key="filter.current" /></span>
<div style="padding-bottom:10px;">
  <div id="filter-grid" style="height:80px; border: 0px" ></div>
</div>
<span class="smallTitle">
  <fmt:message key="header.dataPoints" />
</span>
<div id="points-permissions-grid" style="height:450px;"></div>
<hr>
<div id="permissions">
  <span class="smallTitle">
  <fmt:message key="permissions.bulkApply" />
  <img id="bulkPermissionsHelp" class="ptr"
   src="/images/help.png"
   title="<fmt:message key="permission.bulkEditDataPoint" />" 
   alt="<fmt:message key="permission.bulkEditDataPoint" />"
  />
  </span>

 <div class="formItem">
  <label class="formLabelRequired" style="width:110px" for="setPermission"><fmt:message key="pointEdit.props.permission.set"/></label>
  <div class="formField"><input id="setPermissions" type="text"/><button id="applySetPermission"><fmt:message key="permissions.apply"/></button></div>
 </div>
 <div class="formItem">
    <label class="formLabelRequired" style="width:110px" for="readPermission"><fmt:message key="pointEdit.props.permission.read"/></label>
    <div class="formField"><input id="readPermissions" type="text"/><button id="applyReadPermission"><fmt:message key="permissions.apply"/></button></div>
 </div>
</div>
