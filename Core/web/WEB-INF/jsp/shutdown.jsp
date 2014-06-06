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
            startupConsole.set('content', message.message + startupConsole.get('content'));
        }
    });
    
    //Initialize Existing info
    getStatus(0);
    
    var pollPeriodMs = 1000;
    var i = 0;
    var myProgressBar = new ProgressBar({
        style: "width: 300px"
    },"startupProgress");
    
    /**
     * Set the method to poll for updates
     */
    setInterval(function(){
        var timestamp = new Date().getTime() - pollPeriodMs;
        getStatus(timestamp);
    }, pollPeriodMs);
    
    /**
     * Get the status from the server
     **/
    function getStatus(timestamp){
       StartupDwr.getStartupProgress(timestamp, function(response){
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
	
	            var redirect = false;
	            
	            //Print the message for what Mango is doing
	            var startingMessageDiv = dojo.byId("startingMessage");
	            startingMessageDiv.innerHTML = response.data.processMessage; 
	            
	            var progress = 0;
	            //We don't care if we are starting up or shutting down, just need to know which one
	            if((response.data.startupProgress >= 100) && (response.data.shutdownProgress > 0)){
	                //Dirty hack for now to show that the restart has happened, once the web server is off no more messages.
	                progress = 98; //This looks like its almost restarted, then if it does it will flip over to 'Starting' messages
	            }
	
	            if(response.data.startupProgress < 100)
	                progress = response.data.startupProgress;
	
	            
	            //If the interval is > 100 then we should redirect, just remember at this point we could be shutting down
	             if((response.data.startupProgress >= 100) && (response.data.shutdownProgress == 0)){
	                 progress = 100; //Ready for start, redirect now
	                 redirect = true;
	             }
	            
	            
	            myProgressBar.set("value", progress + "%");
	            var startupMessageDiv = dojo.byId("startupMessage");
	            startupMessageDiv.innerHTML = response.data.state;
	            
	            //Do redirect?
	            if(redirect){
	                setTimeout(function(){
	                    window.location.href = response.data.startupUri;
	                }, 500);
	               
	            }
            });
    
    }
    
});

</script>
    <div style="width: 100%; padding: 1em 2em 1em 1em;"
            data-dojo-type="dijit/layout/ContentPane"
            data-dojo-props="region:'center'">
    <div id="startingMessage" class='bigTitle'></div>
    <div id="startupProgress"></div>
    <div id="startupMessage"></div>
    <div id="startupConsole"
            style=" height: 200px; margin: 1em 3em 1em 1em; border: 2px; padding: .2em 1em 1em 1em; overflow:auto; border: 2px solid; border-radius:10px; border-color: lightblue;"
            data-dojo-type="dijit/layout/ContentPane"></div>
    </div>
</tag:page>
