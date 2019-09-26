package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.dwr.emport.Importer;

/**
 * Import Users except for the current user
 *
 * @author Matthew Lohbihler
 * @author Terry Packer
 */
public class UserImporter extends Importer {

    public UserImporter(JsonObject json, PermissionHolder user) {
        super(json, user);
    }

    @Override
    protected void importImpl() {
        String username = json.getString("username");
        if (StringUtils.isBlank(username))
            addFailureMessage("emport.user.username");
        else if((this.user != null)&&(StringUtils.equals(this.user.getPermissionHolderName(), username))){
            addFailureMessage("emport.user.cannotImportCurrentUser");
        }else {
            User existing = ctx.getUsersService().get(username, user);
            User imported;
            if (existing == null) {
                imported = new User();
                imported.setUsername(username);
            }else {
                imported = existing.copy();
            }

            try {
                ctx.getReader().readInto(imported, json);

                // check if a password algorithm was specified, if not assume it was SHA-1 (legacy JSON format)
                if (imported.assumeSha1Algorithm()) {
                    // Password had no algorithm prefix
                    // Can assume this JSON was exported by mango < 2.8.x
                    // Check if they have receiveEvents==NONE, as IGNORE is the new NONE
                    if(imported.getReceiveAlarmEmails() == AlarmLevels.NONE)
                        imported.setReceiveAlarmEmails(AlarmLevels.IGNORE);
                }

                try {
                    if(existing == null) {
                        ctx.getUsersService().insert(imported, user);
                        addSuccessMessage(true, "emport.user.prefix", username);
                    }else {
                        ctx.getUsersService().update(existing, imported, user);
                        addSuccessMessage(false, "emport.user.prefix", username);
                    }
                }catch(ValidationException e) {
                    setValidationMessages(e.getValidationResult(), "emport.user.prefix", username);
                }catch(PermissionException e){
                    addFailureMessage("emport.user.prefix", username, e.getTranslatableMessage());
                }
            }
            catch (TranslatableJsonException e) {
                addFailureMessage("emport.user.prefix", username, e.getMsg());
            }
            catch (JsonException e) {
                addFailureMessage("emport.user.prefix", username, getJsonExceptionMessage(e));
            }
        }
    }
}
