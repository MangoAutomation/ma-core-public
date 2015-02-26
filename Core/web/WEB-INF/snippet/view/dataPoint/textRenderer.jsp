<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.DataTypes"%>
<div>
  <table>
    <tr><td colspan="3">
      <span class="smallTitle"><fmt:message key="pointEdit.text.props"/></span>
      <tag:help id="textRenderers"/>
    </td></tr>
    
    <tr>
      <td class="formLabelRequired"><fmt:message key="pointEdit.text.type"/></td>
      <td class="formField">
        <input id="textRendererSelect" />
      </td>
    </tr>
    
    <tbody id="suffixRow">
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.text.suffix"/></td>
        <td class="formField">
            <input id="useUnitAsSuffix" data-dojo-type="dijit/form/CheckBox" type="checkbox" />
            <label for="useUnitAsSuffix"><fmt:message key="pointEdit.props.useUnitAsSuffix"/></label>
        </td>
      </tr>
      <tr>
        <td></td>
        <td class="formField"><input id="suffix" type="text"/></td>
      </tr>
    </tbody>
    <tbody id="formatRow" style="display:none;">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.text.format"/></td>
        <td class="formField">
          <input id="format" type="text"/>
          <div id="datetimeFormatHelpDiv"><tag:help id="datetimeFormats"/></div>
        </td>
      </tr>
     </tbody>
    <tbody id="conversionExponentRow">
       <tr>
        <td class="formLabel"><fmt:message key="pointEdit.text.conversionExponent"/></td>
        <td class="formField"><input id="conversionExponent" type="text"/></td>
      </tr>
    </tbody>
    
      <tbody id="binaryValuesRow" style="display:none;">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.text.zero"/></td>
        <td class="formField">
          <table cellspacing="0" cellpadding="0">
            <tr>
              <td valign="top"><input id="zeroLabel" type="text"/></td>
              <td width="10"></td>
              <td valign="top" align="center" id="zeroColourInputRow">
                <div dojoType="dijit.ColorPalette" palette="3x4" id="zeroColour"></div>
                <a href="#" onclick="textRendererEditor.handlerBinaryZeroColour(null); return false;">(<fmt:message key="pointEdit.text.default"/>)</a>
              </td>
            </tr>
          </table>
        </td>
      </tr>
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.text.one"/></td>
        <td class="formField">
          <table cellspacing="0" cellpadding="0">
            <tr>
              <td valign="top"><input id="oneLabel" type="text"/></td>
              <td width="10"></td>
              <td valign="top" align="center" id="oneColourInputRow">
                <div dojoType="dijit.ColorPalette" palette="3x4" id="oneColour"></div>
                <a id="oneColourDefault" href="#" onclick="textRendererEditor.handlerBinaryOneColour(null); return false;">(<fmt:message key="pointEdit.text.default"/>)</a>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </tbody>
    <tbody id="multistateValuesRow" style="display:none;">
      <tr>
        <td colspan="2">
          <table>
            <tr>
              <th><fmt:message key="pointEdit.text.key"/></th>
              <th><fmt:message key="pointEdit.text.text"/></th>
              <th><fmt:message key="pointEdit.text.colour"/></th>
              <td></td>
            </tr>
            <tr>
              <td valign="top"><input type="text" id="textRendererMultistateKey" value="" class="formVeryShort"/></td>
              <td valign="top"><input type="text" id="textRendererMultistateText" value="" class="formShort"/></td>
              <td valign="top" align="center">
                <div dojoType="dijit.ColorPalette" palette="3x4" id="textRendererMultistateColour"></div>
                <a href="#" onclick="textRendererEditor.handlerMultistateColour(null); return false;">(<fmt:message key="pointEdit.text.default"/>)</a>
              </td>
              <td valign="top">
                <tag:img id="multistateValueAdd" png="add" title="common.add" onclick="return textRendererEditor.addMultistateValue();"/>
              </td>
            </tr>
            <tbody id="textRendererMultistateTable"></tbody>
          </table>
        </td>
      </tr>
    </tbody>  
    
    
    
    <!--  I think we can delete this -->
    <tbody id="textRendererAnalog" style="display:none;">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.text.format"/></td>
        <td class="formField">
          <input id="textRendererAnalogFormat" type="text"/>
          <tag:help id="numberFormats"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.text.suffix"/></td>
        <td class="formField"><input id="textRendererAnalogSuffix" type="text"/></td>
      </tr>
    </tbody>
    

    <tbody id="textRendererNone" style="display:none;">
    </tbody>
    <tbody id="textRendererPlain" style="display:none;">
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.text.suffix"/></td>
        <td class="formField"><input id="textRendererPlainSuffix" type="text"/></td>
      </tr>
    </tbody>
    <tbody id="rangeValuesRow" style="display:none;">
      <tr>
        <td colspan="2">
          <table id="rangeValues"> <!-- For contextual field -->
            <tr>
              <th><fmt:message key="pointEdit.text.from"/></th>
              <th><fmt:message key="pointEdit.text.to"/></th>
              <th><fmt:message key="pointEdit.text.text"/></th>
              <th><fmt:message key="pointEdit.text.colour"/></th>
              <td></td>
            </tr>
            <tr>
              <td valign="top"><input type="text" id="textRendererRangeFrom" value="" class="formVeryShort"/></td>
              <td valign="top"><input type="text" id="textRendererRangeTo" value="" class="formVeryShort"/></td>
              <td valign="top"><input type="text" id="textRendererRangeText" value=""/></td>
              <td valign="top" align="center">
                <div dojoType="dijit.ColorPalette" palette="3x4" id="textRendererRangeColour"></div>
                <a href="#" onclick="textRendererEditor.handlerRangeColour(null); return false;">(<fmt:message key="pointEdit.text.default"/>)</a>
              </td>
              <td valign="top">
                <tag:img id="addRangeRendererRange" png="add" title="common.add" onclick="return textRendererEditor.addRangeValue();"/>
              </td>
            </tr>
            <tbody id="textRendererRangeTable"></tbody>
          </table>
        </td>
      </tr>
    </tbody>
    <tbody id="textRendererTime" style="display:none;">
      <tr>
        <td class="formLabelRequired"><fmt:message key="pointEdit.text.format"/></td>
        <td class="formField">
          <input id="textRendererTimeFormat" type="text"/>
          <tag:help id="datetimeFormats"/>
        </td>
      </tr>
      <tr>
        <td class="formLabel"><fmt:message key="pointEdit.text.conversionExponent"/></td>
        <td class="formField"><input id="textRendererTimeConversionExponent" type="text"/></td>
      </tr>
    </tbody>
  </table>
