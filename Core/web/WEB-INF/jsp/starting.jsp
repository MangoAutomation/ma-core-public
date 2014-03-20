<%--
    Copyright (C) 2014 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>




<tag:page dwr="StartupDwr">
<script type="text/javascript">
var lastMessage; //Holds the last recieved log message

require(["dojo/topic","dijit/ProgressBar", "dojo/_base/window", "dojo/domReady!"], 
        function(topic, ProgressBar, win){

    //Setup the console messages target
    topic.subscribe("startupTopic", function(message) {
        //Message has members:
        // duration - int
        // message - string
        // type - string
        var startupConsole = dijit.byId("startupConsole");
        if (message.type == 'clear')
            startupConsole.set('content', "");
        else {
            startupConsole.set('content', 
                    startupConsole.get('content') + message.message + "</br>");
        }
    });
    
    
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
        StartupDwr.getStartupProgress(function(response){
            
            //Do we have a new message
            if(typeof response.data.message != 'undefined'){
	            if((typeof lastMessage == 'undefined')||(lastMessage != response.data.message)){
	                lastMessage = response.data.message;
		            dojo.publish("startupTopic",[{
		                    message:response.data.message,
		                    type: "message",
		                    duration: -1, //Don't go away
		                    }]
		            );
	            }
            }
            
            //If the interval is > 100 then we should redirect, just remember at this point we could be shutting down

            if((response.data.startupProgress >= 100) && (response.data.shutdownProgress == 0))
                window.location.href = response.data.startupUri;
            
            
            myProgressBar.set("value", response.data.startupProgress);
            var startupMessageDiv = dojo.byId("startupMessage");
            startupMessageDiv.innerHTML = response.data.state;
       });
    }, 100);
});

</script>
    <div style="width: 100%; padding: 1em 2em 1em 1em;"
            data-dojo-type="dijit/layout/ContentPane"
            data-dojo-props="region:'center'">
    <div id="startingMessage" class='bigTitle'></div>
    <div id="startupProgress"></div>
    <div id="startupMessage">Mango is starting up, please wait...</div>
    <div id="startupConsole"
            style=" height: 200px; margin: 1em 3em 1em 1em; border: 2px; padding: .2em 1em 1em 1em; overflow:auto; border: 2px solid; border-radius:10px; border-color: lightblue;"
            data-dojo-type="dijit/layout/ContentPane"></div>
    </div>
</tag:page>
