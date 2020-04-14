/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.permission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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

    @JsonProperty
    protected final Set<Set<Role>> roles;

    public MangoPermission() {
        this(Collections.emptySet());
    }

    public MangoPermission(Set<Set<Role>> roles) {
        this.roles = Collections.unmodifiableSet(roles);
    }

    public Set<Set<Role>> getRoles() {
        return roles;
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
        return unique;
    }

    public List<MangoPermissionEncoded> encode() {
        Set<Role> allRoles = new LinkedHashSet<>();
        for (Set<Role> term : roles) {
            allRoles.addAll(term);
        }
        List<MangoPermissionEncoded> encoded = new ArrayList<>(allRoles.size());
        for (Role r : allRoles) {
            long mask = 0;
            int bit = 0;
            for (Set<Role> term : roles) {
                if (!term.contains(r)) {
                    mask |= 1L << bit;
                }
                bit++;
            }
            encoded.add(new MangoPermissionEncoded(r, mask));
        }
        return encoded;
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

    /**
     * Assumes that all non-zero terms are packed into the lower bits.
     *
     * @param encoded
     * @return
     */
    public static MangoPermission decode(List<MangoPermissionEncoded> encoded) {
        if(encoded.size() == 0) {
            return new MangoPermission();
        }

        long combinedMask = 0;
        for (MangoPermissionEncoded e : encoded) {
            combinedMask |= e.mask;
        }
        int numTerms = Long.SIZE - Long.numberOfLeadingZeros(combinedMask);
        // no mask bits set, short circuit logic, return single term containing all roles
        if (numTerms == 0) {
            Set<Role> term = new LinkedHashSet<>(encoded.size());
            for (MangoPermissionEncoded e : encoded) {
                term.add(e.role);
            }
            return new MangoPermission(Collections.singleton(term));
        }
        Set<Set<Role>> terms = new LinkedHashSet<>(numTerms);
        for (int bit = 0; bit < numTerms; bit++) {
            Set<Role> term = new LinkedHashSet<>(encoded.size());
            for (MangoPermissionEncoded e : encoded) {
                if ((e.mask & (1L << bit)) == 0) {
                    term.add(e.role);
                }
            }
            terms.add(term);
        }
        return new MangoPermission(terms);
    }

    public boolean isGranted(PermissionHolder user) {
        Set<Role> userRoles = user.getRoles();
        for (Set<Role> term : roles) {
            if (userRoles.containsAll(term)) {
                return true;
            }
        }
        return false;
    }

    public static class MangoPermissionEncoded {
        final Role role;
        final long mask;
        public MangoPermissionEncoded(Role role, long mask) {
            this.role = role;
            this.mask = mask;
        }
        public Role getRole() {
            return role;
        }
        public long getMask() {
            return mask;
        }

    }

}
