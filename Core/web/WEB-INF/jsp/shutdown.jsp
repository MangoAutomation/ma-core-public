<%--
    Copyright (C) 2014 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>




<tag:page dwr="StartupDwr">
    <div style="width: 100%; padding: 1em 2em 1em 1em;"
            data-dojo-type="dijit/layout/ContentPane"
            data-dojo-props="region:'center'">
    <div id="startingMessage" class='bigTitle'></div>
    <div id="startupProgress"></div>
    <div id="startupMessage"></div>
    <div id="startupConsole"
            style=" height: 500px; margin: 1em 3em 1em 1em; border: 2px; padding: .2em 1em 1em 1em; overflow:auto; border: 2px solid; border-radius:10px; border-color: lightblue;"
            data-dojo-type="dijit/layout/ContentPane"></div>
    </div>
    
    <script type="text/javascript">
var lastMessage; //Holds the last recieved log message
var pollLock = false; //Ensure we don't overpoll mango

require(["dojo/_base/xhr", "dijit/ProgressBar", "dojo/_base/window", "dojo/ready", "dojo/domReady!"], 
        function(xhr, ProgressBar, win, ready){
	
    //Initialize Existing info
	ready(function(){
	    getStatus(0);
	    //Set the tab index so we can see if it has focus
	    dijit.byId("startupConsole").domNode.tabIndex = "1";
	    
	});
    
    var pollPeriodMs = 500;
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
        if(pollLock)
            return;
        else
            pollLock = true;
        xhr.get({
            url: "/status/mango?time=" + timestamp,
            handleAs: "json",
            headers: { 'Content-Type' : 'application/json'},
            load: function(data){
 	            
                //Update my messages
                var startupMessageDiv = dojo.byId("startupMessage");
                startupMessageDiv.innerHTML = data.state;
                var startupConsole = dijit.byId("startupConsole");
                var msgContent = '';
                for(var i=0; i<data.messages.length; i++){
                   	var msg = data.messages[i].replace(/\n/g, "<br />");
                   	if(msg.startsWith('WARN') || msg.startsWith('ERROR') || msg.startsWith('FATAL')){
                	   msg = '<span style="color: red">' + msg + "</span>";
                	}
                    msgContent = msgContent + msg;
                }

                startupConsole.set('content', startupConsole.get('content') + msgContent);
              //Scrol to bottom of div if we added a new message
                if((data.messages.length > 0)&&(document.activeElement != startupConsole.domNode))
                	startupConsole.domNode.scrollTop = startupConsole.domNode.scrollHeight;
                
	            var redirect = false;
	            
	            var progress = 0;
	            //We don't care if we are starting up or shutting down, just need to know which one
	            if((data.startupProgress >= 100) && (data.shutdownProgress > 0)){
	                //Dirty hack for now to show that the restart has happened, once the web server is off no more messages.
	                progress = 98; //This looks like its almost restarted, then if it does it will flip over to 'Starting' messages
	            }
	
	            if(data.startupProgress < 100){
	                progress = data.startupProgress;
	                //Also check if we can early re-direct, ie. the system is ready for the login page
		            if(typeof data.startupUri != 'undefined')
		            	redirect = true;
	            }
	

	            
	            //If the interval is > 100 then we should redirect, just remember at this point we could be shutting down
	             if((data.startupProgress >= 100) && (data.shutdownProgress == 0)){
	                 progress = 100; //Ready for start, redirect now
	                 redirect = true;
	             }
	            

	            //Do redirect?
	            if(redirect){
	                setTimeout(function(){
	                    window.location.href = data.startupUri;
	                }, 500);
	               
	            }
                //Update the progress bar
                myProgressBar.set("value", progress + "%");
	            
	            pollLock = false; 
            },
            error: function(error){
                pollLock = false;           
            }
        });
    }
    
});
</script>
</tag:page>
