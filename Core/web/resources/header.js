/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
*/
dojo.require("dojox.layout.FloatingPane");

//The following allows fixes to the positioning of the Help Floating Pane
var ConstrainedFloatingPane;

require(["dojo/_base/fx", "dojo/_base/lang", "dojo/dom-style", "dojo/dnd/move", "dojo/dom-geometry", "dojo/domReady!"], 
        function(fx, lang, domStyle, move, domGeom) {
    
    ConstrainedFloatingPane = dojo.declare(dojox.layout.FloatingPane, {
        
        show: function(/* Function? */callback){
            // summary:
            //      Show the FloatingPane
            var anim = fx.fadeIn({node:this.domNode, duration:this.duration,
                beforeBegin: lang.hitch(this,function(){
                    this.domNode.style.display = "";
                    this.domNode.style.visibility = "visible";
                    if (this.dockTo && this.dockable) { this.dockTo._positionDock(null); }
                    if (typeof callback == "function") { callback(); }
                    this._isDocked = false;
                    if (this._dockNode) {
                        this._dockNode.destroy();
                        this._dockNode = null;
                    }
                })
            }).play();
            // use w / h from content box dimensions and x / y from position
            var contentBox = domGeom.getContentBox(this.domNode)
            var pos = domGeom.position(this.domNode, true);
            pos = lang.mixin(pos, {w: contentBox.w, h: contentBox.h});
            this.resize(pos);
            this._onShow(); // lazy load trigger
        }
        
    });
});

    

//
// Error handling
window.onerror = function mangoHandler(desc, page, line)  {
    BrowserDetect.init();
    if (checkCombo(BrowserDetect.browser, BrowserDetect.version, BrowserDetect.OS)) {
        MiscDwr.jsError(desc, page, line, BrowserDetect.browser, BrowserDetect.version, BrowserDetect.OS,
                window.location.href);
    }
    return false;
};


mango.header = {};
mango.header.onLoad = function() {
	//Removed when replaced alarm notification widget
//    if (dojo.isIE)
//        mango.header.evtVisualizer = new IEBlinker($("__header__alarmLevelDiv"), 500, 200);
//    else
//        mango.header.evtVisualizer = new ImageFader($("__header__alarmLevelDiv"), 75, .2);
    mango.longPoll.start();
};

function hMD(desc, source) {
    var c = $("headerMenuDescription");
    if (desc) {
        var srcPosition = dojo.position(source, true);
        c.innerHTML = desc;
        c.style.left = (srcPosition.x + 16) +"px";
        c.style.top = (srcPosition.y - 10) +"px";
        show(c);
        
        // Check the window bound.
        var cBounds = dojo.position(c, true);
        if (cBounds.x + cBounds.w > window.innerWidth)
            c.style.left = (srcPosition.x - cBounds.w) +"px";
    }
    else
        hide(c);
};

//
// Help
function help(documentId, source) {
    var fpId = "documentationPane";
    var fp = dijit.byId(fpId);
    if (!fp) {
        var div = dojo.doc.createElement("div");
        dojo.body().appendChild(div);
        fp = new ConstrainedFloatingPane({
            id: fpId,
            title: mango.i18n["js.help.loading"], 
            closeable: true,
            dockable: false,
            resizable: true,
            style: "position: absolute; zIndex: 980; padding: 2px; left:10; top:10;"
        }, div);
        
        fp.startup();
    }
    
    var top, left;
    if (source) {
        var position = dojo.position(source, false);
        left = position.x + position.w;
        top = position.y + position.h;
    }
    else {
        left = 10;
        top = 10;
    }
    
    require(["dojo/dom-style", "dojo/dom-geometry"], function(domStyle, domGeom) {
        var scroll = domGeom.docScroll();
        left += scroll.x;
        top += scroll.y;
        
        domStyle.set(fp.domNode, {
            width:"400px",
            height:"300px",
            left:left +"px",
            top:top +"px"
        });
    });
    
    helpImpl(documentId);
};

