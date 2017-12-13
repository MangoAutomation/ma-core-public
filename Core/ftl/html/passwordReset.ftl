<#--
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
-->
<#include "include/header.ftl">

<div><span class="copySmall"><@fmt key="ftl.automatedEmail"/></span></div>

<p><@fmt key="ftl.passwordReset.body"/></p>

<p><a href="${resetUri}"><@fmt key="ftl.passwordReset.resetYourPassword"/></a></p>

<p>
<span><@fmt key="ftl.passwordReset.passwordResetToken"/></span>
<pre>${token}</pre>
</p>

<#include "include/footer.ftl">
