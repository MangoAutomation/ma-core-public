package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;

/**
 * Abstract CSVPropertyEditor that need to be implemented
 * for fields not supported by the default available PropertyEditors.
 *
 * See PropertyEditorManager for what fields can be handles by default
 *
 * The PropertyEditor need to be able to handle that the value can be null if
 * the annoteted field can be null.
 * 
 * @see PropertyEditorManager
 * Based on work done by Staffan Friberg
 * @author Terry Packer
 */
public abstract class CSVPropertyEditor implements PropertyEditor {

	protected Object context;
	
	public Object getContext(){
		return context;
	}
	public void setContext(Object context){
		this.context = context;
	}
	
   /**
    * Set (or change) the object that is to be edited.
    * 
    * @param value The new target object to be edited.
    */
   public abstract void setValue(Object value);

   /**
    * Gets the property value.
    *
    * @return The value of the property.
    */
   public abstract Object getValue();

   /**
    * Gets the property value as text.
    *
    * @return The property value as a human editable string.
    */
   public abstract String getAsText();

   /**
    * Set the property value by parsing a given String. May raise
    * java.lang.IllegalArgumentException if either the String is badly
    * formatted or if this kind of property can't be expressed as text.
    *
    * @param text The string to be parsed.
    * @throws IllegalArgumentException
    */
   public abstract void setAsText(String text) throws IllegalArgumentException;

   /*
    *  The rest of the methods are unused and don't need to be implemented
    */

   public boolean isPaintable() {
      throw new UnsupportedOperationException("Not supported.");
   }

   public void paintValue(Graphics gfx, Rectangle box) {
      throw new UnsupportedOperationException("Not supported.");
   }

   public String getJavaInitializationString() {
      throw new UnsupportedOperationException("Not supported.");
   }

   public String[] getTags() {
      throw new UnsupportedOperationException("Not supported.");
   }

   public Component getCustomEditor() {
      throw new UnsupportedOperationException("Not supported.");
   }

   public boolean supportsCustomEditor() {
      throw new UnsupportedOperationException("Not supported.");
   }

   public void addPropertyChangeListener(PropertyChangeListener listener) {
      throw new UnsupportedOperationException("Not supported.");
   }

   public void removePropertyChangeListener(PropertyChangeListener listener) {
      throw new UnsupportedOperationException("Not supported.");
   }
}
