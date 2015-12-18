var lastMessage; //Holds the last received log message

require(["dijit/ProgressBar", "dojo/_base/window",'dojo/_base/xhr',"dojo/ready", "dojo/domReady!"], 
        function(ProgressBar, win, xhr, ready){

    
    
    //Initialized from existing info
	ready(function(){
	    getStatus(0);
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
               for (var i = data.messages.length - 1; i >= 0; i--) {
                   newMessages += data.messages[i] + "<br>";
               }
               
               //Push it out to the div
               var startupConsole = dijit.byId("startupConsole");
               //var startupConsole = registry.byId("startupConsole");
               startupConsole.set('content', newMessages + startupConsole.get('content'));
               
               
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
           }
        });
    }
    
    /**
     * Schedule another poll of the status
     */
    function schedulePoll(lastPollTime){
        setTimeout(function(){
            getStatus(lastPollTime);
        }, 100);
    }
    
});
