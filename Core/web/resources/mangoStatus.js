var lastMessage; //Holds the last received log message

require(["dijit/ProgressBar", "dojo/_base/window",'dojo/_base/xhr',"dojo/ready", "dojox/fx/scroll", "dojo/domReady!"], 
        function(ProgressBar, win, xhr, ready, scroll){

    
    
    //Initialized from existing info
	ready(function(){
	    getStatus(0);
	    //Set the tab index so we can see if it has focus
	    dijit.byId("startupConsole").domNode.tabIndex = "1";
	    
	});
    
    var i = 0;
    var myProgressBar = new ProgressBar({
        style: "width: 300px"
    },"startupProgress");

    
    /**
     * Get the Startup Status
     **/
    function getStatus(timestamp){
        var lastPollTime = new Date().getTime();
        xhr.get({
           url: "/status/mango?time=" + timestamp,
           handleAs: "json",
           headers: { 'Content-Type' : 'application/json'},
           load: function(data){
               
               //Update the progress bar
               myProgressBar.set("value", data.startupProgress + "%");
               
               //Update my messages
               var startupMessageDiv = dojo.byId("startupMessage");
               startupMessageDiv.innerHTML = data.state;
               
               //Combine this block of messages into 1 HTML String
               var newMessages = "";
               for (var i = 0; i < data.messages.length; i++) {
            	   //Replace any \n with <br> for cleaner messages
            	   var msg = data.messages[i].replace(/\n/g, "<br />");
            	   if(msg.startsWith('WARN') || msg.startsWith('ERROR') || msg.startsWith('FATAL')){
            		   msg = '<span style="color: red">' + msg + "</span>";
            	   }
                   newMessages += msg;
               }
               
               //Push it out to the div
               var startupConsole = dijit.byId("startupConsole");
               //var startupConsole = registry.byId("startupConsole");
               startupConsole.set('content', startupConsole.get('content') + newMessages);
               
               //Scrol to bottom of div if we added a new message
               if((data.messages.length > 0)&&(document.activeElement != startupConsole.domNode))
            	   startupConsole.domNode.scrollTop = startupConsole.domNode.scrollHeight;
               
               //Do redirect?
               if(data.startupProgress >= 100){
                   setTimeout(function(){
                	   if(typeof data.startupUri != 'undefined')
                		   window.location.href = data.startupUri;
                	   else
                		   window.location.href = '';
                   }, 500);
                  
               }else{
                   schedulePoll(lastPollTime);
               }

           },
           error: function(error, ioArgs){
               alert(error);
               //Kill Polling or Wait and try again?
           }
        });
    }
    
    /**
     * Schedule another poll of the status
     */
    function schedulePoll(lastPollTime){
        setTimeout(function(){
            getStatus(lastPollTime);
        }, 1000);
    }
    
});
