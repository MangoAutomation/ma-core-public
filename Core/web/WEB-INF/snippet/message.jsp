<%--
  Define a global store for the messages to use in translations
  Copyright (C) 2013 Infinite Automation. All rights reserved.
  @author Terry Packer, Jared Wiltshire
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<!-- Add in useful messages for page, also done in M2M2ContextListener, should be moved to here -->
<%
String[] mangoMessageKeys = {
		
	    "dsList.name",
	    "dsList.type",
	    "dsList.connection",
	    "dsList.status",
	    
        "table.confirmDelete.DataSource",        
        
        "table.error.wrongId",
        "table.error.dwr"
};
application.setAttribute("mangoMessageKeys",mangoMessageKeys);
%>

<script type="text/javascript">
	
	var mangoMsg = {};
	<c:forEach items="${mangoMessageKeys}" var="messageKey" >
	mangoMsg['${messageKey}'] = "<fmt:message key='${messageKey}' />";</c:forEach>

	
	function mangoImg(name) {
		return "/images/" + name;
	}
	
	function mangoTranslate(key, vars) {
	    var msg = mangoMsg[key];
	    if (typeof vars == 'undefined') {
	        return msg;
	    }
	    
	    if(typeof msg == 'undefined')
          console.log("Missing Key in mangoMsg:" + key);

	    
	    for (var i = 0; i < vars.length; i++) {
		        msg = msg.replace("'{" + i + "}'", vars[i]);
		        msg = msg.replace("{" + i + "}", vars[i]);
		}
	    return msg;
	}
</script>