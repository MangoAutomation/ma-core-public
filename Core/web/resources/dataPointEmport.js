/*
 * Copyright (C) 2013 Infinite Automation. All rights reserved.
 * @author Jared Wiltshire/Terry Packer
 */


var showDataPointEmport;

require(["dijit/Dialog", "dojox/form/Uploader", "dijit/form/Button", "dojox/form/uploader/FileList",
         "dojo/dom", "dojo/on", "dojo/_base/lang", "dojo/dom-construct",
         "dgrid/OnDemandGrid", "dojo/store/Memory", "dojo/domReady!"],
function(Dialog, Uploader, Button, FileList, dom, on, lang, domConstruct, OnDemandGrid, Memory) {
    var form = dom.byId('msForm');
    
    var dataPointEmport = new Dialog({
        title: mangoTranslate('emport.import')
    }, "dataPointEmport");
    
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
    
    var dataPointEmportClose = new Button({
    }, "dataPointEmportClose");
    
    var uploaderStatus = new OnDemandGrid({
        columns: {
            filename: "Filename",
            rowsImported: "Rows Imported",
            rowsWithErrors: "Row Errors"
        }
    }, "uploaderStatus");
    
    var errorBoxParent;
    showDataPointEmport = function() {
        // move error box into dialog
        var errorBox = dom.byId('mangoErrorBox');
        errorBoxParent = errorBox.parentNode;
        errorBoxParent.removeChild(errorBox);
        var contentArea = dom.byId('dataPointEmportContent');
        contentArea.insertBefore(errorBox, contentArea.firstChild);
        
        dataPointEmport.show();
    };
    
    on(submit, "click", function() {
    	var dsid = dojo.byId("dsId");
    	dsid.value = (dataPointsDataSourceId);
        uploader.submit();
    });
    on(reset, "click", function() {
        uploader.reset();
    });
    
    on(dataPointEmportClose, "click", function() {
        dataPointEmport.hide();
    });
    
    on(dataPointEmport, "hide", function() {
        uploader.reset();
        
        var store = new Memory({data: []});
        uploaderStatus.set('store', store);
        
        // put error box back where it belongs
        var errorBox = dom.byId('deltaErrorBox');
        var contentArea = dom.byId('dataPointEmportContent');
        contentArea.removeChild(errorBox);
        errorBoxParent.insertBefore(errorBox, errorBoxParent.firstChild);
    });
    
    on(uploader, "complete", function(json) {
        var store = new Memory({data: json.fileInfo});
        uploaderStatus.set('store', store);
        
        for (var i = 0; i < json.fileInfo.length; i++) {
            var info = json.fileInfo[i];
            if (info.hasImportErrors === true) {
                for (var j = 0; j < info.errorMessages.length; j++) {
                    var error = info.errorMessages[j];
                    addErrorDiv(error);
                }
            }
        }
        
        if (machineStates) {
            machineStates.grid.refresh();
        }
    });

    if (require.has('file-multiple')) {
        var domnode = uploader.domNode.parentNode;
        if (uploader.addDropTarget && uploader.uploadType == 'html5') {
            uploader.addDropTarget(domnode);
        }
    }
});