</div>

<script type="text/javascript">
  dojo.require("dijit.ColorPalette");
  dojo.require("dijit.form.Select");
  
  /**
   * On Select change re-render view
   */
  function textRendererChanged(name,oldValue,value){
      if (value == "textRendererAnalog"){
    	  hide("datetimeFormatHelpDiv");
          show("suffixRow");
          show("formatRow");
          hide("rangeValuesRow");
          hide("conversionExponentRow");
          hide("multistateValuesRow");
          hide("binaryValuesRow");
      }else if (value == "textRendererBinary"){
    	  hide("datetimeFormatHelpDiv");
    	  show("binaryValuesRow");
    	  hide("suffixRow");
          hide("formatRow");
          hide("rangeValuesRow");
          hide("conversionExponentRow");
          hide("multistateValuesRow");
      }else if (value == "textRendererMultistate"){
    	  textRendererEditor.refreshMultistateList(); //Refresh the html table
    	  hide("datetimeFormatHelpDiv");
          hide("suffixRow");
          hide("formatRow");
          hide("rangeValuesRow");
          hide("conversionExponentRow");
          show("multistateValuesRow");
          hide("binaryValuesRow");
      }else if (value == "textRendererNone"){
    	  hide("datetimeFormatHelpDiv");
          hide("suffixRow");
          hide("formatRow");
          hide("rangeValuesRow");
          hide("conversionExponentRow");
          hide("multistateValuesRow");
          hide("binaryValuesRow");
      }else if (value == "textRendererPlain"){
    	  hide("datetimeFormatHelpDiv");
    	  show("suffixRow");
          hide("formatRow");
          hide("rangeValuesRow");
          hide("conversionExponentRow");
          hide("multistateValuesRow");
          hide("binaryValuesRow");
      }else if (value == "textRendererRange"){
    	  textRendererEditor.refreshRangeList(); //Refresh the html table
    	  hide("datetimeFormatHelpDiv");
    	  show("formatRow");
    	  hide("suffixRow");
    	  show("rangeValuesRow");
          hide("conversionExponentRow");
          hide("multistateValuesRow");
          hide("binaryValuesRow");
      }else if (value == "textRendererTime"){
    	  show("datetimeFormatHelpDiv");
          hide("suffixRow");
          show("formatRow");
          hide("rangeValuesRow");
          show("conversionExponentRow");
          hide("multistateValuesRow");
          hide("binaryValuesRow");
      }else{
          alert("Unknown text renderer: " + vo.textRenderer.typeName);
      }
  }
  
  
  
  /*
   * Set the page values from the current data point VO
   */
  function setTextRenderer(vo){
      
      DataPointDwr.getTextRendererOptions(vo.pointLocator.dataTypeId,function (response){
          var options = [];
          for(var i=0; i<response.data.options.length; i++){
              options.push({
                  label: mangoMsg[response.data.options[i].nameKey],
                  value: response.data.options[i].name,
              })
          }
          textRendererEditor.textRendererSelect.options = [];
          textRendererEditor.textRendererSelect.addOption(options);
          textRendererEditor.multistateValues = new Array(); //clear out
          textRendererEditor.rangeValues = new Array(); //clear out
    
        if(vo.textRenderer != null){
      	  textRendererEditor.textRendererSelect.set('value',vo.textRenderer.typeName);
      	  
            if (vo.textRenderer.typeName == "textRendererAnalog"){
          	  	dojo.byId("format").value = vo.textRenderer.format;
                dojo.byId("suffix").value = vo.textRenderer.suffix;
                dijit.byId("useUnitAsSuffix").set('checked',vo.textRenderer.useUnitAsSuffix);
            }else if (vo.textRenderer.typeName == "textRendererBinary"){
          	  dojo.byId("zeroLabel").value = vo.textRenderer.zeroLabel;
          	  textRendererEditor.handlerBinaryZeroColour( vo.textRenderer.zeroColour);
          	  dojo.byId("oneLabel").value = vo.textRenderer.oneLabel;
          	  textRendererEditor.handlerBinaryOneColour(vo.textRenderer.oneColour);
            }else if (vo.textRenderer.typeName == "textRendererMultistate"){
          	  textRendererEditor.setMultistateValues(vo.textRenderer.multistateValues);
            }else if (vo.textRenderer.typeName == "textRendererNone"){
          	  //Nothing
            }else if (vo.textRenderer.typeName == "textRendererPlain"){
          	  dojo.byId("suffix").value = vo.textRenderer.suffix;
          	  dijit.byId("useUnitAsSuffix").set('checked',vo.textRenderer.useUnitAsSuffix);
            }else if (vo.textRenderer.typeName == "textRendererRange"){
          	  dojo.byId("format").value = vo.textRenderer.format;
          	  textRendererEditor.setRangeValues(vo.textRenderer.rangeValues);
            }else if (vo.textRenderer.typeName == "textRendererTime"){
          	  dojo.byId("format").value = vo.textRenderer.format;
          	  dojo.byId("conversionExponent").value = vo.textRenderer.conversionExponent;
            }else{
                alert("Unknown text renderer: " + vo.textRenderer.typeName);
            }
        }//Not null
        });
      
  }

  /*
   * Get the values from the page and put into current data point VO
   */
  function getTextRenderer(vo){

	   var typeName = textRendererEditor.textRendererSelect.get('value');
	   
       if (typeName == "textRendererAnalog"){
     	   vo.textRenderer = new AnalogRenderer();
           vo.textRenderer.format = dojo.byId("format").value;
           vo.textRenderer.suffix = dojo.byId("suffix").value;
           vo.textRenderer.useUnitAsSuffix = dojo.byId("useUnitAsSuffix").checked;
       }else if (typeName == "textRendererBinary"){
     	  vo.textRenderer = new BinaryTextRenderer();
           vo.textRenderer.zeroLabel = dojo.byId("zeroLabel").value;
           vo.textRenderer.zeroColour = dijit.byId("zeroColour").selectedColour;
           vo.textRenderer.oneLabel = dojo.byId("oneLabel").value;
           vo.textRenderer.oneColour = dijit.byId("oneColour").selectedColour;
       }else if (typeName == "textRendererMultistate"){
     	  vo.textRenderer = new MultistateRenderer();
     	  vo.textRenderer.multistateValues = textRendererEditor.multistateValues;
       }else if (typeName == "textRendererNone"){
           vo.textRenderer = new NoneRenderer();
       }else if (typeName == "textRendererPlain"){
     	  vo.textRenderer = new PlainRenderer();
           vo.textRenderer.suffix = dojo.byId("suffix").value;
           vo.textRenderer.useUnitAsSuffix = dojo.byId("useUnitAsSuffix").checked;
       }else if (typeName == "textRendererRange"){
     	  vo.textRenderer = new RangeRenderer();
           vo.textRenderer.format = dojo.byId("format").value;
           vo.textRenderer.rangeValues = textRendererEditor.rangeValues;
       }else if (typeName == "textRendererTime"){
     	  vo.textRenderer = new TimeRenderer();
           vo.textRenderer.format = dojo.byId("format").value;
           vo.textRenderer.conversionExponent = dojo.byId("conversionExponent").value;
       }else{
           alert("Unknown text renderer: " + vo.textRenderer.typeName);
       }
       //Set the type name
       vo.textRenderer.typeName = typeName;
  }
  
  /**
   * Reset the Text Renderer Input to the default for that data type
   */
  function resetTextRendererOptions(dataTypeId){
          //Change the renderer to the default for this data type
          var vo = {
        		 textRenderer: {
        			 typeName: 'textRendererNone',
        			 suffix: '',
            		 useUnitAsSuffix: false
            		 },
        		 pointLocator: {dataTypeId: dataTypeId},
        		 
        		 
          };
          switch(dataTypeId){
          	case <%= DataTypes.ALPHANUMERIC %>:
          	case <%= DataTypes.BINARY %>:
          	case <%= DataTypes.MULTISTATE %>:
          	case <%= DataTypes.NUMERIC %>:
          		vo.textRenderer.typeName = "textRendererPlain";
          	break;
          	case <%= DataTypes.IMAGE %>:
          		vo.textRenderer.typeName = "textRendererNone";
          	break;
          }
          setTextRenderer(vo);
     // });
  }
  //Register for callbacks when the data type is changed
  dataTypeChangedCallbacks.push(resetTextRendererOptions);

  /**
   * Main Editing Logic
   */
  function TextRendererEditor() {
	  this.textRendererSelect;
      var currentTextRenderer;
      
      this.multistateValues = new Array();
      this.rangeValues = new Array();
      
      this.init = function() {
          // Colour handler events
          dijit.byId("textRendererRangeColour").onChange = this.handlerRangeColour;
          dijit.byId("textRendererMultistateColour").onChange = this.handlerMultistateColour;
          dijit.byId("zeroColour").onChange = this.handlerBinaryZeroColour;
          dijit.byId("oneColour").onChange = this.handlerBinaryOneColour;
          
          this.textRendererSelect = new dijit.form.Select({
              name: 'textRendererSelect',
          },"textRendererSelect");
          
          this.textRendererSelect.watch("value",textRendererChanged);
          
          var useUnitAsSuffix = dijit.byId("useUnitAsSuffix");
          var suffix = dojo.byId("suffix");
          useUnitAsSuffix.watch('checked',function(value){
        	  if(useUnitAsSuffix.checked)
        		  hide("suffix");
        	  else
        		  show("suffix");
          });
      }
      
      this.disableInputs = function(){
    	  dijit.byId('textRendererSelect').set('disabled', true);
    	  dijit.byId('useUnitAsSuffix').set('disabled', true);
    	  setDisabled('suffix', true);
    	  setDisabled('format', true);
    	  setDisabled('conversionExponent', true);
    	  setDisabled('zeroLabel', true);
    	  hide('zeroColourInputRow');
    	  setDisabled('oneLabel', true);
    	  hide('oneColourInputRow');
    	  setDisabled('textRendererMultistateKey', true);
    	  setDisabled('textRendererMultistateText', true);
    	  hide('multistateValueAdd');
    	  setDisabled('textRendererAnalogFormat', true);
    	  setDisabled('textRendererAnalogSuffix', true);
    	  setDisabled('textRendererPlainSuffix', true);
    	  setDisabled('textRendererRangeFrom', true);
    	  setDisabled('textRendererRangeTo', true);
    	  setDisabled('textRendererRangeText', true);
    	  hide('addRangeRendererRange');
    	  setDisabled('textRendererTimeFormat', true);
    	  setDisabled('textRendererTimeConversionExponent', true);
      };
      
      this.enableInputs = function(){
    	  dijit.byId('textRendererSelect').set('disabled', false);
    	  dijit.byId('useUnitAsSuffix').set('disabled', false);
    	  setDisabled('suffix', false);
    	  setDisabled('format', false);
    	  setDisabled('conversionExponent', false);
    	  setDisabled('zeroLabel', false);
    	  show('zeroColourInputRow');
    	  setDisabled('oneLabel', false);
    	  show('oneColourInputRow');
    	  setDisabled('textRendererMultistateKey', false);
    	  setDisabled('textRendererMultistateText', false);
    	  show('multistateValueAdd');
    	  setDisabled('textRendererAnalogFormat', false);
    	  setDisabled('textRendererAnalogSuffix', false);
    	  setDisabled('textRendererPlainSuffix', false);
    	  setDisabled('textRendererRangeFrom', false);
    	  setDisabled('textRendererRangeTo', false);
    	  setDisabled('textRendererRangeText', false);
    	  show('addRangeRendererRange');
    	  setDisabled('textRendererTimeFormat', false);
    	  setDisabled('textRendererTimeConversionExponent', false);
      };
  
      this.change = function() {
          if (currentTextRenderer)
              hide($(currentTextRenderer));
          currentTextRenderer = $("textRendererSelect").value
          show($(currentTextRenderer));
      };
      
      //
      // List objects
      this.MultistateValue = function() {
          this.key;
          this.text;
          this.colour;
      };
      
      this.RangeValue = function() {
          this.from;
          this.to;
          this.text;
          this.colour;
      };
      
      
      /*
       * Create a new set of values from an existing vo's list
       */
      this.setMultistateValues = function(list){
    	  //Clear the list
    	  this.multistateValues = new Array();
    	  this.refreshMultistateList();
    	  for(var i=0; i<list.length; i++){
    		  this.addMultistateValue(list[i].key,list[i].text,list[i].colour);
    	  }
      }
      
      //
      // Multistate list manipulation
      this.addMultistateValue = function(theKey, text, colour) {
          if (!theKey)
              theKey = $get("textRendererMultistateKey");
          var theNumericKey = parseInt(theKey);
          if (isNaN(theNumericKey)) {
              alert("<fmt:message key='pointEdit.text.errorParsingKey'/>" + theKey);
              return false;
          }
          for (var i=this.multistateValues.length-1; i>=0; i--) {
              if (this.multistateValues[i].key == theNumericKey) {
                  alert("<fmt:message key='pointEdit.text.listContainsKey'/> "+ theNumericKey);
                  return false;
              }
          }
          
          var theValue = new this.MultistateValue();
          theValue.key = theNumericKey;
          if (text)
              theValue.text = text;
          else
              theValue.text = $get("textRendererMultistateText");
          if (colour)
              theValue.colour = colour;
          else
              theValue.colour = dijit.byId("textRendererMultistateColour").selectedColour;
          this.multistateValues[this.multistateValues.length] = theValue;
          this.sortMultistateValues();
          this.refreshMultistateList();
          $set("textRendererMultistateKey", theNumericKey+1);
          
          return false;
      };
      
      this.removeMultistateValue = function(theValue) {
          for (var i=this.multistateValues.length-1; i>=0; i--) {
              if (this.multistateValues[i].key == theValue)
                  this.multistateValues.splice(i, 1);
          }
          this.refreshMultistateList();
          return false;
      };
      
      this.sortMultistateValues = function() {
          this.multistateValues.sort( function(a,b) { return a.key-b.key; } );
      };
      
      this.refreshMultistateList = function() {
          dwr.util.removeAllRows("textRendererMultistateTable");
          dwr.util.addRows("textRendererMultistateTable", this.multistateValues, [
                  function(data) { return data.key; },
                  function(data) { 
                      if (data.colour)
                          return "<span style='color:"+ data.colour +"'>"+ data.text +"</span>";
                      return data.text;
                  },
                  function(data) {
                      return "<a href='#' onclick='return textRendererEditor.removeMultistateValue("+ data.key +
                             ");'><img src='images/bullet_delete.png' width='16' height='16' "+
                             "title='<fmt:message key="common.delete"/>'/><\/a>";
                  }
                  ], null);
      };
      
      /*
       * Set the range values from the vo's list
       */
      this.setRangeValues = function(list){
          //Clear the list
          this.rangeValues = new Array();
          this.refreshRangeList();
          for(var i=0; i<list.length; i++){
              this.addRangeValue(list[i].from,list[i].to,list[i].text,list[i].colour);
          }

      }
      
      
      //
      // Range list manipulation
      this.addRangeValue = function(theFrom, theTo, text, colour) {
          if (typeof theFrom === 'undefined')
              theFrom = parseFloat($get("textRendererRangeFrom"));
          if (isNaN(theFrom)) {
              alert("<fmt:message key='pointEdit.text.errorParsingFrom'/>");
              return false;
          }
          
          if (typeof theTo === 'undefined')
              theTo = parseFloat($get("textRendererRangeTo"));
          if (isNaN(theTo)) {
              alert("<fmt:message key='pointEdit.text.errorParsingTo'/>");
              return false;
          }
          
          if (isNaN(theTo >= theFrom)) {
              alert("<fmt:message key='pointEdit.text.toGreaterThanFrom'/>");
              return false;
          }
          
          for (var i=0; i<this.rangeValues.length; i++) {
              if (this.rangeValues[i].from == theFrom && this.rangeValues[i].to == theTo) {
                  alert("<fmt:message key='pointEdit.text.listContainsRange'/> "+ theFrom +" - "+ theTo);
                  return false;
              }
          }
          
          var theValue = new this.RangeValue();
          theValue.from = theFrom;
          theValue.to = theTo;
          if (text)
              theValue.text = text;
          else
              theValue.text = $get("textRendererRangeText");
          if (colour)
              theValue.colour = colour;
          else
              theValue.colour = dijit.byId("textRendererRangeColour").selectedColour;
          this.rangeValues[this.rangeValues.length] = theValue;
          this.sortRangeValues();
          this.refreshRangeList();
          $set("textRendererRangeFrom", theTo);
          $set("textRendererRangeTo", theTo + (theTo - theFrom));
          return false;
      };
      
      this.removeRangeValue = function(theFrom, theTo) {
          for (var i=this.rangeValues.length-1; i>=0; i--) {
              if (this.rangeValues[i].from == theFrom && this.rangeValues[i].to == theTo)
                  this.rangeValues.splice(i, 1);
          }
          this.refreshRangeList();
          return false;
      };
      
      this.sortRangeValues = function() {
          this.rangeValues.sort( function(a,b) {
              if (a.from == b.from)
                  return a.to-b.to;
              return a.from-b.from;
          });
      };
      
      this.refreshRangeList = function() {
          dwr.util.removeAllRows("textRendererRangeTable");
          dwr.util.addRows("textRendererRangeTable", this.rangeValues, [
                  function(data) { return data.from; },
                  function(data) { return data.to; },
                  function(data) { 
                      if (data.colour)
                          return "<span style='color:"+ data.colour +"'>"+ data.text +"</span>";
                      return data.text;
                  },
                  function(data) {
                      return "<a href='#' onclick='return textRendererEditor.removeRangeValue("+
                             data.from +","+ data.to +");'><img src='images/bullet_delete.png' width='16' "+
                             "height='16' title='<fmt:message key="common.delete"/>'/><\/a>";
                  }
                  ], null);
      };
      
      //
      // Color handling
      this.handlerRangeColour = function(colour) {
          dijit.byId("textRendererRangeColour").selectedColour = colour;
          $("textRendererRangeText").style.color = colour;
      };
      this.handlerMultistateColour = function(colour) {
          dijit.byId("textRendererMultistateColour").selectedColour = colour;
          $("textRendererMultistateText").style.color = colour;
      };
      this.handlerBinaryZeroColour = function(colour) {
          dijit.byId("zeroColour").selectedColour = colour;
          $("zeroLabel").style.color = colour;
      };
      this.handlerBinaryOneColour = function(colour) {
          dijit.byId("oneColour").selectedColour = colour;
          $("oneLabel").style.color = colour;
      };
  }
  var textRendererEditor = new TextRendererEditor();
  
  
</script>