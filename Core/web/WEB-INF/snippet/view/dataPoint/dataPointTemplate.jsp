<%--
    Copyright (C) 2015 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp"%>
<%@page import="com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateDefinition" %>
<style>
  .dgrid-loading{
      height: 100%;
      width: 100%;
      display: block;
      background: url('/images/throbber.gif') no-repeat 32px 32px;
      background-position: center;
  }
  .dgrid-no-data{
  	padding-top: 30px;
  }
  .baseOverlay{
    height: 100%;
    width: 100%;
    display: block;
  }
</style>
<div id="templateEditorTableBaseLoadingOverlay" class="baseOverlay">
	<div id="templateEditorTableLoadingOverlay" class="baseOverlay">
	<table id="templateEditorTable">
	  <tr>
	    <td colspan="2"><span class="smallTitle"><fmt:message
	          key="pointEdit.template.propertyTemplate" /></span> <tag:help
	        id="dataPointTemplate" /></td>
	  </tr>
	  <tr>
	    <td colspan="2">
	    <table>
	      <tbody id="pointPropertyTemplateMessages" />
	    </table></td>
	  </tr>
	  <tr>
	    <td class="formLabel"><fmt:message key="pointEdit.template.usePropertyTemplate" /></td>
	    <td class="formField"><input
	      data-dojo-type="dijit/form/CheckBox" id="usePointPropertyTemplate"
	      name="usePointPropertyTemplate" /> <input
	      id="pointPropertyTemplate" />
	      <button type="button" id='editDataPointTemplateButton'
	        onClick="editDataPointTemplate()">
	        <fmt:message key="pointEdit.template.editPropertyTemplate" />
	      </button>
	      <button type="button" id='cancelEditDataPointTemplateButton' style='display:none'
	        onClick="cancelEditDataPointTemplate()">
	        <fmt:message key="pointEdit.template.cancelEditPropertyTemplate" />
	      </button>
	      <button type="button" id='updateDataPointTemplateButton'
	        onClick="getPointsUpdatedByTemplate();" style="display:none">
	        <fmt:message key="pointEdit.template.updatePropertyTemplate" />
	      </button>
	      <button type="button" id='saveNewDataPointTemplateButton' style='display:none'
	        onClick="showNewTemplateDialog();">
	        <fmt:message key="pointEdit.template.saveNewPropertyTemplate" />
	      </button>
	    </td>
	  </tr>
	</table>
  </div>
</div>
<%-- Save New Dialog --%>
<div data-dojo-type="dijit.Dialog" id="newTemplateDialog"
  title="<fmt:message key='template.saveNew'/>" style="width: 300px">
  <div class="dijitDialogPaneContentArea">
    <label for='templateXid'>
      <fmt:message key="common.xid" />  
    </label><input id='templateXid'
      type='text' />
  </div>
  <div class="dijitDialogPaneActionBar">
    <button id='saveNewDataPointTemplateButton' onClick='saveNewDataPointTemplate()'>
      <fmt:message key="common.save" />
    </button>
    <button onClick="dijit.byId('newTemplateDialog').hide()">
      <fmt:message key="common.cancel" />
    </button>
  </div>
</div>
<%-- Update Dialog --%>
<div data-dojo-type="dijit.Dialog" id="updateTemplateDialog"
  title="<fmt:message key='template.update'/>" style="width: 400px">
  <div class="dijitDialogPaneContentArea">
    <span style="color:red">
      <fmt:message key="pointEdit.template.updateWarning" />
    </span>
    <br>
    <%-- Points To Update Table --%>
    <div id="templateChangeAffectsPointsListBaseLoadingOverlay" class="baseOverlay">
        <div id="templateChangeAffectsPointsListLoadingOverlay" class="baseOverlay">
          <div id="templateChangeAffectsPointsList"></div>
        </div>
      </div>
	
    <div style="width: 300px; margin: 5px auto">
      <label for='updateTemplateXid'>
        <fmt:message key="common.xid" />
      </label>
      <input id='updateTemplateXid'type='text' />
    </div>
  </div>
  <div class="dijitDialogPaneActionBar">
    <button id='confirmUpdateDataPointTemplateButton' onClick='updateDataPointTemplate()'>
      <fmt:message key="common.update" />
    </button>
    <button onClick="dijit.byId('updateTemplateDialog').hide()">
      <fmt:message key="common.cancel" />
    </button>
  </div>
