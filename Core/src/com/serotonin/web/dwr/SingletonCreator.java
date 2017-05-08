package com.serotonin.web.dwr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.create.AbstractCreator;
import org.directwebremoting.util.LocalUtil;
import org.directwebremoting.util.Messages;

public class SingletonCreator extends AbstractCreator {
    private static final Log LOG = LogFactory.getLog(SingletonCreator.class);

    private Object instance;

    public void setClass(String classname) {
        try {
            Class<?> clazz = LocalUtil.classForName(classname);
            instance = clazz.newInstance();
        }
        catch (Exception ex) {
            LOG.error("", ex);
            throw new IllegalArgumentException(Messages.getString("Creator.ClassNotFound", classname)); //$NON-NLS-1$
        }
    }

    public Object getInstance() {
        return instance;
    }

    public Class<?> getType() {
        return instance.getClass();
    }
}
