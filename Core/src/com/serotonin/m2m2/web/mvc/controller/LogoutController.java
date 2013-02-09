/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.filter.LoginPageProvider;
import com.serotonin.provider.Providers;

public class LogoutController extends AbstractController {
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
        // Check if the user is logged in.
        User user = Common.getUser(request);
        if (user != null) {
            // The user is in fact logged in. Invalidate the session.
            request.getSession().invalidate();

            // Notify any authentication modules of the logout.
            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class))
                def.logout(request, response, user);
        }

        // Regardless of what happened above, forward to the configured view.
        String uri = Providers.get(LoginPageProvider.class).getForwardUri();
        return new ModelAndView(new RedirectView(uri));
    }
}
