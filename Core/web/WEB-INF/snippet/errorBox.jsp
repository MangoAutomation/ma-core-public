<%--
    Copyright (C) 2013 Deltamation Software. All rights reserved.
    @author Jared Wiltshire,Terry Packer
--%>

<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<script type="text/javascript" >
    function closeErrorBox() {
        var errorBox = dojo.byId("mangoErrorBox");
        hide(errorBox);
        var divs = errorBox.getElementsByTagName("div");
        while(divs.length > 0) {
            errorBox.removeChild(divs[0]);
        }
    }
    
    // TODO rename these, way too generic
    function addErrorDiv(message) {
        var errorBox = dojo.byId("mangoErrorBox");
        var div = document.createElement('div');
        div.innerHTML = message;
        errorBox.appendChild(div);
        show(errorBox);
    }
    
    function addContextualMessage(key, message) {
        addErrorDiv("Error with '" + key + "': " + message);
    }
    
    function addMessage(message) {
        if (message.contextualMessage) {
            addContextualMessage(message.contextKey, message.contextualMessage);
        } else {
            addErrorDiv(message.genericMessage);
        }
    }
    
    function addMessages(messages) {
        for (var i = 0; i < messages.length; i++) {
            addMessage(messages[i])
        }
    }
</script>

<div id="mangoErrorBox" class="borderDiv" style="display:none">
  <tag:img id="closeErrorBoxImg" png="cross" onclick="closeErrorBox()" title="downtime.view.clearErrors"/>
</div>