function helpImpl(documentId) {
    MiscDwr.getDocumentationItem(documentId, function(result) {
        var fpId = "documentationPane";
        var fp = dijit.byId(fpId);
        
        if (result.error) {
            fp.set('title', mango.i18n["js.help.error"]);
            fp.set('content', result.error);
        }
        else {
            //var t = "<img src='images/help_doc.png'/> "+ ;
            fp.set('title', result.title);
            var content = result.content;
            if (result.relatedList && result.relatedList.length > 0) {
                content += "<p><b>"+ mango.i18n["js.help.related"] +"</b><br/>";
                for (var i=0; i<result.relatedList.length; i++)
                    content += "<a class='ptr' onclick='helpImpl(\""+ result.relatedList[i].id +"\");'>"+
                            result.relatedList[i].title +"</a><br/>";
                content += "</p>";
            }
            if (result.lastUpdated)
                content += "<p>"+ mango.i18n["js.help.lastUpdated"] +": "+ result.lastUpdated +"</p>";
            content = "<div>"+ content +"</div><div style='height:13px'></div>";
            fp.set('content', content);
        }
        
        fp.show();

    });
};

//
// Sound related stuff

//Test for HTML5 Support
var a = document.createElement('audio');
if(!!(a.canPlayType && a.canPlayType('audio/mpeg;').replace(/no/, ''))){

	//Has HTML 5
	function Html5SoundPlayer() {
		
	    //Set to true when sound is successfully loaded
	    this.hasLevel1 = false;
	    this.hasLevel2 = false;
	    this.hasLevel3 = false;
	    this.hasLevel4 = false;
	    
	    //Set to true when sound has completed its load attempt
	    this.level1Ready = false;
	    this.level2Ready = false;
	    this.level3Ready = false;
	    this.level4Ready = false;
		
	    this.soundId = null;
	    this.mute = false;
	    this.timeoutId;
	    
		var self = this;
		
		this.level1 = new Audio();
		this.level1.addEventListener("ended", function(){self._repeatDelay();});
		this.level1.addEventListener('error', function(evt){
			console.log('Level1 Sound Failed to load');
			self.hasLevel1 = false;
			self.level1Ready = true;
		});
		this.level1.addEventListener('loadeddata', function(evt){
			self.hasLevel1 = true;
			self.level1Ready = true;
		});
		this.level1.src = '/audio/information.mp3';
		
		this.level2 = new Audio();
		this.level2.addEventListener("ended", function(){self._repeatDelay();});
		this.level2.addEventListener('error', function(evt){
			console.log('Level2 Sound Failed to load');
			self.hasLevel2 = false;
			self.level2Ready = true;
		});
		this.level2.addEventListener('loadeddata', function(evt){
			self.hasLevel2 = true;
			self.level2Ready = true;
		});
		this.level2.src = '/audio/urgent.mp3';
		
		this.level3 = new Audio();
		this.level3.addEventListener("ended", function(){self._repeatDelay();});
		this.level3.addEventListener('error', function(evt){
			console.log('Level3 Sound Failed to load');
			self.hasLevel3 = false;
			self.level3Ready = true;
		});
		this.level3.addEventListener('loadeddata', function(evt){
			self.hasLevel3 = true;
			self.level3Ready = true;
		});
		this.level3.src = '/audio/critical.mp3';
		
		
		this.level4 = new Audio();
		this.level4.addEventListener("ended", function(){self._repeatDelay();});
		this.level4.addEventListener('error', function(evt){
			console.log('Level4 Sound Failed to load');
			self.hasLevel4 = false;
			self.level4Ready = true;
		});
		this.level4.addEventListener('loadeddata', function(evt){
			self.hasLevel4 = true;
			self.level4Ready = true;
		});
		this.level4.src = '/audio/lifesafety.mp3';
	    
	    this.play = function(soundId) {
	        this.stop();
	        this.soundId = soundId;
	        if (!this.mute)
	        	this._repeat();
	    };
	    
	    this.stop = function() {
	        if (this.soundId != null) {
	            var sid = this.soundId;
	            this.soundId = null;
	            this._stopRepeat(sid);
	        }
	    };
	    
	    this.isMute = function() {
	        return this.mute;
	    };
	    
	    this.setMute = function(muted) {
	    	if (muted != this.mute) {
		        this.mute = muted;
		        if (this.soundId) {
			        if (muted)
			            this._stopRepeat(this.soundId);
			        else
			            this._repeat();
		        }
	    	}
	    };
	    
	    this._stopRepeat = function(sId) {
	    	if(self.playerReady() && self.hasSound(sId)){
		        self[sId].pause();
		        self[sId].currentTime = 0;
	    	}
	        clearTimeout(self.timeoutId);
	    };
	    
	    this._repeat = function() {
	        if (self.playerReady() === true) {
	            if (self.soundId && !self.mute && self.hasSound(self.soundId)) {
	            	self[self.soundId].play();
	            }
	        }
	        else
	            // Wait for the sound manager to load.
	            setTimeout(self._repeat, 500);
	    };
	    
	    this._repeatDelay = function() {
	        if (self.soundId && !self.mute)
	            self.timeoutId = setTimeout(self._repeat, 10000);
	    };
	    
	    this.hasSound = function(soundId){
	    	switch(soundId){
	    	case 'level1':
	    		return this.hasLevel1;
	    	case 'level2':
	    		return this.hasLevel2;
	    	case 'level3':
	    		return this.hasLevel3;
	    	case 'level4':
	    		return this.hasLevel4;
	    	default:
	    	return false;
	    	}
	    };
	    
	    this.playerReady = function(){
	    	return (this.level1Ready && this.level2Ready && this.level3Ready && this.level4Ready)
	    }
	};
	mango.soundPlayer = new Html5SoundPlayer();
	
}else{
	if (typeof(soundManager) != "undefined") {
		soundManager.url = '/';
	    soundManager.debugMode = false;
	    soundManager.onloadFinished = false;
	    soundManager.onload = function() {
	        soundManager.createSound({ id:'level1', url:'/audio/information.mp3' });
	        soundManager.createSound({ id:'level2', url:'/audio/urgent.mp3' });
	        soundManager.createSound({ id:'level3', url:'/audio/critical.mp3' });
	        soundManager.createSound({ id:'level4', url:'/audio/lifesafety.mp3' });
	        soundManager.onloadFinished = true;
	    };
	}
	function SoundManagerSoundPlayer() {
	    this.soundId;
	    this.mute = false;
	    this.timeoutId;
	    var self = this;
	    
	    this.play = function(soundId) {
	        this.stop();
	        this.soundId = soundId;
	        if (!this.mute)
	        	this._repeat();
	    };
	    
	    this.stop = function() {
	        if (this.soundId) {
	            var sid = this.soundId;
	            this.soundId = null;
	            this._stopRepeat(sid);
	        }
	    };
	    
	    this.isMute = function() {
	        return this.mute;
	    };
	    
	    this.setMute = function(muted) {
	    	if (muted != this.mute) {
		        this.mute = muted;
		        if (this.soundId) {
			        if (muted)
			            this._stopRepeat(this.soundId);
			        else
			            this._repeat();
		        }
	    	}
	    };
	    
	    this._stopRepeat = function(sId) {
	        soundManager.stop(sId);
	        clearTimeout(this.timeoutId);
	    };
	    
	    this._repeat = function() {
	        if (soundManager.onloadFinished) {
	            if (self.soundId && !self.mute) {
	                var snd = soundManager.getSoundById(self.soundId);
	                if (snd) {
	                    if (snd.readyState == 0 || snd.readyState == 1) {
	                        if (snd.readyState == 0)
	                            // Load the sound
	                            snd.load(snd.options);
	                        // Wait for the sound to load.
	                        setTimeout(self._repeat, 500);
	                    }
	                    else if (snd.readyState == 3)
	                        // The sound exists, so play it.
	                        soundManager.play(self.soundId, { onfinish: self._repeatDelay } );
	                }
	            }
	        }
	        else
	            // Wait for the sound manager to load.
	            setTimeout(self._repeat, 500);
	    };
	    
	    this._repeatDelay = function() {
	        if (self.soundId && !self.mute)
	            self.timeoutId = setTimeout(self._repeat, 10000);
	    };
	};
	mango.soundPlayer = new SoundManagerSoundPlayer();
}
//
// Browser detection
var BrowserDetect = {
    init: function () {
        this.browser = this.searchString(this.dataBrowser) || "An unknown browser";
        this.version = this.searchVersion(navigator.userAgent)
            || this.searchVersion(navigator.appVersion)
            || "an unknown version";
        this.OS = this.searchString(this.dataOS) || "an unknown OS ("+ navigator.userAgent +")";
    },
    searchString: function (data) {
        for (var i=0;i<data.length;i++) {
            var dataString = data[i].string;
            var dataProp = data[i].prop;
            this.versionSearchString = data[i].versionSearch || data[i].identity;
            if (dataString) {
                if (dataString.indexOf(data[i].subString) != -1)
                    return data[i].identity;
            }
            else if (dataProp)
                return data[i].identity;
        }
    },
    searchVersion: function (dataString) {
        var index = dataString.indexOf(this.versionSearchString);
        if (index == -1) return;
        return parseFloat(dataString.substring(index+this.versionSearchString.length+1));
    },
    dataBrowser: [
        {
            string: navigator.userAgent,
            subString: "Chrome",
            identity: "Chrome"
        },
        {
            string: navigator.userAgent,
            subString: "OmniWeb",
            versionSearch: "OmniWeb/",
            identity: "OmniWeb"
        },
        {
            string: navigator.vendor,
            subString: "Apple",
            identity: "Safari",
            versionSearch: "Version"
        },
        {
            prop: window.opera,
            identity: "Opera"
        },
        {
            string: navigator.vendor,
            subString: "iCab",
            identity: "iCab"
        },
        {
            string: navigator.vendor,
            subString: "KDE",
            identity: "Konqueror"
        },
        {
            string: navigator.userAgent,
            subString: "Firefox",
            identity: "Firefox"
        },
        {
            string: navigator.vendor,
            subString: "Camino",
            identity: "Camino"
        },
        {		// for newer Netscapes (6+)
            string: navigator.userAgent,
            subString: "Netscape",
            identity: "Netscape"
        },
        {
            string: navigator.userAgent,
            subString: "MSIE",
            identity: "Explorer",
            versionSearch: "MSIE"
        },
        {
            string: navigator.userAgent,
            subString: "Gecko",
            identity: "Mozilla",
            versionSearch: "rv"
        },
        { 		// for older Netscapes (4-)
            string: navigator.userAgent,
            subString: "Mozilla",
            identity: "Netscape",
            versionSearch: "Mozilla"
        }
    ],
    dataOS : [
        {
            string: navigator.platform,
            subString: "Win",
            identity: "Windows"
        },
        {
            string: navigator.platform,
            subString: "Mac",
            identity: "Mac"
        },
        {
            string: navigator.userAgent,
            subString: "iPhone",
            identity: "iPhone/iPod"
        },
        {
            string: navigator.userAgent,
            subString: "Android",
            identity: "Android"
        },
        {
            string: navigator.platform,
            subString: "Linux",
            identity: "Linux"
        }
    ]
};

function checkCombo(browser, version, os) {
    if (browser == "Firefox" && version > 1)
        return true;
    if (browser == "Explorer" && version > 6 && os == "Windows")
        return true;
    if (browser == "Chrome")
        return true;
    if (browser == "Safari" && version > 3 && os == "Mac")
        return true;
    if (browser == "Safari" && version > 3 && os == "Android")
        return true;
    return false;
};
