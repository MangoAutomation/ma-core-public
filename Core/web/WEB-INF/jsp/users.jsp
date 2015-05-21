<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.Common"%>

<tag:html5 showHeader="${param.showHeader}" showToolbar="${param.showToolbar}" >
  <jsp:attribute name="styles">
    <style>
    .permissionStr { display: block; }
    .notifyjs-mangoErrorMessage-base {
      white-space: normal;
     }
    </style>
    <link rel="stylesheet" href="/resources/dijit/themes/claro/claro.css"/>
    <link rel="stylesheet" type="text/css" href="resources/dojo/resources/dojo.css"/>
    <tag:versionedCss href="/resources/common.css"/>
    <link rel="stylesheet" href="/resources/dgrid-0.4/css/dgrid.css"/>
  </jsp:attribute>


  <jsp:attribute name="scripts">
    <m2m2:moduleExists name="mangoApi">

    <script type="text/javascript">
      require.config({
          map: {'*': {'mango': 'mango-2.0'}}
      });
      var tr,mangoAPI;
      
      
      var currentUser = {};
      currentUser['username'] = '${sessionUser.username}'; 
      currentUser['admin'] = ${sessionUser.admin};
      
      
      //TODO probably remove this later
      var USER_DATA_SAVED_MESSAGE = '<fmt:message key="users.dataSaved"/>';
      var USER_ADDED_MESSAGE = '<fmt:message key="users.added"/>';
      var USERS_SAVED = '<fmt:message key="users.saved"/>';
      var NEW_ID = <c:out value="<%= Common.NEW_ID %>"/>;
      var NO_NEW_PERMISSIONS_CONTENT = '<m2m2:translate key="users.permissions.nothingNew" escapeDQuotes="true"/>';
      var CONFIRM_DELETE_USER = '<m2m2:translate key="users.deleteConfirm" escapeDQuotes="true"/>';
      
	 
	  
      require(['jquery', 'mango/api', 'dijit/layout/TabContainer', 'dijit/layout/ContentPane', 'view/users/Users'], 
    		  function($, MangoAPI, TabContainer, ContentPane, UsersView){
    	  
    	  // add a dojo dijit style class to body
    	  $('body').addClass('claro');
    	  
    	  mangoAPI = MangoAPI.defaultApi;
          var translationNamespaces = ['common', 'users', 'validate'];
    	  
    	  //Load in the required data
    	  $.when(mangoAPI.setupGlobalize.apply(mangoAPI, translationNamespaces)).then(MangoAPI.firstArrayArg).done(function(Globalize){
    		tr = Globalize.formatMessage.bind(Globalize);
    		$(document).ready(setupPage);
    	  }).fail(MangoAPI.logError);
    	  
    	  /**
    	   * Create the tab container and tabs
    	   */
    	  function setupPage(){
        	var tc = new TabContainer({
      			  style: "width: 100%; height: 100vh;"
      	  	},'tab-container');
      	  
      	  	var userTab = new ContentPane({
      			title: '<span class="smallTitle"><fmt:message key="users.title" /></span>',
      			style: "width: 100%; height: 100vh",
      		 	selected: true
      	  	}, 'user-tab');
      	  	tc.addChild(userTab);
      	  	//Fill out the tab
      	  	var usersView = new UsersView();
      	  	
      	  	usersView.loadUser('${sessionUser.username}');

      	  	<%-- Add permissions tab if we are admin --%>
      	    <c:if test="${sessionUser.admin}">
      	  	var permissionTab = new ContentPane({
      			title: '<span class="smallTitle"><fmt:message key="users.permissions"/></span>'
      	  	}, 'permission-tab');
      	  	tc.addChild(permissionTab);
      	  	</c:if>
      	  	
      	  	tc.startup();
      	  	$('#tab-container').show();
    	  }
    	  
    	  
      });
      
    </script>
    <tag:versionedJavascript src="/resources/common.js"/>
    <script type="text/javascript">
      mango.i18n = <sst:convert obj="${clientSideMessages}"/>;
    </script>
    <tag:versionedJavascript src="/dwr/engine.js"/>
    <tag:versionedJavascript src="/dwr/util.js"/>
    <tag:versionedJavascript src="/dwr/interface/MiscDwr.js"/>
    <tag:versionedJavascript src="/dwr/interface/UsersDwr.js"/>
    
    <tag:versionedJavascript src="/resources/view/users/permissions.js"/>
    </m2m2:moduleExists>
  </jsp:attribute>

<jsp:body>
<%-- Claro class for dojo widgets --%>
<div>
  <m2m2:moduleExists name="mangoApi">
  <div style="width: 100%; height: 100vh">
    <div id="tab-container" style="display: none">
        <div id="user-tab">
            <jsp:include page="/WEB-INF/snippet/view/users/users.jsp"/>
        </div>
        <div id="permission-tab">
          <jsp:include page="/WEB-INF/snippet/view/users/dataPointPermissionsTable.jsp"/>
        </div>           
    </div>
  </div>
  </m2m2:moduleExists>
  <m2m2:moduleDoesNotExist name="mangoApi">
    <h1>Required Module MangoApi is not installed.</h1>
  </m2m2:moduleDoesNotExist>
</div>
</jsp:body>
</tag:html5>
