/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Jared Wiltshire/Terry Packer
 */


var showPointValueEmport;
var closeImportErrorBox;

require(["dijit/Dialog", "dojox/form/Uploader", "dijit/form/Button", "dojox/form/uploader/FileList",
         "dojo/dom", "dojo/on", "dojo/_base/lang", "dojo/dom-construct",
         "dgrid/OnDemandGrid", "dojo/store/Memory", "dojo/domReady!"],
function(Dialog, Uploader, Button, FileList, dom, on, lang, domConstruct, OnDemandGrid, Memory) {
    var form = dom.byId('msForm');
    
    var pointValueEmport = new Dialog({
        title: mangoTranslate('emport.import')
    }, "pointValueEmport");
    
    // note, according to https://dojotoolkit.org/reference-guide/1.9/dojox/form/Uploader.html#id4
    // we can't use just Uploader here but it seems that bug has been fixed in 1.9
    // https://bugs.dojotoolkit.org/ticket/14811
    var uploader = new Uploader({
        label: mangoTranslate('view.browse'),
        multiple: true
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
