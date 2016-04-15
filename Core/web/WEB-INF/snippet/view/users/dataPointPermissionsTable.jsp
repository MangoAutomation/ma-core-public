<%--
    Copyright (C) 2013 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>



<span class="smallTitle"><fmt:message key="filter.current" />
   <img class="ptr bulkPermissionsHelp"
   src="/images/help.png"
   title="<fmt:message key="permissions.bulkdEditDataPoint" />" 
   alt="<fmt:message key="permissions.bulkdEditDataPoint" />"
  />
</span>
<div style="padding-bottom:10px;">
  <div id="filter-grid" style="height:120px; border: 0px" ></div>
</div>
<span class="smallTitle">
  <fmt:message key="header.dataPoints" />
</span>
<div id="points-permissions-grid" style="height:450px;"></div>
<hr>
<div id="permissions">
  <span class="smallTitle">
  <fmt:message key="permissions.bulkApply" />
  <img class="ptr bulkPermissionsHelp"
   src="/images/help.png"
   title="<fmt:message key="permissions.bulkdEditDataPoint" />" 
   alt="<fmt:message key="permissions.bulkdEditDataPoint" />"
  />
  </span>

 <div class="formItem">
  <label class="formLabelRequired" style="width:110px" for="setPermission"><fmt:message key="pointEdit.props.permission.set"/></label>
  <div class="formField">
    <button id="applySetPermission"><fmt:message key="permissions.apply"/></button>
    <button id="clearSetPermission"><fmt:message key="permissions.clear"/></button>
    <input id="setPermissions" type="text"/>
    <img id="setPermissionsViewer" class="ptr" 
      src="/images/bullet_down.png" 
      title="<fmt:message key="users.permissions" />" 
      alt="<fmt:message key="users.permissions" />"
    />
   </div>
 </div>
 <div class="formItem">
    <label class="formLabelRequired" style="width:110px" for="readPermission"><fmt:message key="pointEdit.props.permission.read"/></label>
    <div class="formField">
      <button id="applyReadPermission"><fmt:message key="permissions.apply"/></button>
      <button id="clearReadPermission"><fmt:message key="permissions.clear"/></button>
      <input id="readPermissions" type="text"/>
      <img id="readPermissionsViewer" class="ptr" 
        src="/images/bullet_down.png" 
        title="<fmt:message key="users.permissions" />" 
        alt="<fmt:message key="users.permissions" />"
      />
    </div>
 </div>
</div>
