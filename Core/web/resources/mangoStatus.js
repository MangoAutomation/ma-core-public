var lastMessage; //Holds the last recieved log message
var pollLock = false; //Lock to ensure we don't have duplicate polls running

require(["dojo/topic","dijit/ProgressBar", "dojo/_base/window",'dojo/_base/xhr', "dojo/domReady!"], 
        function(topic, ProgressBar, win, xhr){

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
    
    //Initialized from existing info
    getStatus(0);
    
    var pollPeriodMs = 500;
    var i = 0;
    var myProgressBar = new ProgressBar({
        style: "width: 300px"
    },"startupProgress");
    
    
    setInterval(function(){
        var timestamp = new Date().getTime() - pollPeriodMs;
        getStatus(timestamp);
    }, pollPeriodMs);
    
    /**
     * Get the Startup Status
     **/
    function getStatus(timestamp){
        if(pollLock)
            return;
        else
            pollLock = true; //Lock the polls
        xhr.get({
           url: "/status/mango.json?time=" + timestamp,
           handleAs: "json",
           load: function(data){
               
               //Update the progress bar
               myProgressBar.set("value", data.startupProgress + "%");
               
               //Update my messages
               var startupMessageDiv = dojo.byId("startupMessage");
               startupMessageDiv.innerHTML = data.state;
               for(var i=0; i<data.messages.length; i++){
                   dojo.publish("startupTopic",[{
                       message:data.messages[i] + "<br>",
                       type: "message",
                       duration: -1, //Don't go away
                       }]
                   );
               }
               
               //Do redirect?
               if(data.startupProgress >= 100){
                   setTimeout(function(){
                       window.location.href = '';
                   }, 500);
                  
               }
               pollLock = false;
           },
           error: function(error){
               pollLock = false;           
           }
        });

        
    };
    
});