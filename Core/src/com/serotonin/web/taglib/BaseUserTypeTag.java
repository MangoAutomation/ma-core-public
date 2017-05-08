package com.serotonin.web.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

abstract public class BaseUserTypeTag<T> extends TagSupport {
    private static final long serialVersionUID = 1L;

    //
    // /
    // / Stuff to implement
    // /
    //
    /**
     * @return the user that is currently in session. This is only called if the user object is not provided as an
     *         attribute.
     */
    abstract protected T getSessionUser();

    /**
     * @return the complete list of all supported user types. This is used to validate the is/isnt attributes.
     */
    abstract protected String[] getUserTypes();

    /**
     * @return a string representing the user type of this user object. If the user in fact qualifies for multiple user
     *         types, this method should return null, and the string array version below should return all of the types.
     */
    abstract protected String getUserType(T user);

    /**
     * @return an array of strings representing all user types for which the user qualifies. This is only called if the
     *         above single user type method returns null.
     */
    abstract protected String[] getUserTypes(T user);

    //
    // /
    // / Base implementation
    // /
    //
    private static final String ANONYMOUS = "anonymous";

    private String[] is;
    private String[] isnt;
    private T user;

    public void setIs(String is) throws JspException {
        this.is = is.split(",");
        validate(this.is);
    }

    public void setIsnt(String isnt) throws JspException {
        this.isnt = isnt.split(",");
        validate(this.isnt);
    }

    public void setUser(T user) {
        this.user = user;
    }

    @Override
    public int doStartTag() throws JspException {
        if (is == null && isnt == null)
            throw new JspException("One of 'is' or 'isnt' attributes must be defined");
        if (is != null && isnt != null)
            throw new JspException("Only one of 'is' or 'isnt' attributes can be defined");

        // Get the regular type of user object.
        T userToTest = user;
        if (userToTest == null)
            userToTest = getSessionUser();

        if (userToTest == null) {
            // No user object to test, so check if an anonymous user is allowed.
            if (isUserType(ANONYMOUS) || isntUserType(ANONYMOUS))
                return EVAL_BODY_INCLUDE;
        }
        else {
            String userType = getUserType(userToTest);
            if (userType == null) {
                // Multiple user type.
                String[] userTypes = getUserTypes(userToTest);
                if (isUserType(userTypes) || isntUserType(userTypes))
                    return EVAL_BODY_INCLUDE;
            }
            else {
                // Single user type.
                if (isUserType(userType) || isntUserType(userType))
                    return EVAL_BODY_INCLUDE;
            }
        }

        return SKIP_BODY;
    }

    private boolean isUserType(String userType) {
        if (is == null)
            return false;
        return contains(is, userType);
    }

    private boolean isUserType(String[] userTypes) {
        if (is == null)
            return false;
        return contains(userTypes, is);
    }

    private boolean isntUserType(String userType) {
        if (isnt == null)
            return false;
        return !contains(isnt, userType);
    }

    private boolean isntUserType(String[] userTypes) {
        if (isnt == null)
            return false;
        return !contains(userTypes, isnt);
    }

    /**
     * Check for any intersection of the given arrays. If there is at least one common element, return true otherwise
     * return false.
     * 
     * @param arr1
     *            the first array
     * @param arr2
     *            the second array.
     * @return true if there is a common element.
     */
    private boolean contains(String[] arr1, String[] arr2) {
        for (int i = 0; i < arr1.length; i++) {
            if (contains(arr2, arr1[i]))
                return true;
        }
        return false;
    }

    /**
     * Check if the given item is in the given array.
     * 
     * @param arr
     *            the array of strings
     * @param item
     *            the string item to look for
     * @return true if the item is found, false otherwise.
     */
    private boolean contains(String[] arr, String item) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(item))
                return true;
        }
        return false;
    }

    private void validate(String[] list) throws JspException {
        // Each item in the given list has to be a valid user type.
        String[] userTypes = getUserTypes();
        for (int i = 0; i < list.length; i++) {
            if (!ANONYMOUS.equals(list[i]) && !contains(userTypes, list[i]))
                throw new JspException("Invalid user type: " + list[i]);
        }
    }

    @Override
    public void release() {
        super.release();
        is = null;
        isnt = null;
        user = null;
    }
}
