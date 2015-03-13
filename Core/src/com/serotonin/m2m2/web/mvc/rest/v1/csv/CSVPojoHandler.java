package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.module.ModelDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.NoSupportingModelException;

/**
 * Class that handles the mapping between Pojo classes their children and CSV
 * files
 * 
 * Based on work done by Staffan Friberg
 * @author Terry Packer
 */
class CSVPojoHandler<T> {

	// Pojo Class
	private Class<T> clazz;
	private T instance;
	private ModelDefinition definition;
	// Mapping of headers to Fields and PropertyEditors
	private final Map<String, CSVDataHandler> mapping;
	private final List<CSVDataHandler> handlers;
	// Child CSVEntities found in the pojo
	private final List<PojoChild> children;

	/**
	 * Create a new handler for a pojo structure Will throw
	 * IllegalArguemtnException if the pojo isn't annotated with CSVEntity
	 *
	 * @param clazz
	 *            The pojo class to handle
	 */
	public CSVPojoHandler() {
		this.mapping = new HashMap<String, CSVDataHandler>();
		this.handlers = new ArrayList<CSVDataHandler>();
		this.children = new ArrayList<PojoChild>();
	}

	public boolean isInitialized(){
		return definition != null;
	}
	
	public void initialize(T instance) throws NoSupportingModelException{
		this.instance = instance;
		this.clazz = (Class<T>) instance.getClass();
		//TODO Need to Create a Registry in the ModuleRegsitry to create new models for Types...
		this.definition = findModelDefinition(this.clazz);
		
		if (clazz.getAnnotation(CSVEntity.class) == null) {
			throw new IllegalArgumentException("The class " + clazz
					+ " is not annotated with " + CSVEntity.class.getName());
		}
		buildMapping(instance);
	}
	
	public void initialize(String[] line, T instance) throws ModelNotFoundException, NoSupportingModelException{
		this.clazz = (Class<T>) instance.getClass();
		this.definition = findModelDefinition(this.clazz);
		
		if (clazz.getAnnotation(CSVEntity.class) == null) {
			throw new IllegalArgumentException("The class " + clazz
					+ " is not annotated with " + CSVEntity.class.getName());
		}
		buildMapping(line);
	}
	
	/**
	 * @return
	 * @throws NoSupportingModelException 
	 */
	public ModelDefinition findModelDefinition(Class<T> clazz) throws NoSupportingModelException {
		List<ModelDefinition> definitions = ModuleRegistry.getModelDefinitions();
		for(ModelDefinition definition : definitions){
			if(definition.supportsClass(clazz))
				return definition;
		}
		throw new NoSupportingModelException(clazz);
	}

	/**
	 * @return
	 * @throws ModelNotFoundException 
	 */
	public ModelDefinition findModelDefinition(String typeName) throws ModelNotFoundException {
		List<ModelDefinition> definitions = ModuleRegistry.getModelDefinitions();
		for(ModelDefinition definition : definitions){
			if(definition.getModelTypeName().equalsIgnoreCase(typeName))
				return definition;
		}
		throw new ModelNotFoundException(typeName);
	}

	
	public List<CSVDataHandler> getHandlers(){
		List<CSVDataHandler> allHandlers = new ArrayList<CSVDataHandler>(this.handlers);
		//Add all from children
		for (PojoChild child : children) {
			allHandlers.addAll(child.getPojoHandler().getHandlers());
		}
		
		return allHandlers;
	}
	
	/**
	 * Get a list of all headers for this pojo including children. Fields will
	 * be sorted by name in within each CVSPojoEntity
	 *
	 * @return List of String contain all headers
	 */
	public List<String> getHeaders() {
		List<CSVDataHandler> allHandlers = getHandlers();
		
		//Sort master list
		Collections.sort(allHandlers);
		List<String> headers = new ArrayList<String>(allHandlers.size());
		for(CSVDataHandler handler : allHandlers){
			headers.add(handler.getHeader());
		}
		return headers;
		
//		List<String> headers = new ArrayList<String>(mapping.keySet());
//		// Make sure we always output in the same order
//		Collections.sort(headers);
//		for (PojoChild child : children) {
//			headers.addAll(child.getPojoHandler().getHeaders());
//		}
//		return headers;
	}

