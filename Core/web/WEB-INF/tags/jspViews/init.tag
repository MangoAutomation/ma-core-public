<%--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
--%><%@include file="/WEB-INF/tags/decl.tagf"%><%--
--%><%@taglib prefix="jviews" uri="/modules/jspViews/web/jviews.tld" %><%--
--%><%@tag body-content="empty"%><%--
--%><%@attribute name="username" required="true"%>
<c:set var="modulePath" value="/modules/jspViews"/>
<!-- <c:set var="dojoURI">http://ajax.googleapis.com/ajax/libs/dojo/1.7.2/</c:set> -->
<script type="text/javascript" src="${dojoURI}/dojo/dojo.js" data-dojo-config="async: false, parseOnLoad: true, isDebug:true, extraLocale: ['en-us', 'nl', 'nl-nl', 'ja-jp', 'fi-fi', 'sv-se', 'zh-cn', 'zh-tw','xx']"></script>
<tag:versionedJavascript  src="/dwr/engine.js" />
<tag:versionedJavascript  src="/dwr/util.js" />
<tag:versionedJavascript  src="/resources/common.js" />
<tag:versionedJavascript  src="/dwr/interface/MiscDwr.js" />
<tag:versionedJavascript  src="/dwr/interface/JspViewDwr.js" />
<tag:versionedJavascript  src="/resources/view.js" />
<tag:versionedJavascript  src="${modulePath}/web/jviews.js" />
<jviews:viewInit username="${username}"/>
<script type="text/javascript">
  dwr.util.setEscapeHtml(false);
  mango.view.initJspView();
  dojo.ready(mango.longPoll.start);
  
  function setPoint(xid, value, callback) {
      mango.view.setPoint(xid, value, callback);
  }
</script>
