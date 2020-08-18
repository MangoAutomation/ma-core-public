/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Container for a set of roles that apply to a permission such as 'permissionDatasource' or  'permissions.user.editSelf'
 *   or a specific VO permission such as data point set permission.
 *
 *  @author Terry Packer
 *
 */
public class MangoPermission {

    protected Integer id;

    @JsonProperty
    protected final Set<Set<Role>> roles;

    public MangoPermission(Integer id) {
        this(Collections.emptySet());
        this.id = id;
    }

    public MangoPermission() {
        this(Collections.emptySet());
    }

    public MangoPermission(Set<Set<Role>> roles) {
        //TODO Mango 4.0 make inner sets unmodifiable or enforce that they must be
        this.roles = Collections.unmodifiableSet(roles);
    }

    public Set<Set<Role>> getRoles() {
        return roles;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This computes the hash on roles alone
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        return result;
    }

    /**
     * This assumes equality based on roles alone
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MangoPermission other = (MangoPermission) obj;

        if (roles == null) {
            if (other.roles != null)
                return false;
        } else {
            if(other.roles == null) {
                return false;
            }else {
                //check to see if they have the same terms
                for(Set<Role> roleSet : roles) {
                    int found = 0;
                    for(Set<Role> otherRoleSet : other.roles) {
                        for(Role role : roleSet) {
                            if(otherRoleSet.contains(role)) {
                                found++;
                            }
                        }
                    }
                    if(found != roleSet.size()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    /**
     * When a role is deleted from the system it must be removed from any RT
     * @param role
     * @return
     */
    public MangoPermission removeRole(Role role) {
        Set<Set<Role>> newOuterRoles = new HashSet<>();
        for(Set<Role> roleSet : roles) {
            Set<Role> newInnerRoles = new HashSet<>(roleSet);
            newInnerRoles.remove(role);
            newOuterRoles.add(Collections.unmodifiableSet(newInnerRoles));
        }
        return new MangoPermission(newOuterRoles);
    }

    /**
     * Is this role contained in this permission
     * @param role
     * @return
     */
    public boolean containsRole(Role role) {
        for(Set<Role> roleSet : roles) {
            if(roleSet.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a set of unique roles for this permission
     * @return
     */
    public Set<Role> getUniqueRoles() {
        Set<Role> unique = new HashSet<>();
        for(Set<Role> roleSet : roles) {
            unique.addAll(roleSet);
        }
        return Collections.unmodifiableSet(unique);
    }

    public static MangoPermission requireAnyRole(Role ...roles) {
        return requireAnyRole(Arrays.stream(roles));
    }

    public static MangoPermission requireAnyRole(Set<Role> roles) {
        return requireAnyRole(roles.stream());
    }

    public static MangoPermission requireAnyRole(Stream<Role> roles) {
        return new MangoPermission(roles
                .map(Collections::singleton)
                .collect(Collectors.toSet()));
    }

    public static MangoPermission requireAllRoles(Role ...roles) {
        return requireAllRoles(Arrays.stream(roles));
    }

    public static MangoPermission requireAllRoles(Set<Role> roles) {
        return new MangoPermission(Collections.singleton(roles));
    }

    public static MangoPermission requireAllRoles(Stream<Role> roles) {
        return requireAllRoles(roles.collect(Collectors.toSet()));
    }

    @Override
    public String toString() {
        return "id: " + id + "\n" + roles.toString();
    }

}
