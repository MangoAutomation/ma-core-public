<#--
    Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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

<p>Reset link and token will expire at ${expiration?datetime} ${expiration?string["z"]}.</p>

<#include "include/footer.ftl">
