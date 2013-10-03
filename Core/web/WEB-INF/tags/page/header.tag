<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%>
<%@tag import="com.serotonin.m2m2.Common"%>
<%@attribute name="showHeader" %>

    
  
<div id="mainHeader" data-dojo-type="dijit/layout/BorderContainer" style="width:100%; height: 85px" >
     <div style="border:0px; padding:0px" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="region:'leading'" ><img src="<%= Common.applicationLogo %>" alt="Logo"/></div>
     
     <c:if test="${!simple}">
     <div id="alarmToaster" style="border:0px; padding:.2em 0em 0em 5em;" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="region:'center'">
        <div style="width:100%; height:100%"></div>
     </div>
     </c:if>
     
     <div style="border:0px; padding:0px;" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="region:'trailing'">
        <c:if test="${!empty instanceDescription}">
        <div style="position: relative; width: 100%; height: 100%">
 	    <span style="position: absolute; bottom:0px; right:0px; white-space:nowrap;" class="smallTitle">${instanceDescription}</span>
 	    </div>
        </c:if>
     </div>
<!-- Could put toolbar here later     <div data-dojo-type="dijit/layout/ContentPane" data-dojo-props="region:'bottom'">Bottom pane</div> -->
</div>


  <script type="text/javascript">
  require(["dojo/parser", "dijit/registry", "dojo/on", "dojo/topic", "dojo/dom-construct", "dojo/dom", "dijit/layout/BorderContainer", "dijit/layout/ContentPane", "dojo/domReady!"],
            function(parser,registry, on, topic, domConstruct, dom) {
	    
       // Register the alerting routine with the "alertUser" topic.
       topic.subscribe("alarmTopic", function(message){
           //Message has members:
           // duration - int
           // message - string
           // type - string
           var alarmMessageDiv = dojo.byId("alarmToaster");
           if(message.type == 'clear')
               alarmMessageDiv.innerHTML = "";
           else{
               alarmMessageDiv.innerHTML += message.message + "</br>";
           }   
       });
    
   });
  </script>
  