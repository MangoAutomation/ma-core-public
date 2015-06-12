/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.serotonin.m2m2.module.ModelDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;
import com.serotonin.m2m2.web.mvc.rest.v1.model.email.AddressEntryModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.email.EmailRecipientModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.email.MailingListModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.email.UserEntryModel;

/**
 * @author Terry Packer
 *
 */
public class EmailRecipientModelDeserializer extends StdDeserializer<EmailRecipientModel<?>>{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		

		/**
		 * 
		 */
		protected EmailRecipientModelDeserializer() {
			super(EmailRecipientModel.class);
		}

		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public EmailRecipientModel<?> deserialize(JsonParser jp,
				DeserializationContext ctxt) throws IOException,
				JsonProcessingException {
			ObjectMapper mapper = (ObjectMapper) jp.getCodec();  
			JsonNode tree = jp.readValueAsTree();
			String type = tree.get("type").asText();
			int code = EmailRecipient.TYPE_CODES.getId(type);
			switch(code){
			case EmailRecipient.TYPE_ADDRESS:
				return (AddressEntryModel) mapper.treeToValue(tree, AddressEntryModel.class);
			case EmailRecipient.TYPE_MAILING_LIST:
				return (MailingListModel) mapper.treeToValue(tree, MailingListModel.class);
			case EmailRecipient.TYPE_USER:
				return (UserEntryModel) mapper.treeToValue(tree, UserEntryModel.class);
			default:
				throw new ModelNotFoundException(type);
			}
		}

		/**
		 * @return
		 */
		public ModelDefinition findModelDefinition(String typeName) throws ModelNotFoundException{
			List<ModelDefinition> definitions = ModuleRegistry.getModelDefinitions();
			for(ModelDefinition definition : definitions){
				if(definition.getModelTypeName().equalsIgnoreCase(typeName))
					return definition;
			}
			throw new ModelNotFoundException(typeName);
		}


}
