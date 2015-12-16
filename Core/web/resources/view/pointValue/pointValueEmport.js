/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Jared Wiltshire/Terry Packer
 */


var showPointValueEmport;
var closeImportErrorBox;

require(["dijit/Dialog", "dojox/form/Uploader", "dijit/form/Button", "dojox/form/uploader/FileList",
         "dojo/dom", "dojo/on", "dojo/_base/lang", "dojo/dom-construct",
         "dgrid/OnDemandGrid", "dojo/store/Memory", "dojo/cookie", "dojo/domReady!"],
function(Dialog, Uploader, Button, FileList, dom, on, lang, domConstruct, OnDemandGrid, Memory, cookie) {
    var form = dom.byId('msForm');
    
    var pointValueEmport = new Dialog({
        title: mangoTranslate('emport.import')
    }, "pointValueEmport");
    
    // note, according to https://dojotoolkit.org/reference-guide/1.9/dojox/form/Uploader.html#id4
    // we can't use just Uploader here but it seems that bug has been fixed in 1.9
    // https://bugs.dojotoolkit.org/ticket/14811
    var uploader = new Uploader({
        label: mangoTranslate('view.browse'),
        multiple: false,
        //Hack to override XHR request and set the CSRF header
		createXhr: function(){
			var xhr = new XMLHttpRequest();
			var timer;
			xhr.upload.addEventListener("progress", lang.hitch(this, "_xhrProgress"), false);
			xhr.addEventListener("load", lang.hitch(this, "_xhrProgress"), false);
			xhr.addEventListener("error", lang.hitch(this, function(evt){
				this.onError(evt);
				clearInterval(timer);
			}), false);
			xhr.addEventListener("abort", lang.hitch(this, function(evt){
				this.onAbort(evt);
				clearInterval(timer);
			}), false);
			xhr.onreadystatechange = lang.hitch(this, function(){
				if(xhr.readyState === 4){
	//				console.info("COMPLETE")
					clearInterval(timer);
					try{
						this.onComplete(JSON.parse(xhr.responseText.replace(/^\{\}&&/,'')));
					}catch(e){
						var msg = "Error parsing server result:";
						console.error(msg, e);
						console.error(xhr.responseText);
						this.onError(msg, e);
					}
				}
			});
			xhr.open("POST", this.getUrl());
			xhr.setRequestHeader("Accept","application/json");
			xhr.setRequestHeader("X-XSRF-TOKEN", cookie('XSRF-TOKEN'));
			
			timer = setInterval(lang.hitch(this, function(){
				try{
					if(typeof(xhr.statusText)){} // accessing this error throws an error. Awesomeness.
				}catch(e){
					//this.onError("Error uploading file."); // not always an error.
					clearInterval(timer);
				}
			}),250);
	
			return xhr;
		}
    }, "msUploader");
    
    var reset = new Button({
        label: mangoTranslate('view.clear')
    }, "msReset");
    
    var submit = new Button({
        label: mangoTranslate('view.submit')
    }, "msSubmit");
    
    var msFileList = new FileList({
        uploaderId: "msUploader"
    }, "msFileList");
    
    var pointValueEmportClose = new Button({
    }, "pointValueEmportClose");
    
    var uploaderStatus = new OnDemandGrid({
        columns: {
            filename: "Filename",
            rowsImported: "Rows Imported",
            rowsWithErrors: "Row Errors"
        }
    }, "uploaderStatus");
    
    var errorBoxParent;
    showPointValueEmport = function(actionUrl) {
    	//Set the inputs/action
    	uploader.set('url', actionUrl);

    	clearImportErrorBox();
    	clearUploadList();
        pointValueEmport.show();
    };
    
    on(submit, "click", function() {
        clearImportErrorBox();
        clearUploadList();
    	uploader.submit();
    });
    on(reset, "click", function() {
        uploader.reset();
        clearImportErrorBox();
        clearUploadList();
    });
    
    on(pointValueEmportClose, "click", function() {
        pointValueEmport.hide();
    });
    
    on(pointValueEmport, "hide", function() {
        uploader.reset();
        clearUploadList();
    });
    
    on(uploader, "complete", function(json) {
        var store = new Memory({data: json.fileInfo});
        uploaderStatus.set('store', store);
        uploaderStatus.refresh();
        
        for (var i = 0; i < json.fileInfo.length; i++) {
            var info = json.fileInfo[i];
            if (info.hasImportErrors === true) {
                for (var j = 0; j < info.errorMessages.length; j++) {
                    var error = info.errorMessages[j];
                    addImportErrorMessage(error);
                }
                show('importErrorBox');
            }
        }

    });

    if (require.has('file-multiple')) {
        var domnode = uploader.domNode.parentNode;
        if (uploader.addDropTarget && uploader.uploadType == 'html5') {
            uploader.addDropTarget(domnode);
        }
    }
    
    closeImportErrorBox = function(){
    	hide('importErrorBox');
    };
    
    function addImportErrorMessage(message){
    	var errorBox = dojo.byId('importErrors');
        var div = document.createElement('div');
        div.innerHTML = message;
        errorBox.appendChild(div);
    }
    
    function clearImportErrorBox(){
    	closeImportErrorBox();
    	var myNode = document.getElementById('importErrors');
    	while (myNode.firstChild) {
    	    myNode.removeChild(myNode.firstChild);
    	}
    }
    
    function clearUploadList(){
        var store = new Memory({data: []});
        uploaderStatus.set('store', store);
        uploaderStatus.refresh();
    }
    
});
