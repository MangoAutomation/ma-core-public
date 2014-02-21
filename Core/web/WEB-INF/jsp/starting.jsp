<%--
    Copyright (C) 2014 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>




<tag:page dwr="StartupDwr">
<script type="text/javascript">

require(["dijit/ProgressBar", "dojo/_base/window", "dojo/domReady!"], function(ProgressBar, win){
    var i = 0;
    var myProgressBar = new ProgressBar({
        style: "width: 300px"
    },"startupProgress");
    
    //Get the startup message because the translate tag will not work at
    // this point in the startup process
    StartupDwr.getStartingMessage(function (response){
        var startingMessageDiv = dojo.byId("startingMessage");
        startingMessageDiv.innerHTML = response.data.message; 
    });
    
    
    setInterval(function(){
        StartupDwr.getStartupProgress( function(response){
            
            //If the interval is > 100 then we should redirect
            if(response.data.progress >= 100)
                window.location.href = "login.htm";
            
            myProgressBar.set("value", response.data.progress);
            var startupMessageDiv = dojo.byId("startupMessage");
            startupMessageDiv.innerHTML = response.data.state;
       });
    }, 100);
});

</script>
    <div id="startingMessage" class='bigTitle'></div>
    <div id="startupProgress"></div>
    <div id="startupMessage">Mango is starting up, please wait...</div>

</tag:page>
