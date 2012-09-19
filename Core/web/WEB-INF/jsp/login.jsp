<%--
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>

<c:if test='${m2m2:envBoolean("ssl.on", false)}'>
  <c:if test='${pageContext.request.scheme == "http"}'>
    <c:redirect url='https://${pageContext.request.serverName}:${m2m2:envString("ssl.port", "8443")}${requestScope["javax.servlet.forward.request_uri"]}'/>
  </c:if>
</c:if>

<tag:page>
  <script type="text/javascript">
    dojo.ready(function () {
        $("username").focus();
        BrowserDetect.init();
        
        $set("browser", BrowserDetect.browser +" "+ BrowserDetect.version +" <fmt:message key="login.browserOnPlatform"/> "+ BrowserDetect.OS);
        
        if (checkCombo(BrowserDetect.browser, BrowserDetect.version, BrowserDetect.OS)) {
            $("browserImg").src = "images/accept.png";
            show("okMsg");
        }
        else {
            $("browserImg").src = "images/thumb_down.png";
            show("warnMsg");
        }
    });
  </script>
  <table cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td>
        <form id="loginForm" action="login.htm" method="post">
          <table>
            <spring:bind path="login.username">
              <tr>
                <td class="formLabelRequired"><fmt:message key="login.userId"/></td>
                <td class="formField">
                  <input id="username" type="text" name="username" value="${status.value}" maxlength="40"/>
                </td>
                <td class="formError">${status.errorMessage}</td>
              </tr>
            </spring:bind>
            
            <spring:bind path="login.password">
              <tr>
                <td class="formLabelRequired"><fmt:message key="login.password"/></td>
                <td class="formField">
                  <input id="password" type="password" name="password" value="${status.value}" maxlength="20"/>
                </td>
                <td class="formError">${status.errorMessage}</td>
              </tr>
            </spring:bind>
                
            <spring:bind path="login">
              <c:if test="${status.error}">
                <td colspan="3" class="formError">
                  <c:forEach items="${status.errorMessages}" var="error">
                    <c:out value="${error}"/><br/>
                  </c:forEach>
                </td>
              </c:if>
            </spring:bind>
            
            <tr>
              <td colspan="2" align="center">
                <input type="submit" value="<fmt:message key="login.loginButton"/>"/>
                <tag:help id="welcomeToMango"/>
              </td>
              <td></td>
            </tr>
          </table>
        </form>
        <br/>
      </td>
      <td valign="top">
        <table>
          <tr>
            <td valign="top"><img id="browserImg" src="images/magnifier.png"/></td>
            <td><b><span id="browser"><fmt:message key="login.unknownBrowser"/></span></b></td>
          </tr>
          <tr>
            <td valign="top" colspan="2">
              <span id="okMsg" style="display:none"><fmt:message key="login.supportedBrowser"/></span>
              <span id="warnMsg" style="display:none"><fmt:message key="login.unsupportedBrowser"/></span>
            </td>
          </tr>
        </table>
        <br/><br/>
      </td>
    </tr>
  </table>
</tag:page>
