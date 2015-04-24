<%--
    Copyright (C) 2015 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%>
  <tr>
    <td class="formLabel"><fmt:message key="scripting.permission.dataSource"/></td>
    <td class="formField">
      <input type="text" id="scriptDataSourcePermission" class="formLong"/>
      <tag:img png="bullet_down" onclick="permissionUI.viewPermissions('scriptDataSourcePermission')"/>
    </td>
  </tr>
  <tr>
    <td class="formLabel"><fmt:message key="scripting.permission.dataPointRead"/></td>
    <td class="formField">
      <input type="text" id="scriptDataPointReadPermission" class="formLong"/>
      <tag:img png="bullet_down" onclick="permissionUI.viewPermissions('scriptDataPointReadPermission')"/>
      <tag:help id="scriptPermissions"/>
    </td>
  </tr>
  <tr>
    <td class="formLabel"><fmt:message key="scripting.permission.dataPointSet"/></td>
    <td class="formField">
      <input type="text" id="scriptDataPointSetPermission" class="formLong"/>
      <tag:img png="bullet_down" onclick="permissionUI.viewPermissions('scriptDataPointSetPermission')"/>
    </td>
  </tr>
<script type="text/javascript">

function getScriptPermissions(){
	return {
			dataSourcePermissions: $get('scriptDataSourcePermission'),
			dataPointSetPermissions: $get('scriptDataPointSetPermission'),
			dataPointReadPermissions: $get('scriptDataPointReadPermission')
	};
}
function setScriptPermissions(permissions){
	$set('scriptDataSourcePermission', permissions.dataSourcePermissions);
	$set('scriptDataPointSetPermission', permissions.dataPointSetPermissions);
	$set('scriptDataPointReadPermission', permissions.dataPointReadPermissions);
}
</script>