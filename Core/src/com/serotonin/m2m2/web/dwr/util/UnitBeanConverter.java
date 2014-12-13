/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr.util;

import java.net.URLDecoder;

import javax.measure.unit.Unit;

import org.directwebremoting.dwrp.SimpleOutboundVariable;
import org.directwebremoting.extend.Converter;
import org.directwebremoting.extend.ConverterManager;
import org.directwebremoting.extend.InboundContext;
import org.directwebremoting.extend.InboundVariable;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.util.UnitUtil;

/**
 * @author Terry Packer
 *
 */
public class UnitBeanConverter implements Converter{

	/* (non-Javadoc)
	 * @see org.directwebremoting.extend.Converter#convertInbound(java.lang.Class, org.directwebremoting.extend.InboundVariable, org.directwebremoting.extend.InboundContext)
	 */
	@Override
	public Object convertInbound(Class paramClass,
			InboundVariable paramInboundVariable,
			InboundContext paramInboundContext) throws MarshallException {
		
		//Convert from string to Unit
		try{
			//For the ONE unit
			if(paramInboundVariable.getValue().equals("ONE"))
				return Unit.ONE;
			else
				return UnitUtil.parseLocal(URLDecoder.decode(paramInboundVariable.getValue(), Common.UTF8));
		}catch(Exception e){
			throw new MarshallException(paramClass);
		}
	}

	/* (non-Javadoc)
	 * @see org.directwebremoting.extend.Converter#convertOutbound(java.lang.Object, org.directwebremoting.extend.OutboundContext)
	 */
	@Override
	public OutboundVariable convertOutbound(Object paramObject,
			OutboundContext paramOutboundContext) throws MarshallException {
		//Convert from Unit to String

		// Check to see if we have done this one already
        OutboundVariable ov = paramOutboundContext.get(paramObject);
        if (ov != null) {
            // So the object as been converted already, we just need to refer to it.
            return ov.getReferenceVariable();
        }
        
        if(paramObject instanceof Unit<?>){
	        
	        Unit<?> unit = (Unit<?>)paramObject;
	        String unitString = UnitUtil.formatLocal(unit);
	        if(unit == unit.ONE)
	        	unitString = "ONE";
	        	
	        return new SimpleOutboundVariable("'" + unitString + "';",paramOutboundContext,false);
        }else{
                throw new MarshallException(paramObject.getClass());
        }
        
        
	}

	/* (non-Javadoc)
	 * @see org.directwebremoting.extend.Converter#setConverterManager(org.directwebremoting.extend.ConverterManager)
	 */
	@Override
	public void setConverterManager(ConverterManager paramConverterManager) {
		// TODO Auto-generated method stub
		
	}

}
