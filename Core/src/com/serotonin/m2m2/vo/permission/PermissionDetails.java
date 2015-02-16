package com.serotonin.m2m2.vo.permission;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author Matthew
 */
public class PermissionDetails {
    private final String username;
    private boolean admin;
    private final Set<String> allGroups = new TreeSet<>();
    private final Set<String> matchingGroups = new TreeSet<>();

    public PermissionDetails(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public Set<String> getAllGroups() {
        return allGroups;
    }

    public Set<String> getMatchingGroups() {
        return matchingGroups;
    }

    public void addGroup(String group) {
        allGroups.add(group);
    }

    public void addMatchingGroup(String group) {
        matchingGroups.add(group);
    }

    public boolean isAccess() {
        if (admin)
            return true;
        return !matchingGroups.isEmpty();
    }
}
