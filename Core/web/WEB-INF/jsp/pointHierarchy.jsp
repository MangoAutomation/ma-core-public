<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>
<%@page import="com.serotonin.m2m2.vo.DataPointVO"%>
<tag:page showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" dwr="PointHierarchyDwr">
<jsp:attribute name="styles">
  <style>
    html > body .dijitTreeNodeLabelSelected { background-color: inherit; color: inherit; }
    .dijitTreeIcon { display: none; }
  </style>
</jsp:attribute>

<jsp:body>
  <script type="text/javascript">
    dojo.require("dijit.Tree");
    dojo.require("dijit.tree.TreeStoreModel");
    dojo.require("dojo.data.ItemFileWriteStore");
    dojo.require("dijit.tree.dndSource");
    
    var rootItem;
    var store;
    var tree;
    var editDialog;
    var editMap = {};
    
    function $$(item, attr, value) {
        if (typeof(value) == "undefined")
            // Get
            return store.getValue(item, attr);
        // Set
        store.setValue(item, attr, value);
    };
    var selectedFolderNode;
    
    dojo.ready(function() {
        PointHierarchyDwr.getPointHierarchy(initCB);
        setErrorMessage();
    });
    
    function initCB(rootFolder) {
        var storeItems = [];
        rootItem = {
                name: '<fmt:message key="pointHierarchy.root"/>',
                folderId: 0,
                children: []
        };
        storeItems.push(rootItem);
        addFolder(rootItem.children, rootFolder);
        
        store = new dojo.data.ItemFileWriteStore({data: { label: 'name', items: storeItems } });
        
        var div = dojo.create("div");
        $("treeDiv").appendChild(div);

        // Create the tree.
        tree = new dijit.Tree({
            model: new dijit.tree.ForestStoreModel({ store: store }),
            showRoot: false,
            persist: false,
            dndController: "dijit.tree.dndSource",
            checkItemAcceptance: function(target, source, position) {
                var node = dijit.getEnclosingWidget(target);
                var folderId = $$(node.item, "folderId");
                return typeof(folderId) != "undefined";
            },
            onClick: function(item, widget) {
                setErrorMessage();
                var folderId = $$(item, "folderId");
                if (folderId) {
                    selectedFolderNode = widget;
                    editFolderName(item, widget);
                }
                else
                    hide("folderEditDiv");
            },
            _createTreeNode: function(/*Object*/ args) {
                var tnode = new dijit._TreeNode(args);
                tnode.labelNode.innerHTML = args.label;
                selectedFolderNode = tnode;
                return tnode;
            }              
        }, div);
        
        tree._expandNode(tree.getNodesByItem(rootItem)[0]);
        
        hide("loadingImg");
        show("treeDiv");
    }
    
    function addFolder(parent, pointFolder) {
        // Add subfolders
        var i;
        for (i=0; i<pointFolder.subfolders.length; i++) {
            var folder = pointFolder.subfolders[i];
            var item = {
                    name: "<img src='images/folder_brick.png'/> "+ folder.name,
                    folderId: folder.id,
                    folderName: folder.name,
                    children: []
            };
            parent.push(item);
            addFolder(item.children, folder);
        }
        
        // Add points
        for (i=0; i<pointFolder.points.length; i++) {
            var point = pointFolder.points[i];
            var item = { 
                    name: "<img src='images/icon_comp.png'/> "+ point.extendedName,
                    point: makeNonTreeItem(point)
            };
            parent.push(item);
        }
    }
    
    function newFolder() {
        setErrorMessage();
        var name = "<fmt:message key="pointHierarchy.defaultName"/>";
        var item = {
                name: "<img src='images/folder_brick.png'/> "+ name,
                folderId: <c:out value="<%= Common.NEW_ID %>"/>,
                folderName: name,
                children: []
        };
        
        var newItem = store.newItem(item, {parent: rootItem, attribute: "children"});
        // selectedFolderNode gets set in the _createTreeNode tree callback.
        tree.set('path', selectedFolderNode.getTreePath());
        var widget = tree.getNodesByItem(newItem)[0];
        editFolderName(newItem, widget);
    }
    
    function save() {
        setErrorMessage();
        hide("folderEditDiv");
        var rootFolder = gatherTreeData();
        startImageFader($("saveBtn"));
        PointHierarchyDwr.savePointHierarchy(rootFolder, function(rootFolder) {
            stopImageFader($("saveBtn"));
            setErrorMessage("<fmt:message key="pointHierarchy.saved"/>");
            tree.destroy();
            initCB(rootFolder);
        });
    }
    
    function gatherTreeData() {
        var rootFolder = { id: 0, name: "root", subfolders: [], points: [] };
        gatherTreeDataRecur(tree.model.root.children[0], rootFolder);
        return rootFolder;
    }
    
    function gatherTreeDataRecur(node, folder) {
        if (node.children) {
            for (var i=0; i<node.children.length; i++) {
                var child = node.children[i];
                var folderId = $$(child, "folderId");
                if (folderId) {
                    var subfolder = {
                            id: folderId,
                            name: $$(child, "folderName"),
                            subfolders: [],
                            points: []
                    };
                    folder.subfolders.push(subfolder);
                    gatherTreeDataRecur(child, subfolder);
                }
                else {
                    var point = $$(child, "point");
                    delete point.extendedName;
                    delete point._type;
                    folder.points.push(point);
                }
            }
        }
    }
    
    function deleteFolder() {
        setErrorMessage();
        
        if (selectedFolderNode.item.children.length > 0) {
            if (!confirm("<fmt:message key="pointHierarchy.deleteConfirm"/>"))
                return;
        }
        
        while (selectedFolderNode.item.children && selectedFolderNode.item.children.length > 0) {
            var child = selectedFolderNode.item.children[0];
            selectedFolderNode.tree.model.pasteItem(child, selectedFolderNode.item, selectedFolderNode.getParent().item);
        }

        store.deleteItem(selectedFolderNode.item);
        hide("folderEditDiv");
    }
    
    function saveFolder() {
        setErrorMessage();
        var name = $get("folderName");
        if (!name || name == "")
            alert("<fmt:message key="pointHierarchy.noName"/>");
        else {
            $$(selectedFolderNode.item, "folderName", name);
            selectedFolderNode.labelNode.innerHTML = "<img src='images/folder_brick.png'/> "+ name;
        }
    }
    
    function setErrorMessage(message) {
        if (!message)
            hide("errorMessage");
        else {
            $("errorMessage").innerHTML = message;
            show("errorMessage");
        }
    }
    
    function editFolderName(item, widget) {
        $set("folderName", $$(item, "folderName"));
        show("folderEditDiv");
        
        // Set the position of the editing panel to the location of the folder's row.
        require(["dojo/_base/html", "dojo/dom-style", "dojo/window", "dijit/focus"], function(html, domStyle, win, foc) {
            var position = html.position(widget.domNode, true);
            domStyle.set("folderEditDiv", "top", position.y +"px");
            win.scrollIntoView("folderEditDiv");
            setTimeout(function() { $("folderName").focus(); }, 100);
        });
    }
    
    function openEditor() {
    	if(editDialog){
    		editDialog.show();
    		return;
    	}
    	require(["dijit/form/Select", "dijit/form/Button", "dijit/form/ValidationTextBox", "dijit/Dialog", "dojo/dom-construct", "dojo/dom-attr"], 
    			function(Select, Button, ValidationTextBox, Dialog, domConstruct) {
   			editDialog = new Dialog({
   				title:"Hello Point Hierarchy!",
   				content:"<div id=\"dialogContent\"></div><div id=\"dialogFooter\"></div>", //
   				style:"width: 600px"
   			});
   			dojo.place(dojo.byId("newEditSelect"), dojo.byId("dialogContent"), "first");
   			domConstruct.create("div", {id:"addEditField"}, "dialogContent", "last");
   			domConstruct.create("hr", {}, "dialogFooter", "first");
   			domConstruct.create("div", {id:"dialogButtonBar"}, "dialogFooter", "last");
   			domConstruct.create("div", {id:"dialogCancel"}, "dialogButtonBar", "first");
   			domConstruct.create("div", {id:"dialogSubmit"}, "dialogButtonBar", "last");
   			var add = new Button({
   				label:"<fmt:message key="common.add"/>"
   			}, "addEditField");
   			add.on("click", function(){
   				var select = dojo.byId("newEditSelect");
   				var pointType = $get("newEditSelect");
   				domConstruct.create("div", {id:"edit_"+pointType}, "dialogContent", "last");
   				$("edit_"+pointType).innerHTML = '<span style="min-width: 200px;">'+select[pointType].innerHTML+
   					'</span><input id="edit_'+pointType+'_value" type="text" style="min-width:200px; margin-left:50px;"/><div id="edit_'+pointType+'_delete"/>';
   				var btn = new Button({label:"-", style:"background-color:#8b3c3c;",
   					onClick:function(){
   						dojo.destroy("edit_"+pointType);
   						btn.destroyRecursive();
   					}}, "edit_"+pointType+"_delete");
   				
   				editMap[pointType] = function(){return $get("edit_"+pointType+"_value")};
   			});
   			
   			var cancel = new Button({
   				label:"<fmt:message key="common.cancel"/>"
   			}, "dialogCancel");
   			cancel.on("click", function(){
   				editDialog.hide()
   			});
   			
   			var submit = new Button({
   				label:"<fmt:message key="common.save"/>"
   			}, "dialogSubmit")
   			submit.on("click", function(){
   				submitEdits();
   				editDialog.hide()
   			});
   			editDialog.show();
    	});
    }
    
    function submitEdits() {
    	massEdit = {};
    	for(key in editMap)
    		massEdit[key] = editMap[key]();
    	pointList = [];
    	for(k in tree.selectedItems) {
    		item = tree.selectedItems[k];
    		if("point" in item)
    			pointList.push(item.point[0].id);
    	}
    	PointHierarchyDwr.saveEdits(pointList, massEdit, function() {
    		PointHierarchyDwr.getPointHierarchy(function(rootFolder) {
                tree.destroy();
                initCB(rootFolder);
            });
    	});
    }
    
    
  </script>
  
  <table>
    <tr>
      <td valign="top">
        <div class="borderDivPadded">
          <table class="wide">
            <tr>
              <td>
                <span class="smallTitle"><fmt:message key="pointHierarchy.hierarchy"/></span>
                <tag:help id="pointHierarchy"/>
              </td>
              <td align="right">
                <tag:img png="folder_add" title="common.add" onclick="newFolder()"/>
                <tag:img id="saveBtn" png="save" title="common.save" onclick="save()"/>
                <tag:img id="editBtn" png="pencil" title="common.edit" onclick="openEditor()"/>
              </td>
            </tr>
            <tr><td class="formError" id="errorMessage"></td></tr>
          </table>
        
          <tag:img png="hourglass" id="loadingImg"/>
          <div id="treeDiv"></div>
        </div>
      </td>
      
      <td valign="top">
        <div id="folderEditDiv" class="borderDivPadded" style="display:none; position:absolute;">
          <table class="wide">
            <tr>
              <td class="smallTitle"><fmt:message key="pointHierarchy.details"/></td>
              <td align="right">
                <tag:img id="deleteImg" png="delete" title="common.delete" onclick="deleteFolder();"/>
                <tag:img id="saveImg" png="save" title="common.save" onclick="saveFolder();"/>
              </td>
            </tr>
          </table>
          
          <table>
            <tr>
              <td class="formLabelRequired"><fmt:message key="pointHierarchy.name"/></td>
              <td class="formField"><input id="folderName" type="text"
                      onkeypress="if (event.keyCode==13) $('saveImg').onclick();"/></td>
            </tr>
          </table>
        </div>
      </td>
    </tr>
  </table>
  <div style="display:none;"><tag:exportCodesOptions id="newEditSelect" optionList="<%= DataPointVO.PROPERTY_CODES.getIdKeys() %>"/></div>
</jsp:body>
</tag:page>