<%--
    Copyright (C) 2015 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@tag body-content="empty"%>
  <tr>
    <td class="formLabel"><fmt:message key="javascript.permissions"/></td>
    <td class="formField">
      <input type="text" id="scriptPermissions" class="formLong"/>
      <tag:img png="bullet_down" onclick="permissionUI.viewPermissions('scriptPermissions')"/>
      <tag:help id="scriptPermissions"/>
    </td>
  </tr>
<script type="text/javascript">

function getScriptPermissions(){
    return $get('scriptPermissions');
}
function setScriptPermissions(permissions){
    //So we can accept strings or ScriptPermissions
	if(typeof permissions === 'object') {
		$set('scriptPermissions', permissions.permissions);
	}else{
	    $set('scriptPermissions', permissions);
	}
}
</script>