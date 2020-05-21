/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.permission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
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
                    for(Set<Role> otherRoleSet : other.roles) {
                        int found = 0;
                        for(Role role : roleSet) {
                            if(otherRoleSet.contains(role)) {
                                found++;
                            }
                        }
                        if(found != roleSet.size()) {
                            return false;
                        }
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

    public static MangoPermission createOrSet(Role...roles) {
        Set<Set<Role>> roleSet = new HashSet<>();
        for(Role role : roles) {
            roleSet.add(Collections.singleton(role));
        }
        return new MangoPermission(roleSet);
    }

    public static MangoPermission createOrSet(Set<Role> roles) {
        Set<Set<Role>> roleSet = new HashSet<>();
        for(Role role : roles) {
            roleSet.add(Collections.singleton(role));
        }
        return new MangoPermission(roleSet);
    }

    public static MangoPermission createAndSet(Role...roles) {
        Set<Set<Role>> roleSet = new HashSet<>();
        Set<Role> andSet = new HashSet<>();
        roleSet.add(andSet);
        for(Role role : roles) {
            andSet.add(role);
        }
        return new MangoPermission(roleSet);
    }


    /**
     * @param readRoles
     * @return
     */
    public static MangoPermission createAndSet(Set<Role> roles) {
        Set<Set<Role>> roleSet = new HashSet<>();
        Set<Role> andSet = new HashSet<>();
        roleSet.add(andSet);
        for(Role role : roles) {
            andSet.add(role);
        }
        return new MangoPermission(roleSet);
    }

    public boolean isGranted(PermissionHolder user) {
        Set<Role> userRoles = user.getAllInheritedRoles();
        for (Set<Role> term : roles) {
            if (userRoles.containsAll(term)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "id: " + id;
    }

}
