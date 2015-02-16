<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<%@page import="com.serotonin.m2m2.module.ModuleRegistry"%>
<%@page import="com.serotonin.m2m2.module.MenuItemDefinition"%>

 <tag:mobile-page>
     <div id="icons" dojoType="dojox.mobile.View">
         <div id="iconsMain">
             <ul dojoType="dojox.mobile.IconContainer" defaultIcon="/images/cog.png">
                 <c:if test="${!empty sessionUser}">
                   <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.USER) %>" var="mi">
                     <m2m2:mobile-menuItem def="${mi}"/>
                   </c:forEach>
                         
                   <c:if test="${sessionUser.dataSourcePermission}">
                     <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.DATA_SOURCE) %>" var="mi">
                       <m2m2:mobile-menuItem def="${mi}"/>
                     </c:forEach>
                   </c:if>
                   
                   <m2m2:mobile-menuItem id="usersMi" href="/users.shtm" png="user" key="header.users"/>
                   
                   <c:if test="${sessionUser.admin}">
                     <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ADMINISTRATOR) %>" var="mi">
                       <m2m2:mobile-menuItem def="${mi}"/>
                     </c:forEach>
                   </c:if>
                   
                   <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" var="mi">
                     <m2m2:mobile-menuItem def="${mi}"/>
                   </c:forEach>
                 </c:if>
                 <c:if test="${empty sessionUser}">
                   <m2m2:mobile-menuItem id="loginMi" href="/login.htm" png="control_play_blue" key="header.login"/>
                   <c:forEach items="<%= ModuleRegistry.getMenuItems().get(MenuItemDefinition.Visibility.ANONYMOUS) %>" var="mi">
                     <m2m2:mobile-menuItem def="${mi}"/>
                   </c:forEach>
                 </c:if>
           
             
             </ul>
         </div>
     </div>

 <script type="text/javascript">
         require(["dojox/mobile", "dojox/mobile/parser", "dojo/ready", 
             "mango/mobile/DataSource", "dojox/mobile/compat", "dojox/mobile/Button",
             "dojox/mobile/View", "dojox/mobile/IconContainer", "dojox/mobile/ScrollableView",
             "dojox/mobile/IconItem",
             "dojo/domReady!"], function(mobile, parser, ready, DataSource){
                 ready(function(){
                     DataSource.init();
                 });
             });
 </script>
 
 
 </tag:mobile-page>