	public void setupModelDefinition(Class<T> clazz) throws NoSupportingModelException{
		this.clazz = clazz;
		this.definition = findModelDefinition(this.clazz);
	}
	/**
	 * Create a new instance of the pojo.
	 *
	 * @return The new Pojo object
	 */
	public T newInstance() {
		return (T) definition.createModel();
	}
	
	/**
	 * Set the filed of in a pojo or child entity of it.
	 *
	 * @param pojo
	 *            The object to set the value in
	 * @param header
	 *            The header of the field
	 * @param text
	 *            The String representation of the value to set
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean setField(T pojo, String header, String text) {
		if (text.length() > 0) {
			// If text is empty use default value in pojo which is set by the
			// user

			CSVDataHandler fieldHandler = mapping.get(header);
			// Check that column is mapped by a field a pojo might not contain
			// all columns
			if (fieldHandler != null) {
				PropertyEditor editor = fieldHandler.getEditor();
				if(editor instanceof CSVPropertyEditor)
					((CSVPropertyEditor) editor).setContext(pojo);
				editor.setAsText(text);
				Object value = editor.getValue();
				fieldHandler.setValue(pojo, value);
				return true;
			} else {
				// No mapping found in this pojo, check children
				for (PojoChild child : children) {
					CSVPojoHandler pojoHandler = child.getPojoHandler();
					if (pojoHandler.containsField(header)) {

						Object obj = child.getValue(pojo);
						if (obj == null) {
							obj = pojoHandler.newInstance();
						}
						if (pojoHandler.setField(obj, header, text)) {
							// Only set child field if we actually update the
							// field in the object object
							// If no fields are set in the child, the child
							// should be the default value
							child.setValue(pojo, obj);
							return true;
						}
					}
				}
			}
		}else{
			//No value set as it was empty
			return true;
		}
		System.err.println("No Such field found - " + header);
		return false;
	}

	/**
	 * Get the field in a Pojo inluding child entities
	 *
	 * @param pojo
	 *            The pojo object to look in
	 * @param header
	 *            The header to find
	 * @return The String representing the value in the pojo
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getField(T pojo, String header) {
		if (pojo == null) {
			// If a pojo is null we should just return empty strings for the
			// field
			return "";
		}
		CSVDataHandler fieldHandler = mapping.get(header);
		if (fieldHandler != null) {
			PropertyEditor editor = fieldHandler.getEditor();
			editor.setValue(fieldHandler.getValue(pojo));
			return editor.getAsText();
		} else {
			// No mapping found in this pojo, check children
			for (PojoChild child : children) {
				if(child.getHeader().equals(header)){
					return child.getCsvEntityAnnotation().typeName();
				}
				CSVPojoHandler pojoHandler = child.getPojoHandler();
				if (pojoHandler.containsField(header)) {
					Object obj = child.getValue(pojo);
					return pojoHandler.getField(obj, header);
				}
			}
		}
		// With correctly built Pojo structures we shouldn't end up here.
		throw new IllegalArgumentException("The header," + header
				+ ", is not available in the Pojo structure.");
	}

	private boolean containsField(String header) {
		return mapping.containsKey(header);
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void buildMapping(String[] line) throws ModelNotFoundException, NoSupportingModelException {
		for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
			if (c.getAnnotation(CSVEntity.class) != null) {
				for (Field field : c.getDeclaredFields()) {
					CSVColumn csvColumn = field.getAnnotation(CSVColumn.class);
					if(csvColumn == null)
						continue;
					// Check that the field show be used (not transient and not
					// hidden)
					if (!Modifier.isTransient(field.getModifiers())
							&& (csvColumn == null || !csvColumn.hidden())) {
						PropertyEditor editor;
						try {
							// Try to determine PropertyEditor for field
							editor = (csvColumn == null || csvColumn.editor() == CSVPropertyEditor.class) ? PropertyEditorManager
									.findEditor(field.getType()) : csvColumn
									.editor().newInstance();
						} catch (InstantiationException ex) {
							throw new InstantiationError(
									ex.getLocalizedMessage());
						} catch (IllegalAccessException ex) {
							throw new IllegalAccessError(
									ex.getLocalizedMessage());
						}
						if (editor != null) {
							String header = (csvColumn != null && csvColumn
									.header().length() > 0) ? csvColumn
									.header() : field.getName();
							CSVFieldHandler	handler = new CSVFieldHandler(csvColumn.order(), header , field, editor);
							handlers.add(handler);
							mapping.put(header, handler);
						} else {
							// If no default or specified editor available check
							// if this is CSVEntity child class
							CSVEntity csvEntity = field.getType().getAnnotation(CSVEntity.class);
							if (csvEntity != null) {
								CSVPojoHandler handler = new CSVPojoHandler();
								PojoChildField child = new PojoChildField(csvColumn.header(), csvColumn.order(), field, handler, csvEntity);
								handler.initialize(line, field.getType());
								children.add(child);
							} 
						}
					}
				}
				//Sort the Getters and Setters Into a map with Col Order for them
				Map<Integer, CSVColumnMethodAnnotations> methodMap = new HashMap<Integer, CSVColumnMethodAnnotations>();
				for (Method method : c.getDeclaredMethods()) {
					CSVColumnGetter csvColumnGetter = method.getAnnotation(CSVColumnGetter.class);
					if(csvColumnGetter == null)
						continue;
					boolean isCSVEntity = false;
					if (method.getReturnType().getAnnotation(CSVEntity.class) != null) {
						isCSVEntity = true;
					}
					CSVColumnMethodAnnotations annos = methodMap.get(csvColumnGetter.order());
					if(annos != null){
						annos.setGetterAnnotation(csvColumnGetter);
						annos.setGetter(method);
						annos.setCSVEntity(isCSVEntity);
					}else{
						//Create new one
						annos = new CSVColumnMethodAnnotations(csvColumnGetter, method, null, null, isCSVEntity);
						methodMap.put(csvColumnGetter.order(), annos);
					}
					
				}
				for (Method method : c.getDeclaredMethods()) {
					CSVColumnSetter csvColumnSetter = method.getAnnotation(CSVColumnSetter.class);
					if(csvColumnSetter == null)
						continue;
					boolean isCSVEntity = false;
					if (method.getReturnType().getAnnotation(CSVEntity.class) != null) {
						isCSVEntity = true;
					}
					CSVColumnMethodAnnotations annos = methodMap.get(csvColumnSetter.order());
					if(annos != null){
						annos.setSetterAnnotation(csvColumnSetter);
						annos.setSetter(method);
					}else{
						//Create new one
						annos = new CSVColumnMethodAnnotations(null, null, csvColumnSetter, method, isCSVEntity);
						methodMap.put(csvColumnSetter.order(), annos);
					}
					
				}
				//Now map the matched methods
				Iterator<Integer> it = methodMap.keySet().iterator();
				while(it.hasNext()){
					Integer order = it.next();
					CSVColumnMethodAnnotations method = methodMap.get(order);
					CSVColumnGetter csvColumnGetter = method.getGetterAnnotation();
					CSVColumnSetter csvColumnSetter = method.getSetterAnnotation();
					// Check that the field show be used (not transient and not hidden)
					//Getter First
					if (!Modifier.isTransient(method.getGetter().getModifiers())
							&& (csvColumnGetter == null || !csvColumnGetter
									.hidden())) {
						PropertyEditor editor;
						try {
							// Try to determine PropertyEditor for field
							editor = (csvColumnGetter == null || csvColumnGetter
									.editor() == CSVPropertyEditor.class) ? PropertyEditorManager
									.findEditor(method.getGetter().getReturnType())
									: csvColumnGetter.editor().newInstance();
						} catch (InstantiationException ex) {
							throw new InstantiationError(
									ex.getLocalizedMessage());
						} catch (IllegalAccessException ex) {
							throw new IllegalAccessError(
									ex.getLocalizedMessage());
						}
						if (editor != null) {
							String header = (csvColumnGetter != null && csvColumnGetter
									.header().length() > 0) ? csvColumnGetter
									.header() : method.getGetter().getName();
							CSVMethodHandler handler = new CSVMethodHandler(csvColumnGetter.order(), header, method.getGetter(), method.getSetter(), editor);
							handlers.add(handler);
							mapping.put(header, handler);
						} else {
							// If no default or specified editor available check
							// if this is CSVEntity child class
							CSVEntity csvEntity = method.getGetter().getReturnType().getAnnotation(CSVEntity.class);
							if (csvEntity != null) {
								CSVPojoHandler handler = new CSVPojoHandler();
								PojoChildMethod child = new PojoChildMethod(csvColumnGetter.header(), order, method.getGetter(), method.getSetter(), handler, csvEntity);
								children.add(child);
								
								//Check Runtime Type for return value
								if(csvEntity.derived()){
									ModelDefinition definition = null;
									if(line.length > method.getGetterAnnotation().order())
										definition = this.findModelDefinition(line[method.getGetterAnnotation().order()]);
									if(definition != null){
										handler.initialize(line, definition.createModel());
										editor = new CsvEntityAnnotationPropertyEditor(csvEntity.typeName());
										CSVMethodHandler methodHandler = new CSVMethodHandler(csvColumnGetter.order(), csvColumnGetter.header(), method.getGetter(), method.getSetter(), editor);
										handlers.add(methodHandler);
										mapping.put(csvColumnGetter.header(), methodHandler);
									}
								}else{
									if(!csvEntity.typeName().equals("")){
										ModelDefinition definition = this.findModelDefinition(csvEntity.typeName());
										handler.initialize(line, definition.createModel());
										editor = new CsvEntityAnnotationPropertyEditor(csvEntity.typeName());
										CSVMethodHandler methodHandler = new CSVMethodHandler(csvColumnGetter.order(), csvColumnGetter.header(), method.getGetter(), method.getSetter(), editor);
										handlers.add(methodHandler);
										mapping.put(csvColumnGetter.header(), methodHandler);

									}
								}

							} else {
								throw new IllegalArgumentException(
										"The method: "
												+ c.getName()
												+ "."
												+ method.getGetter().getName()
												+ " of type: "
												+ method.getGetter().getReturnType().getName()
												+ " is not a CSVEntity or has no PropertyEditor available.");
							}
						}
					}
				}
			}
		}
		// Sort children so we always output them in the same order
		Collections.sort(this.children);
	}

	/**
	 * Extract the mapping from an annoteted pojo class.
	 *
	 * Fields that are not matched by a PropertyEditor is check if it is an
	 * CSVEntity child. CSVEntity children will be sorted to make sure output
	 * will be deterministic.
	 *
	 * Will throw IllegalArgumentException if a field can't be handled
	 * correctly.
	 *
	 * @param clazz
	 *            The pojo class
	 * @throws NoSupportingModelException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void buildMapping(T instance) throws NoSupportingModelException {
		for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
			if (c.getAnnotation(CSVEntity.class) != null) {
				for (Field field : c.getDeclaredFields()) {
					CSVColumn csvColumn = field.getAnnotation(CSVColumn.class);
					if(csvColumn == null)
						continue;
					// Check that the field show be used (not transient and not
					// hidden)
					if (!Modifier.isTransient(field.getModifiers())
							&& (csvColumn == null || !csvColumn.hidden())) {
						PropertyEditor editor;
						try {
							// Try to determine PropertyEditor for field
							editor = (csvColumn == null || csvColumn.editor() == CSVPropertyEditor.class) ? PropertyEditorManager
									.findEditor(field.getType()) : csvColumn
									.editor().newInstance();
						} catch (InstantiationException ex) {
							throw new InstantiationError(
									ex.getLocalizedMessage());
						} catch (IllegalAccessException ex) {
							throw new IllegalAccessError(
									ex.getLocalizedMessage());
						}
						if (editor != null) {
							String header = (csvColumn != null && csvColumn
									.header().length() > 0) ? csvColumn
									.header() : field.getName();
							CSVFieldHandler	handler = new CSVFieldHandler(csvColumn.order(), header , field, editor);
							handlers.add(handler);
							mapping.put(header, handler);
						} else {
							// If no default or specified editor available check
							// if this is CSVEntity child class
							CSVEntity csvEntity = field.getType().getAnnotation(CSVEntity.class);
							if (csvEntity != null) {
								CSVPojoHandler handler = new CSVPojoHandler();
								PojoChildField child = new PojoChildField(csvColumn.header(), csvColumn.order(), field, handler, csvEntity);
								handler.initialize(child.getValue(instance));
								children.add(child);
							} //Allow un-annotated Members to be ignored else {
//								throw new IllegalArgumentException(
//										"The field: "
//												+ c.getName()
//												+ "."
//												+ field.getName()
//												+ " of type: "
//												+ field.getType().getName()
//												+ " is not a CSVEntity or has no PropertyEditor available.");
//							}
						}
					}
				}
				//Sort the Getters and Setters Into a map with Col Order for them
				Map<Integer, CSVColumnMethodAnnotations> methodMap = new HashMap<Integer, CSVColumnMethodAnnotations>();
				for (Method method : c.getDeclaredMethods()) {
					CSVColumnGetter csvColumnGetter = method.getAnnotation(CSVColumnGetter.class);
					if(csvColumnGetter == null)
						continue;
					boolean isCSVEntity = false;
					if (method.getReturnType().getAnnotation(CSVEntity.class) != null) {
						isCSVEntity = true;
					}
					CSVColumnMethodAnnotations annos = methodMap.get(csvColumnGetter.order());
					if(annos != null){
						annos.setGetterAnnotation(csvColumnGetter);
						annos.setGetter(method);
						annos.setCSVEntity(isCSVEntity);
					}else{
						//Create new one
						annos = new CSVColumnMethodAnnotations(csvColumnGetter, method, null, null, isCSVEntity);
						methodMap.put(csvColumnGetter.order(), annos);
					}
					
				}
				for (Method method : c.getDeclaredMethods()) {
					CSVColumnSetter csvColumnSetter = method.getAnnotation(CSVColumnSetter.class);
					if(csvColumnSetter == null)
						continue;
					boolean isCSVEntity = false;
					if (method.getReturnType().getAnnotation(CSVEntity.class) != null) {
						isCSVEntity = true;
					}
					CSVColumnMethodAnnotations annos = methodMap.get(csvColumnSetter.order());
					if(annos != null){
						annos.setSetterAnnotation(csvColumnSetter);
						annos.setSetter(method);
					}else{
						//Create new one
						annos = new CSVColumnMethodAnnotations(null, null, csvColumnSetter, method, isCSVEntity);
						methodMap.put(csvColumnSetter.order(), annos);
					}
					
				}
				//Now map the matched methods
				Iterator<Integer> it = methodMap.keySet().iterator();
				while(it.hasNext()){
					Integer order = it.next();
					CSVColumnMethodAnnotations method = methodMap.get(order);
					CSVColumnGetter csvColumnGetter = method.getGetterAnnotation();
					CSVColumnSetter csvColumnSetter = method.getSetterAnnotation();
					// Check that the field show be used (not transient and not hidden)
					//Getter First
					if (!Modifier.isTransient(method.getGetter().getModifiers())
							&& (csvColumnGetter == null || !csvColumnGetter
									.hidden())) {
						PropertyEditor editor;
						try {
							// Try to determine PropertyEditor for field
							editor = (csvColumnGetter == null || csvColumnGetter
									.editor() == CSVPropertyEditor.class) ? PropertyEditorManager
									.findEditor(method.getGetter().getReturnType())
									: csvColumnGetter.editor().newInstance();
						} catch (InstantiationException ex) {
							throw new InstantiationError(
									ex.getLocalizedMessage());
						} catch (IllegalAccessException ex) {
							throw new IllegalAccessError(
									ex.getLocalizedMessage());
						}
						if (editor != null) {
							String header = (csvColumnGetter != null && csvColumnGetter
									.header().length() > 0) ? csvColumnGetter
									.header() : method.getGetter().getName();
							CSVMethodHandler handler = new CSVMethodHandler(csvColumnGetter.order(), header, method.getGetter(), method.getSetter(), editor);
							handlers.add(handler);
							mapping.put(header, handler);
						} else {
							// If no default or specified editor available check
							// if this is CSVEntity child class
							CSVEntity csvEntity = method.getGetter().getReturnType().getAnnotation(CSVEntity.class);
							if (csvEntity != null) {
								CSVPojoHandler handler = new CSVPojoHandler();
								PojoChildMethod child = new PojoChildMethod(csvColumnGetter.header(), order, method.getGetter(), method.getSetter(), handler, csvEntity);
								Object value = child.getValue(instance);
								if(value != null){
									//We can't do anything with a null object anyway
									handler.initialize(value);
									children.add(child);
									//Check Runtime Type for return value
									csvEntity = value.getClass().getAnnotation(CSVEntity.class);
									if(!csvEntity.typeName().equals("")){
										editor = new CsvEntityAnnotationPropertyEditor(csvEntity.typeName());
										CSVMethodHandler methodHandler = new CSVMethodHandler(csvColumnGetter.order(), csvColumnGetter.header(), method.getGetter(), method.getSetter(), editor);
										handlers.add(methodHandler);
										mapping.put(csvColumnGetter.header(), methodHandler);
									}
								}

							} else {
								throw new IllegalArgumentException(
										"The method: "
												+ c.getName()
												+ "."
												+ method.getGetter().getName()
												+ " of type: "
												+ method.getGetter().getReturnType().getName()
												+ " is not a CSVEntity or has no PropertyEditor available.");
							}
						}
					}
				}
			}
		}
		// Sort children so we always output them in the same order
		Collections.sort(this.children);
	}

	/**
	 * Helper class to handle the child entities of a pojo
	 */
	private static class PojoChildField extends PojoChild implements Comparable<PojoChild> {

