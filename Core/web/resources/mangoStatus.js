var lastMessage; //Holds the last received log message

require(["dijit/ProgressBar", "dojo/_base/window",'dojo/_base/xhr',"dojo/ready", "dojox/fx/scroll", "dojo/domReady!"], 
        function(ProgressBar, win, xhr, ready, scroll){

    
    
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
           url: "/status/mango",
           handleAs: "json",
           headers: { 'Content-Type' : 'application/json'},
           load: function(data){
               
               //Update the progress bar
               myProgressBar.set("value", data.startupProgress + "%");
               
               //Update my messages
               var startupMessageDiv = dojo.byId("startupMessage");
               startupMessageDiv.innerHTML = data.state;
               
               //Do redirect?
               if(data.startupProgress >= 100){
                   setTimeout((function(dataAtStartup){
                	   if(typeof data.startupUri != 'undefined')
                		   window.location.href = data.startupUri;
                	   else
                		   window.location.href = '';
                   })(data), 500);
                  
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
