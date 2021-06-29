/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import com.serotonin.m2m2.util.ExceptionListWrapper;

/**
 *
 * @author Phillip Dunlap
 */
public class UserEventMulticaster implements UserEventListener {
    
    private static final int MULTICASTER_ID = -100;
    protected final UserEventListener a, b;
    
    protected UserEventMulticaster(UserEventListener a, UserEventListener b) {
        this.a = a;
        this.b = b;
    }
    
    protected UserEventListener remove(UserEventListener oldl) {
        if(oldl == a)
            return b;
        if(oldl == b)
            return a;
        UserEventListener a2 = remove(a, oldl);
        UserEventListener b2 = remove(b, oldl);
        if(a2 == a && b2 == b)
            return this;
        return add(a2, b2);
    }
    
    /**
     * 
     * @param a - Existing listener(s)
     * @param b - new listener
     * @return
     */
    public static UserEventListener add(UserEventListener a, UserEventListener b) {
        if(a == null)
            return b;
        if(b == null)
            return a;
        return new UserEventMulticaster(a, b);
    }
    

    /**
     * 
     * @param existing - Existing listener(s)
     * @param toRemove - listener to remove from multicast
     * @return
     */
    public static UserEventListener remove(UserEventListener existing, UserEventListener toRemove) {
        if (existing == toRemove || existing == null)
            return null;
        
        if (existing instanceof UserEventMulticaster)
            return ((UserEventMulticaster) existing).remove(toRemove);
        
        return existing;
    }
    
    static int getListenerCount(UserEventListener l) {
        if (l instanceof UserEventMulticaster) {
            UserEventMulticaster mc = (UserEventMulticaster) l;
            return getListenerCount(mc.a) + getListenerCount(mc.b);
        }

        return 1;
    }

    private static int populateListenerArray(UserEventListener[] a, UserEventListener l, int index) {
        if (l instanceof UserEventMulticaster) {
            UserEventMulticaster mc = (UserEventMulticaster) l;
            int lhs = populateListenerArray(a, mc.a, index);
            return populateListenerArray(a, mc.b, lhs);
        }

        if (a.getClass().getComponentType().isInstance(l)) {
            a[index] = l;
            return index + 1;
        }

        return index;
    }

    public static UserEventListener[] getListeners(UserEventListener l) {
        int n = getListenerCount(l);
        UserEventListener[] result = new UserEventListener[n];
        populateListenerArray(result, l, 0);
        return result;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#getUserId()
     */
    @Override
    public int getUserId() {
        return MULTICASTER_ID;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#raised(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public void raised(EventInstance evt) {
        ExceptionListWrapper exceptionWrapper = null;
        if(a.getUserId() == MULTICASTER_ID || evt.getIdsToNotify() == null ||
                evt.getIdsToNotify().contains(a.getUserId())) {
            try {
                a.raised(evt);
            } catch(Exception e) {
                if(!(e instanceof ExceptionListWrapper))
                    exceptionWrapper = new ExceptionListWrapper(e);
                else
                    exceptionWrapper = (ExceptionListWrapper)e;
            }
        }
        
        if(b.getUserId() == MULTICASTER_ID || evt.getIdsToNotify() == null ||
                evt.getIdsToNotify().contains(b.getUserId())) {
            try {
                b.raised(evt);
            } catch(Exception e) {
                if(exceptionWrapper != null)
                    exceptionWrapper.addException(e);
                else if(!(e instanceof ExceptionListWrapper))
                    exceptionWrapper = new ExceptionListWrapper(e);
                else //If it is, it's already added
                    exceptionWrapper = (ExceptionListWrapper)e;
            }
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#returnToNormal(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public void returnToNormal(EventInstance evt) {
        ExceptionListWrapper exceptionWrapper = null;
        if(a.getUserId() == MULTICASTER_ID || evt.getIdsToNotify() == null ||
                evt.getIdsToNotify().contains(a.getUserId())) {
            try {
                a.returnToNormal(evt);
            } catch(Exception e) {
                if(!(e instanceof ExceptionListWrapper))
                    exceptionWrapper = new ExceptionListWrapper(e);
                else
                    exceptionWrapper = (ExceptionListWrapper)e;
            }
        }
        
        if(b.getUserId() == MULTICASTER_ID || evt.getIdsToNotify() == null ||
                evt.getIdsToNotify().contains(b.getUserId())) {
            try {
                b.returnToNormal(evt);
            } catch(Exception e) {
                if(exceptionWrapper != null)
                    exceptionWrapper.addException(e);
                else if(!(e instanceof ExceptionListWrapper))
                    exceptionWrapper = new ExceptionListWrapper(e);
                else //If it is, it's already added
                    exceptionWrapper = (ExceptionListWrapper)e;
            }
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#deactivated(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public void deactivated(EventInstance evt) {
        ExceptionListWrapper exceptionWrapper = null;
        if(a.getUserId() == MULTICASTER_ID || evt.getIdsToNotify() == null ||
                evt.getIdsToNotify().contains(a.getUserId())) {
            try {
                a.deactivated(evt);
            } catch(Exception e) {
                if(!(e instanceof ExceptionListWrapper))
                    exceptionWrapper = new ExceptionListWrapper(e);
                else
                    exceptionWrapper = (ExceptionListWrapper)e;
            }
        }
        
        if(b.getUserId() == MULTICASTER_ID || evt.getIdsToNotify() == null ||
                evt.getIdsToNotify().contains(b.getUserId())) {
            try {
                b.deactivated(evt);
            } catch(Exception e) {
                if(exceptionWrapper != null)
                    exceptionWrapper.addException(e);
                else if(!(e instanceof ExceptionListWrapper))
                    exceptionWrapper = new ExceptionListWrapper(e);
                else //If it is, it's already added
                    exceptionWrapper = (ExceptionListWrapper)e;
            }
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#acknowledged(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public void acknowledged(EventInstance evt) {
        ExceptionListWrapper exceptionWrapper = null;
        if(a.getUserId() == MULTICASTER_ID || evt.getIdsToNotify() == null ||
                evt.getIdsToNotify().contains(a.getUserId())) {
            try {
                a.acknowledged(evt);
            } catch(Exception e) {
                if(!(e instanceof ExceptionListWrapper))
                    exceptionWrapper = new ExceptionListWrapper(e);
                else
                    exceptionWrapper = (ExceptionListWrapper)e;
            }
        }
        
        if(b.getUserId() == MULTICASTER_ID || evt.getIdsToNotify() == null ||
                evt.getIdsToNotify().contains(b.getUserId())) {
            try {
                b.acknowledged(evt);
            } catch(Exception e) {
                if(exceptionWrapper != null)
                    exceptionWrapper.addException(e);
                else if(!(e instanceof ExceptionListWrapper))
                    exceptionWrapper = new ExceptionListWrapper(e);
                else //If it is, it's already added
                    exceptionWrapper = (ExceptionListWrapper)e;
            }
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
        
    }

}