</div>
<script type="text/javascript">
	dojo.require("dojo.dom");
	dojo.require("dojo.dom-style");
	dojo.require("dijit.Dialog");
	dojo.require("dojo.store.Memory");

	//Setup the All Points Grid
  	var updatedTemplatePointsGrid;
  	require(['dgrid/OnDemandGrid'], function(OnDemandGrid){
  		updatedTemplatePointsGrid = new OnDemandGrid({
  		   showHeader: true,
  	       store: new dojo.store.Memory(),
  	       loadingMessage: '<fmt:message key="common.loading"/>',
  	       noDataMessage: '<fmt:message key="common.noData"/>',
  	       columns: [
  	           {label: '<fmt:message key="dsEdit.deviceName"/>', field: 'deviceName'},
  	           {label: '<fmt:message key="common.name"/>', field: 'name'},
  	           {label: '<fmt:message key="common.xid"/>', field: 'xid'}
  	       ]
  	   	}, 'templateChangeAffectsPointsList');
  		//Dirty hack as the headers don't show (bug #1170 on github)
  		updatedTemplatePointsGrid.on('dgrid-refresh-complete',function(){
  			updatedTemplatePointsGrid.resize();
  			});
  		updatedTemplatePointsGrid.startup();
  	});	
	
	function showNewTemplateDialog() {
		dijit.byId('newTemplateDialog').show();
		$set('templateXid',"");
	}
	function showUpdateTemplateDialog() {
		var template = dataPointTemplatePicker.item;
		if(template != null)
			$set('updateTemplateXid', template.xid);
		dijit.byId('updateTemplateDialog').show();
	}
	//Global variables used on the page
	var NEW_ID = -1;
	var dataPointTemplatesList, dataPointTemplatePicker, newTemplateDialog, usePointPropertyTemplate, dataPointTemplateDataTypeId;
	var savedMessageArray = [ {
		genericMessage : '<fmt:message key="pointEdit.template.templateSaved"/>',
		level : 'error' //So message is RED like the other save messages
	} ];
	var templateNotSavedMessage = {
		genericMessage : '<fmt:message key="pointEdit.template.templateNotSaved"/>',
		level : 'error'
	}

	/**
	 * Initialize the Template Inputs
	 */
	function initDataPointTemplates() {

		//Create the store
		dataPointTemplatesList = new dojo.store.Memory({
			idProperty : "id",
			valueProperty : "xid",
			data : []
		});

		//Create the base unit input
		dataPointTemplatePicker = new dijit.form.ComboBox(
				{
					store : dataPointTemplatesList,
					autoComplete : false,
					style : "width: 250px;",
					name: "xid",
					searchAttr: "xid",
					queryExpr : "*\${0}*",
					highlightMatch : "all",
					required : false,
					placeHolder : '<fmt:message key="pointEdit.template.selectPropertyTemplate"/>',
					onChange : function(templateName) {
						if (dataPointTemplatePicker.item != null){
							loadFromDataPointTemplate(dataPointTemplatePicker.item);
    						//Disable all inputs each time because some of the 
    						// Settings areas re-enable inputs on load of new types
    						disableDataPointInputs();
						}
					}
				}, "pointPropertyTemplate");

		//Setup to watch the checkbox
		usePointPropertyTemplate = dijit.byId('usePointPropertyTemplate');
		usePointPropertyTemplate.watch('checked', function(value) {
			if (usePointPropertyTemplate.checked) {
				//Using a template
				enableTemplateControls();
				if (dataPointTemplatePicker.item != null){
					loadFromDataPointTemplate(dataPointTemplatePicker.item);
					//Disable all inputs each time because some of the 
					// Settings areas re-enable inputs on load of new types
					disableDataPointInputs();
				} else if(dataPointTemplatePicker.store.data.length > 0) {
					dataPointTemplatePicker.set("item", dataPointTemplatePicker.store.data[0]);
					loadFromDataPointTemplate(dataPointTemplatePicker.item);
					disableDataPointInputs();
				}
			} else {
				//Not Using Template
				disableTemplateControls();
				enableDataPointInputs();
			}
		});
	}

	/**
	 * Callback to know when the data type has changed
	 */
	function templateDataTypeChanged(newDataTypeId) {
		
		//Reload the template list with only available templates
		dataPointTemplateDataTypeId = newDataTypeId;
		TemplateDwr.getDataPointTemplates(newDataTypeId, function(response) {
			dataPointTemplatesList.setData(response.data.templates);
		});
		//If we are changing data types we must disable our use of a template
		usePointPropertyTemplate.set('checked', false);
		dataPointTemplatePicker.reset(); 
	}
	dataTypeChangedCallbacks.push(templateDataTypeChanged);

	/**
	 * Set the template from the DataPointVO
	 */
	function setDataPointTemplate(vo) {
		//Clear out my messages
		hideGenericMessages("pointPropertyTemplateMessages");
		//Save as reference
		dataPointTemplateDataTypeId = vo.pointLocator.dataTypeId;
		//Reload the template list with only available templates
		TemplateDwr.getDataPointTemplates(vo.pointLocator.dataTypeId, function(
				response) {
			dataPointTemplatesList.setData(response.data.templates);
			if (vo.templateId > 0) {
				enableTemplateControls();
				var template = dataPointTemplatesList.get(vo.templateId);
				//This order is important so that the first check doesn't load the template
				dataPointTemplatePicker.set('_onChangeActive', false);
				dataPointTemplatePicker.set('item', template);
				dataPointTemplatePicker.set('_onChangeActive', true);
				usePointPropertyTemplate.set('checked', true);
			} else {
				if(usePointPropertyTemplate.get('checked')){
					usePointPropertyTemplate.set('checked', false);
				}else{
					//Watch won't fire so just disable the template controls and enable inputs
					//Not Using Template
					disableTemplateControls();
					enableDataPointInputs();
				}
			}
		});

	}

	/**
	 * Add a template to the DataPointVO if necessary
	 */
	function getDataPointTemplate(vo) {
		if (usePointPropertyTemplate.get('checked') == true) {
			if (dataPointTemplatePicker.item != null)
				vo.templateId = dataPointTemplatePicker.item.id;
			else
				vo.templateId = null; //No template in use
		} else {
			vo.templateId = null; //No template in use
		}
	}

	/**
	 * Method to enable template controls
	 */
	 function enableTemplateControls(){
		/* Disable the picker */
		dataPointTemplatePicker.set('disabled', false);
		var updateTemplateButton = dojo.byId('updateDataPointTemplateButton');
		updateTemplateButton.disabled = false;
		var saveTemplateButton = dojo.byId('saveNewDataPointTemplateButton');
		saveTemplateButton.disabled = false;
		show('editDataPointTemplateButton');
		hide('cancelEditDataPointTemplateButton');
		hide('updateDataPointTemplateButton');
		hide('saveNewDataPointTemplateButton');
	}
	
	/**
	 * Method to enable template controls
	 */
	 function disableTemplateControls(){
		/* Disable the picker */
		dataPointTemplatePicker.set('disabled', true);
		var updateTemplateButton = dojo.byId('updateDataPointTemplateButton');
		updateTemplateButton.disabled = true;
		var saveTemplateButton = dojo.byId('saveNewDataPointTemplateButton');
		saveTemplateButton.disabled = true;
		hide('editDataPointTemplateButton');
		hide('cancelEditDataPointTemplateButton');
		hide('updateDataPointTemplateButton');
		hide('saveNewDataPointTemplateButton');
		dataPointTemplatePicker.reset(); 
	}
	
	/**
	 * Method to enable all data point inputs
	 **/
	function enableDataPointInputs() {
		/* Enable all inputs here */
		enablePointProperties(dataPointTemplateDataTypeId);
		enableLoggingProperties(dataPointTemplateDataTypeId);
		textRendererEditor.enableInputs(dataPointTemplateDataTypeId);
		chartRendererEditor.enableInputs(dataPointTemplateDataTypeId);
	}
	
	function disableDataPointInputs(){
		disablePointProperties(dataPointTemplateDataTypeId);
		disableLoggingProperties(dataPointTemplateDataTypeId);
		textRendererEditor.disableInputs(dataPointTemplateDataTypeId);
		chartRendererEditor.disableInputs(dataPointTemplateDataTypeId);
	}

	function editDataPointTemplate(){
		hide('editDataPointTemplateButton');
		show('cancelEditDataPointTemplateButton');
		show('updateDataPointTemplateButton');
		show('saveNewDataPointTemplateButton');
		enableDataPointInputs();
	}
	
	function cancelEditDataPointTemplate(){
		show('editDataPointTemplateButton');
		hide('cancelEditDataPointTemplateButton');
		hide('updateDataPointTemplateButton');
		hide('saveNewDataPointTemplateButton');
		if (dataPointTemplatePicker.item != null)
			loadFromDataPointTemplate(dataPointTemplatePicker.item);
		disableDataPointInputs();
	}
	
	/*
	 * Get a list of points updated by the template if 
	 * they were to save it.
	 */
	function getPointsUpdatedByTemplate(){
		var template = dataPointTemplatePicker.item;
		if (template != null) {
			//Check to see that something is selected
			showUpdateTemplateDialog();
			showLoading('templateChangeAffectsPointsList');
			TemplateDwr.findPointsWithTemplate(template, function(response){
				
				var allPointsStore = new dojo.store.Memory({
	       			idProperty: 'id',
	       			data: response.data.dataPoints
	       		});
				updatedTemplatePointsGrid.set('showHeader', true);
				updatedTemplatePointsGrid.set('store', allPointsStore);		  			
				hideLoading('templateChangeAffectsPointsList');
			});
		}
	}
	
	/**
	 * Method to save currently loaded template if it exists
	 **/
	function updateDataPointTemplate() {
		hideContextualMessages("pointDetails");
		//Close Popup
		dijit.byId('updateTemplateDialog').hide();
		//Get currently selected template
		var template = dataPointTemplatePicker.item;
		//Check to see that something is selected
		if (template != null) {
			//Set the name
			template.xid = $get('updateTemplateXid');
			//Load template info from inputs
			loadIntoDataPointTemplate(template);
			//Save the template
			//Start the loading gif again
			showLoading('templateEditorTable');
			TemplateDwr.saveDataPointTemplate(template, function(response) {
				if (response.hasMessages) {
					response.messages.push(templateNotSavedMessage);
					showTemplateMessages(response.messages,
							'pointPropertyTemplateMessages');
				} else {
					showTemplateMessages(savedMessageArray,
							'pointPropertyTemplateMessages');
					//TODO Show messages for each XID updated: xidsUpdated
					//TODO Show messages for each XID failed: failedXidMap<xid,why>
					show('editDataPointTemplateButton');
					hide('cancelEditDataPointTemplateButton');
					hide('updateDataPointTemplateButton');
					hide('saveNewDataPointTemplateButton');
					disableDataPointInputs();
					dataPointTemplatePicker.set('_onChangeActive', false);
					dataPointTemplatePicker.set('item', template);
					dataPointTemplatePicker.set('_onChangeActive', true);
				}
				hideLoading('templateEditorTable');
			});
		} else {
			//TODO this can happen when someone enters a name into the dropdown and there is no matching template
			var messages = [ templateNotSavedMessage ];
			showTemplateMessages(messages, 'pointPropertyTemplateMessages');
		}

	}

	/**
	 * Method to save currently loaded template if it exists
	 **/
	function saveNewDataPointTemplate() {
		hideContextualMessages("pointDetails");
		//Close Popup
		dijit.byId('newTemplateDialog').hide();

		//Get currently selected template
		//Could create a DWR Call to request a new template of this type
		// then access the module registry and create a new one and return it
		TemplateDwr.getNewDataPointTemplate(function(response){
			var template = response.data.vo;
			template.xid = $get('templateXid');
			//Load the values into the template from the input
			loadIntoDataPointTemplate(template);

			//Check to see that something is selected
			if (template != null) {
				TemplateDwr.saveDataPointTemplate(template, function(response) {
					if (response.hasMessages) {
						response.messages.push(templateNotSavedMessage);
						showTemplateMessages(response.messages,
								'pointPropertyTemplateMessages');
					} else {
						template.id = response.data.id;
						dataPointTemplatesList.put(template);
						//Select the template in the list
						dataPointTemplatePicker.set('item', template);
						showTemplateMessages(savedMessageArray,
								'pointPropertyTemplateMessages');
						show('editDataPointTemplateButton');
						hide('cancelEditDataPointTemplateButton');
						hide('updateDataPointTemplateButton');
						hide('saveNewDataPointTemplateButton');
						disableDataPointInputs();
					}
				});
			}
		});


	}

	/**
	 *  Load the input values from the page into the template
	 */
	function loadIntoDataPointTemplate(template) {

		//Set the Data Type ID
		template.dataTypeId = dataPointTemplateDataTypeId;

		//Not using units anymore getPointProperties(template);
		template.chartColour = dojo.byId("chartColour").value;
		template.plotType = dojo.byId("plotType").value;
		template.simplifyType = dojo.byId("simplifyType").value;
		template.simplifyTolerance = dojo.byId("simplifyTolerance").value;
		template.simplifyTarget = dojo.byId("simplifyTarget").value;
		template.rollup = dojo.byId("rollup").value;
		template.preventSetExtremeValues = dijit.byId("preventSetExtremeValues").get('checked');
		template.setExtremeLowLimit = dojo.byId("setExtremeLowLimit").value;
		template.setExtremeHighLimit = dojo.byId("setExtremeHighLimit").value;

		getLoggingProperties(template);

		getTextRenderer(template);
		getChartRenderer(template);

		//Delete back off the un-necessary properties
		delete template.unitString;
		delete template.renderedUnitString;
		delete template.integralUnitString;
		delete template.pointLocator;
	}

	/**
	 * Load the values from the template into the inputs on the page
	 */
	function loadFromDataPointTemplate(template) {

		//Some massaging because our members are slightly different to DataPointVO
		template.pointLocator = {
			dataTypeId : template.dataTypeId
		};
		
		//For the point properties (not using units anymore)
		dojo.byId("chartColour").value = template.chartColour;
		dojo.byId("plotType").value = template.plotType;
		dojo.byId("simplifyType").value = template.simplifyType;
		dojo.byId("simplifyTolerance").value = template.simplifyTolerance;
		dojo.byId("simplifyTarget").value = template.simplifyTarget;
		dojo.byId("rollup").value = template.rollup;
		
		setLoggingProperties(template);
		setTextRenderer(template);
		setChartRenderer(template);
	}

	function showTemplateMessages(/*ProcessResult.messages*/messages, /*tbody*/
			genericMessageNode) {
		var i, m, field, node, next;
		var genericMessages = new Array();
		for (i = 0; i < messages.length; i++) {
			m = messages[i];
			if (m.contextKey) {
				node = $(m.contextKey + "Ctxmsg");
				if (!node) {
					field = $(m.contextKey);
					if (field)
						node = createContextualMessageNode(field, m.contextKey);
					else {
						m.genericMessage = m.contextKey + " - "
								+ m.contextualMessage;
						genericMessages[genericMessages.length] = m;
					}
				}

				if (node) {
					node.innerHTML = m.contextualMessage;
					show(node);
				}
			} else
				genericMessages[genericMessages.length] = m;
		}

		if (genericMessages.length > 0) {
			if (!genericMessageNode) {
				for (i = 0; i < genericMessages.length; i++)
					alert(genericMessages[i].genericMessage);
			} else {
				genericMessageNode = getNodeIfString(genericMessageNode);
				if (genericMessageNode.tagName == "TBODY") {
					dwr.util.removeAllRows(genericMessageNode);
					dwr.util
							.addRows(
									genericMessageNode,
									genericMessages,
									[ function(data) {
										return data.genericMessage;
									} ],
									{
										cellCreator : function(options) {
											var td = document
													.createElement("td");
											if (options.rowData.level == 'error')
												td.className = "formError";
											else if (options.rowData.level == 'warning')
												td.className = "formWarning";
											else if (options.rowData.level == 'info')
												td.className = "formInfo";
											return td;
										}
									});
					show(genericMessageNode);
				} else if (genericMessageNode.tagName == "DIV"
						|| genericMessageNode.tagName == "SPAN") {
					var content = "";
					for (var i = 0; i < genericMessages.length; i++)
						content += genericMessages[i].genericMessage + "<br/>";
					genericMessageNode.innerHTML = content;
				}
			}
		}
	}
	
  	/**
  	 * Show Loading Graphic
  	 */
  	function showLoading(id){
  		require(["dojo/dom", "dojo/dom-style",], function(dom, style){
  		var loading = dom.byId(id + 'LoadingOverlay');
  		if(loading !== null){
  			style.set(loading, {
	  				'z-index': '1002',
	  				background: "url('/images/throbber.gif') no-repeat 32px 32px",
	  				'background-position': 'center'
  			});
  		}
  		
  		loading = dom.byId(id + 'BaseLoadingOverlay');
  		if(loading !== null){
  	  		style.set(loading, {
  	  				'z-index': '1001',
  	  				opacity: '.5'
  	  			});
  	  	}
  		});
  	}

  	/**
  	 * Hide Loading Graphic
  	 */
  	function hideLoading(id){
  		require(["dojo/dom", "dojo/dom-style",], function(dom, style){
  		var loading = dom.byId(id + 'LoadingOverlay');
  		if(loading !== null){
  			style.set(loading, {
	  				'z-index': '-1',
	  				background: ''
	  			});
  		}
  		
  		loading = dom.byId(id + 'BaseLoadingOverlay');
  		if(loading !== null){
  			style.set(loading, {
	  				'z-index': '-1',
	  				opacity: ''
	  			});
  		}
  		});
  	}
	
</script>

