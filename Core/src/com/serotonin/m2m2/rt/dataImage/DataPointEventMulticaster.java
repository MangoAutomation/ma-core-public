/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.Map;

import com.serotonin.m2m2.util.ExceptionListWrapper;
import com.serotonin.m2m2.vo.DataPointVO;

public class DataPointEventMulticaster implements DataPointListener {
    protected final DataPointListener a, b;

    protected DataPointEventMulticaster(DataPointListener a, DataPointListener b) {
        this.a = a;
        this.b = b;
    }

    protected DataPointListener remove(DataPointListener oldl) {
        if (oldl == a)
            return b;
        if (oldl == b)
            return a;
        DataPointListener a2 = remove(a, oldl);
        DataPointListener b2 = remove(b, oldl);
        if (a2 == a && b2 == b) {
            return this;
        }
        return add(a2, b2);
    }

    public static DataPointListener add(DataPointListener a, DataPointListener b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        return new DataPointEventMulticaster(a, b);
    }

    public static DataPointListener remove(DataPointListener l, DataPointListener oldl) {
        if (l == oldl || l == null)
            return null;

        if (l instanceof DataPointEventMulticaster)
            return ((DataPointEventMulticaster) l).remove(oldl);

        return l;
    }

    private static int getListenerCount(DataPointListener l) {
        if (l instanceof DataPointEventMulticaster) {
            DataPointEventMulticaster mc = (DataPointEventMulticaster) l;
            return getListenerCount(mc.a) + getListenerCount(mc.b);
        }

        return 1;
    }

    private static int populateListenerArray(DataPointListener[] a, DataPointListener l, int index) {
        if (l instanceof DataPointEventMulticaster) {
            DataPointEventMulticaster mc = (DataPointEventMulticaster) l;
            int lhs = populateListenerArray(a, mc.a, index);
            return populateListenerArray(a, mc.b, lhs);
        }

        if (a.getClass().getComponentType().isInstance(l)) {
            a[index] = l;
            return index + 1;
        }

        return index;
    }

    public static DataPointListener[] getListeners(DataPointListener l) {
        int n = getListenerCount(l);
        DataPointListener[] result = new DataPointListener[n];
        populateListenerArray(result, l, 0);
        return result;
    }

    //
    // /
    // / DataPointListener interface
    // /
    // /
	@Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) throws ExceptionListWrapper {
	    ExceptionListWrapper exceptionWrapper = null;
	    try {
	        a.pointChanged(oldValue, newValue);
	    } catch(Exception e) {
	        if(!(e instanceof ExceptionListWrapper)) 
	            exceptionWrapper = new ExceptionListWrapper(e);
	        else //If it is, it's already added
	            exceptionWrapper = (ExceptionListWrapper)e;
	    }
	    
	    try {
	        b.pointChanged(oldValue, newValue);
	    } catch(Exception e) {
	        if(exceptionWrapper != null)
	            exceptionWrapper.addException(e);
	        else if(!(e instanceof ExceptionListWrapper))
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
	    
	    if(exceptionWrapper != null)
	        throw exceptionWrapper;
    }
	@Override
    public void pointSet(PointValueTime oldValue, PointValueTime newValue) throws ExceptionListWrapper {
	    ExceptionListWrapper exceptionWrapper = null;
        try {
	        a.pointSet(oldValue, newValue);
	    } catch(Exception e) {
            if(!(e instanceof ExceptionListWrapper)) 
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
        
        try {
	        b.pointSet(oldValue, newValue);
	    } catch(Exception e) {
            if(exceptionWrapper != null)
                exceptionWrapper.addException(e);
            else if(!(e instanceof ExceptionListWrapper))
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
    }
	@Override
    public void pointUpdated(PointValueTime newValue) throws ExceptionListWrapper {
	    ExceptionListWrapper exceptionWrapper = null;
	    try {
	        a.pointUpdated(newValue);
	    } catch(Exception e) {
            if(!(e instanceof ExceptionListWrapper)) 
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }

	    try {
	        b.pointUpdated(newValue);
	    } catch(Exception e) {
            if(exceptionWrapper != null)
                exceptionWrapper.addException(e);
            else if(!(e instanceof ExceptionListWrapper))
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
    }
	@Override
    public void pointBackdated(PointValueTime value) throws ExceptionListWrapper {
	    ExceptionListWrapper exceptionWrapper = null;
	    try {
	        a.pointBackdated(value);
	    } catch(Exception e) {
            if(!(e instanceof ExceptionListWrapper)) 
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
	    
	    try {
	        b.pointBackdated(value);
	    } catch(Exception e) {
            if(exceptionWrapper != null)
                exceptionWrapper.addException(e);
            else if(!(e instanceof ExceptionListWrapper))
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
    }
	@Override
    public void pointInitialized() throws ExceptionListWrapper {
	    ExceptionListWrapper exceptionWrapper = null;
	    try {
	        a.pointInitialized();
	    } catch(Exception e) {
            if(!(e instanceof ExceptionListWrapper)) 
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }

	    try {
	        b.pointInitialized();
	    } catch(Exception e) {
            if(exceptionWrapper != null)
                exceptionWrapper.addException(e);
            else if(!(e instanceof ExceptionListWrapper))
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
    }
	@Override
    public void pointTerminated(DataPointVO vo) throws ExceptionListWrapper {
	    ExceptionListWrapper exceptionWrapper = null;
	    try {
	        a.pointTerminated(vo);
	    } catch(Exception e) {
            if(!(e instanceof ExceptionListWrapper)) 
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
	    
	    try {
	        b.pointTerminated(vo);
	    } catch(Exception e) {
            if(exceptionWrapper != null)
                exceptionWrapper.addException(e);
            else if(!(e instanceof ExceptionListWrapper))
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
    }

	@Override
	public void pointLogged(PointValueTime value) throws ExceptionListWrapper {
	    ExceptionListWrapper exceptionWrapper = null;
	    try {
	        a.pointLogged(value);
	    } catch(Exception e) {
            if(!(e instanceof ExceptionListWrapper)) 
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
	    
	    try {
	        b.pointLogged(value);
	    } catch(Exception e) {
            if(exceptionWrapper != null)
                exceptionWrapper.addException(e);
            else if(!(e instanceof ExceptionListWrapper))
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.DataPointListener#attributeChanged(java.util.Map)
	 */
	@Override
	public void attributeChanged(Map<String, Object> attributes) throws ExceptionListWrapper {
	    ExceptionListWrapper exceptionWrapper = null;
	    try {
	        a.attributeChanged(attributes);
	    } catch(Exception e) {
            if(!(e instanceof ExceptionListWrapper)) 
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }

	    try {
	        b.attributeChanged(attributes);
	    } catch(Exception e) {
            if(exceptionWrapper != null)
                exceptionWrapper.addException(e);
            else if(!(e instanceof ExceptionListWrapper))
                exceptionWrapper = new ExceptionListWrapper(e);
            else //If it is, it's already added
                exceptionWrapper = (ExceptionListWrapper)e;
        }
        
        if(exceptionWrapper != null)
            throw exceptionWrapper;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.dataImage.DataPointListener#getListenerName()
	 */
	@Override
	public String getListenerName() {
		String path = "";
		if(a != null)
			path += a.getListenerName();
		if(b != null)
			path += "," + b.getListenerName();
		return path;
	}
}
