/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVEntity;
import com.serotonin.m2m2.web.mvc.rest.v1.model.DemoModel.Demo;

/**
 * @author Terry Packer
 *
 */
@CSVEntity
public class DemoModel extends AbstractVoModel<Demo>{

	
	/**
	 * @param data
	 */
	public DemoModel(Demo data) {
		super(data);
	}

	public DemoModel(){
		super(new Demo());
	}
	
	@CSVColumnGetter(order=3, header="demoString")
	public String getDemoString() {
		return data.demoString;
	}
	@CSVColumnSetter(order=3, header="demoString")
	public void setDemoString(String demoString) {
		data.demoString = demoString;
	}

	@CSVColumnGetter(order=4, header="demoInteger")
	public Integer getDemoInteger() {
		return data.demoInteger;
	}
	@CSVColumnSetter(order=4, header="demoInteger")
	public void setDemoInteger(Integer demoInteger) {
		data.demoInteger = demoInteger;
	}
	
	@CSVColumnGetter(order=5, header="demoBoolean")
	public Boolean getDemoBoolean() {
		return data.demoBoolean;
	}
	@CSVColumnSetter(order=5, header="demoBoolean")
	public void setDemoBoolean(Boolean demoBoolean) {
		data.demoBoolean = demoBoolean;
	}
	
	@CSVColumnGetter(order=6, header="demoDouble")
	public Double getDemoDouble() {
		return data.demoDouble;
	}
	@CSVColumnSetter(order=6, header="demoDouble")
	public void setDemoDouble(Double demoDouble) {
		data.demoDouble = demoDouble;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel#getModelType()
	 */
	@Override
	public String getModelType() {
		return DemoModelDefinition.TYPE_NAME;
	}
	
	/**
	 * 
	 * @author Terry Packer
	 *
	 */
	public static class Demo extends AbstractVO<Demo>{

		private String demoString;
		private Integer demoInteger;
		private Boolean demoBoolean;
		private Double demoDouble;
		
		public String getDemoString() {
			return demoString;
		}

		public void setDemoString(String demoString) {
			this.demoString = demoString;
		}

		public Integer getDemoInteger() {
			return demoInteger;
		}

		public void setDemoInteger(Integer demoInteger) {
			this.demoInteger = demoInteger;
		}

		public Boolean getDemoBoolean() {
			return demoBoolean;
		}

		public void setDemoBoolean(Boolean demoBoolean) {
			this.demoBoolean = demoBoolean;
		}

		public Double getDemoDouble() {
			return demoDouble;
		}

		public void setDemoDouble(Double demoDouble) {
			this.demoDouble = demoDouble;
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.util.ChangeComparable#getTypeKey()
		 */
		@Override
		public String getTypeKey() {
			return "DEMO";
		}
		
	}
}
