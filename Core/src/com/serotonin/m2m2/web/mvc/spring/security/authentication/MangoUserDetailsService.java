/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * Class for plug-in User Access for Authentication Data
 * 
 * @author Terry Packer
 *
 */
@Component
public class MangoUserDetailsService implements UserDetailsService {

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
	 */
	@Override
	public User loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = UserDao.instance.getUser(username);
		if (user == null)
		    throw new UsernameNotFoundException(username);
		else
			return user;
	}

	/**
	 * Create a set of Granted Authorities by parsing a permissions string
	 * @param permissionsString
	 * @return
	 */
	public static Set<GrantedAuthority> getGrantedAuthorities(String permissionsString) {
	    if (permissionsString == null) {
	        return Collections.emptySet();
	    }
	    
	    Set<String> permissions = Permissions.explodePermissionGroups(permissionsString);
        Set<GrantedAuthority> grantedAuthorities = new HashSet<GrantedAuthority>(permissions.size());
        
        for (String permission : permissions) {
        	if(SuperadminPermissionDefinition.GROUP_NAME.equals(permission))
        		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        	else
            	grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + permission.toUpperCase(Locale.ROOT)));
        }
        
        return grantedAuthorities;
	}
}
