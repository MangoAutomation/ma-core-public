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
	 
	  
      require(['jquery', 'mango/api', 'dijit/layout/TabContainer', 'dijit/layout/ContentPane', 'view/users/UsersView', 'view/users/DataPointPermissionsView'], 
    		  function($, MangoAPI, TabContainer, ContentPane, UsersView, DataPointPermissionsView){
    	  
    	  // add a dojo dijit style class to body
    	  $('body').addClass('claro');
    	  
    	  mangoAPI = MangoAPI.defaultApi;
          var translationNamespaces = ['common', 'users', 'validate', 'js', 'filter', 'permissions'];
    	  
    	  $(document).ready(setupPage);
    	  
    	  /**
    	   * Create the tab container and tabs
    	   */
    	  function setupPage(){
          	var tc = new TabContainer({
    			  style: "width: 100%; height: 1000px"
    	  	},'tab-container');
    	  
    	  	var userTab = new ContentPane({
    			title: '<span class="smallTitle"><fmt:message key="users.title" /></span>',
    			style: "width: 100%; height: 1000px",
    		 	selected: true
    	  	}, 'user-tab');
    	  	tc.addChild(userTab);

      	  	<%-- Add permissions tab if we are admin --%>
      	    <c:if test="${sessionUser.admin}">
      	  	var permissionTab = new ContentPane({
      			title: '<span class="smallTitle"><fmt:message key="permissions.dataPoint"/></span>',
      	  	}, 'permission-tab');
      	  	tc.addChild(permissionTab);
      	  	</c:if>
    	  	
      	  	//Fill out the tab
      	  	var usersView = new UsersView(
      	  			{	translationNamespaces: translationNamespaces,
      	  				componentReady: function(){
      	        	  		usersView.setupView();
      	            	  	usersView.loadUser('${sessionUser.username}');

      	            	  	<c:if test="${sessionUser.admin}">
      	            	  	var permissionsView = new DataPointPermissionsView();
      	            	  	permissionsView.tr = usersView.tr;
      	            	  	permissionsView.setupView();
      	            	  	</c:if>
      	            	  	
      	            	  	tc.startup();
      	            	  	$('#tab-container').show();
      	  				}
      	  			});
    	  }
      });
      
    </script>
    </m2m2:moduleExists>
  </jsp:attribute>

<jsp:body>
<div>
  <m2m2:moduleExists name="mangoApi">
  <div style="width: 100%; height: 80vh">
    <div id="tab-container" style="display: none">
        <div id="user-tab">
            <jsp:include page="/WEB-INF/snippet/view/users/users.jsp"/>
        </div>
        <c:if test="${sessionUser.admin}">
        <div id="permission-tab">
          <jsp:include page="/WEB-INF/snippet/view/users/dataPointPermissionsTable.jsp"/>
        </div>
        </c:if>           
    </div>
  </div>
  </m2m2:moduleExists>
  <m2m2:moduleDoesNotExist name="mangoApi">
    <h1>Required Module MangoApi is not installed.</h1>
  </m2m2:moduleDoesNotExist>
</div>
</jsp:body>
</tag:html5>