		private final Field field;

		public PojoChildField(String header, Integer order, Field field, CSVPojoHandler<?> pojoHandler, CSVEntity csvEntityAnnotation) {
			super(header, order, pojoHandler, csvEntityAnnotation);
			this.field = field;
		}

		public Field getField() {
			return field;
		}
		/**
		 * Get the value object from a specific field in an object Will try to
		 * read private fields.
		 *
		 * @param field
		 *            The field to extract
		 * @param obj
		 *            Object to extrace the value from
		 * @return The current value/Object stored in the field
		 */
		public Object getValue(Object obj) {
			try {
				return field.get(obj);
			} catch (IllegalAccessException _) {
				// Allow getting of private fields and try again
				field.setAccessible(true);
				try {
					return field.get(obj);
				} catch (IllegalAccessException ex) {
					throw new IllegalAccessError(ex.getMessage());
				}
			}
		}

		/**
		 * Set the value of a specific field in an object. Will try to write to
		 * private fields.
		 *
		 * @param field
		 *            The field to set
		 * @param obj
		 *            The object to update
		 * @param value
		 *            The value to write, must be of the correct object type
		 */
		public void setValue(Object obj, Object value) {
			try {
				field.set(obj, value);
			} catch (IllegalAccessException _) {
				// Allow setting of private fields and try again
				field.setAccessible(true);
				try {
					field.set(obj, value);
				} catch (IllegalAccessException ex) {
					throw new IllegalAccessError(ex.getMessage());
				}
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(PojoChild o) {
			return field.getName().compareTo(((PojoChildField)o).getField().getName());
		}

	}
	/**
	 * Helper class to handle the child entities of a pojo
	 */
	private static class PojoChildMethod extends PojoChild implements Comparable<PojoChild> {

		private final Method getter;
		private final Method setter;

		public PojoChildMethod(String header, Integer order, Method getter, Method setter, CSVPojoHandler<?> pojoHandler, CSVEntity csvEntityAnnotation) {
			super(header, order, pojoHandler, csvEntityAnnotation);
			this.getter = getter;
			this.setter = setter;
		}
		
		/**
		 * Set the value of a specific field in an object. Will try to write to
		 * private fields.
		 *
		 * @param field
		 *            The field to set
		 * @param obj
		 *            The object to update
		 * @param value
		 *            The value to write, must be of the correct object type
		 * @throws InvocationTargetException
		 * @throws IllegalArgumentException
		 */
		public void setValue(Object obj, Object value) {
			if (setter == null)
				return; // Nothing to do
			try {
				setter.invoke(obj, value);
			} catch (IllegalAccessException _) {
				// Allow setting of private fields and try again
				setter.setAccessible(true);
				try {
					setter.invoke(obj, value);
				} catch (IllegalAccessException ex) {
					throw new IllegalAccessError(ex.getMessage());
				} catch (IllegalArgumentException e) {
					throw new IllegalAccessError(e.getMessage());
				} catch (InvocationTargetException e) {
					throw new IllegalAccessError(e.getMessage());
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalAccessError(e.getMessage());
			} catch (InvocationTargetException e) {
				throw new IllegalAccessError(e.getMessage());
			}
		}

		/**
		 * Get the value object from a specific field in an object Will try to
		 * read private fields.
		 *
		 * @param field
		 *            The field to extract
		 * @param obj
		 *            Object to extrace the value from
		 * @return The current value/Object stored in the field
		 * @throws InvocationTargetException
		 * @throws IllegalArgumentException
		 */
		public Object getValue(Object obj) {
			if (getter == null)
				return null;
			try {
				return getter.invoke(obj);
			} catch (IllegalAccessException _) {
				// Allow getting of private fields and try again
				getter.setAccessible(true);
				try {
					return getter.invoke(obj);
				} catch (IllegalAccessException ex) {
					throw new IllegalAccessError(ex.getMessage());
				} catch (IllegalArgumentException e) {
					throw new IllegalAccessError(e.getMessage());
				} catch (InvocationTargetException e) {
					throw new IllegalAccessError(e.getMessage());
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalAccessError(e.getMessage());
			} catch (InvocationTargetException e) {
				throw new IllegalAccessError(e.getMessage());
			}
		}

	}

	/**
	 * Helper class to handle the child entities of a pojo
	 */
	private abstract static class PojoChild implements Comparable<PojoChild> {

		private final String header;
		private final CSVPojoHandler<?> pojoHandler;
		private final Integer order;
		private final CSVEntity csvEntityAnnotation;
		

		public PojoChild(String header, Integer order, CSVPojoHandler<?> pojoHandler, CSVEntity csvEntityAnnotation) {
			this.header = header;
			this.order = order;
			this.pojoHandler = pojoHandler;
			this.csvEntityAnnotation = csvEntityAnnotation;
		}

		public String getHeader(){
			return header;
		}
		public Integer getOrder(){
			return this.order;
		}
		public CSVPojoHandler<?> getPojoHandler() {
			return pojoHandler;
		}
		public CSVEntity getCsvEntityAnnotation(){
			return this.csvEntityAnnotation;
		}
		public abstract void setValue(Object obj, Object value);

		public abstract Object getValue(Object obj);
		
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(PojoChild o) {
			return this.order - o.getOrder();
		}
	}
	
	/**
	 * Helper class to handle the mapping of field to propertyeditor
	 */
	private static class CSVFieldHandler extends CSVDataHandler {

		private Field field;

		public CSVFieldHandler(Integer order, String header, Field field, PropertyEditor editor) {
			super(order, header , editor);
			this.field = field;
		}

		public Field getField() {
			return field;
		}

		/**
		 * Get the value object from a specific field in an object Will try to
		 * read private fields.
		 *
		 * @param field
		 *            The field to extract
		 * @param obj
		 *            Object to extrace the value from
		 * @return The current value/Object stored in the field
		 */
		public Object getValue(Object obj) {
			try {
				return field.get(obj);
			} catch (IllegalAccessException _) {
				// Allow getting of private fields and try again
				field.setAccessible(true);
				try {
					return field.get(obj);
				} catch (IllegalAccessException ex) {
					throw new IllegalAccessError(ex.getMessage());
				}
			}
		}

		/**
		 * Set the value of a specific field in an object. Will try to write to
		 * private fields.
		 *
		 * @param field
		 *            The field to set
		 * @param obj
		 *            The object to update
		 * @param value
		 *            The value to write, must be of the correct object type
		 */
		public void setValue(Object obj, Object value) {
			try {
				field.set(obj, value);
			} catch (IllegalAccessException _) {
				// Allow setting of private fields and try again
				field.setAccessible(true);
				try {
					field.set(obj, value);
				} catch (IllegalAccessException ex) {
					throw new IllegalAccessError(ex.getMessage());
				}
			}
		}

	}

	private static class CSVMethodHandler extends CSVDataHandler {

		private Method getter;
		private Method setter;

		public CSVMethodHandler(Integer order, String header, Method getter, Method setter,
				PropertyEditor editor) {
			super(order, header, editor);
			this.getter = getter;
			this.setter = setter;
		}

		public Method getGetter() {
			return getter;
		}

		public void setGetter(Method getter) {
			this.getter = getter;
		}

		public Method getSetter() {
			return setter;
		}

		public void setSetter(Method setter) {
			this.setter = setter;
		}

		/**
		 * Set the value of a specific field in an object. Will try to write to
		 * private fields.
		 *
		 * @param field
		 *            The field to set
		 * @param obj
		 *            The object to update
		 * @param value
		 *            The value to write, must be of the correct object type
		 * @throws InvocationTargetException
		 * @throws IllegalArgumentException
		 */
		public void setValue(Object obj, Object value) {
			if (setter == null)
				return; // Nothing to do
			try {
				setter.invoke(obj, value);
			} catch (IllegalAccessException _) {
				// Allow setting of private fields and try again
				setter.setAccessible(true);
				try {
					setter.invoke(obj, value);
				} catch (IllegalAccessException ex) {
					throw new IllegalAccessError(ex.getMessage());
				} catch (IllegalArgumentException e) {
					throw new IllegalAccessError(e.getMessage());
				} catch (InvocationTargetException e) {
					throw new IllegalAccessError(e.getMessage());
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalAccessError(e.getMessage());
			} catch (InvocationTargetException e) {
				throw new IllegalAccessError(e.getMessage());
			}
		}

		/**
		 * Get the value object from a specific field in an object Will try to
		 * read private fields.
		 *
		 * @param field
		 *            The field to extract
		 * @param obj
		 *            Object to extrace the value from
		 * @return The current value/Object stored in the field
		 * @throws InvocationTargetException
		 * @throws IllegalArgumentException
		 */
		public Object getValue(Object obj) {
			if (getter == null)
				return null;
			try {
				return getter.invoke(obj);
			} catch (IllegalAccessException _) {
				// Allow getting of private fields and try again
				getter.setAccessible(true);
				try {
					return getter.invoke(obj);
				} catch (IllegalAccessException ex) {
					throw new IllegalAccessError(ex.getMessage());
				} catch (IllegalArgumentException e) {
					throw new IllegalAccessError(e.getMessage());
				} catch (InvocationTargetException e) {
					throw new IllegalAccessError(e.getMessage());
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalAccessError(e.getMessage());
			} catch (InvocationTargetException e) {
				throw new IllegalAccessError(e.getMessage());
			}
		}

	}

	private static abstract class CSVDataHandler implements Comparable<CSVDataHandler>{

		private PropertyEditor editor;
		private Integer order;
		private String header;
		
		public CSVDataHandler(Integer order, String header, PropertyEditor editor) {
			this.order = order;
			this.editor = editor;
			this.header = header;
		}

		public Integer getOrder(){
			return this.order;
		}
		public String getHeader(){
			return header;
		}
		public PropertyEditor getEditor() {
			return editor;
		}
		

		public abstract void setValue(Object obj, Object value);

		public abstract Object getValue(Object obj);
		
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(CSVDataHandler o) {
			return this.order - o.getOrder();
		}

	}
	
	private static class CSVColumnMethodAnnotations {
		private CSVColumnGetter getterAnnotation;
		private Method getter;
		private CSVColumnSetter setterAnnotation;
		private Method setter;
		private PropertyEditor editor;
		private boolean isCSVEntity;
		
		public CSVColumnMethodAnnotations(CSVColumnGetter getterAnnotation, Method getter, CSVColumnSetter setterAnnotation, Method setter, boolean isCSVEntity){
			this.getterAnnotation = getterAnnotation;
			this.getter = getter;
			this.setterAnnotation = setterAnnotation;
			this.setter = setter;
			this.isCSVEntity = isCSVEntity;
		}

		public CSVColumnGetter getGetterAnnotation() {
			return getterAnnotation;
		}

		public void setGetterAnnotation(CSVColumnGetter getterAnnotation) {
			this.getterAnnotation = getterAnnotation;
		}

		public Method getGetter() {
			return getter;
		}

		public void setGetter(Method getter) {
			this.getter = getter;
		}

		public CSVColumnSetter getSetterAnnotation() {
			return setterAnnotation;
		}

		public void setSetterAnnotation(CSVColumnSetter setterAnnotation) {
			this.setterAnnotation = setterAnnotation;
		}

		public Method getSetter() {
			return setter;
		}

		public void setSetter(Method setter) {
			this.setter = setter;
		}
		
		public boolean isCSVEntity() {
			return isCSVEntity;
		}

		public void setCSVEntity(boolean isCSVEntity) {
			this.isCSVEntity = isCSVEntity;
		}

		public PropertyEditor getEditor() {
			return editor;
		}

		public void setEditor(PropertyEditor editor) {
			this.editor = editor;
		}

		

		
	}
	
}
