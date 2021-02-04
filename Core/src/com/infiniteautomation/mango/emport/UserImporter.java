package com.infiniteautomation.mango.emport;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.LinkedAccount;
import com.serotonin.m2m2.vo.OAuth2LinkedAccount;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Import Users except for the current user
 *
 * @author Matthew Lohbihler
 * @author Terry Packer
 */
public class UserImporter extends Importer {

    private final UsersService usersService;
    private final PermissionHolder user;

    public UserImporter(JsonObject json, UsersService usersService, PermissionHolder user) {
        super(json);
        this.usersService = usersService;
        this.user = user;
    }

    @Override
    protected void importImpl() {
        String username = json.getString("username");
        if (StringUtils.isBlank(username))
            addFailureMessage("emport.user.username");
        else if((this.user != null)&&(StringUtils.equals(this.user.getPermissionHolderName(), username))){
            addFailureMessage("emport.user.cannotImportCurrentUser");
        }else {
            User existing = null;
            try {
                existing = usersService.get(username);
            }catch(NotFoundException e) {
                existing = null;
            }
            User imported;
            if (existing == null) {
                imported = new User();
                imported.setUsername(username);
            }else {
                imported = (User) existing.copy();
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
                        usersService.insert(imported);
                        addSuccessMessage(true, "emport.user.prefix", username);
                    }else {
                        usersService.update(existing, imported);
                        addSuccessMessage(false, "emport.user.prefix", username);
                    }
                    linkAccounts(imported.getId());
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

    @SuppressWarnings("unchecked")
    protected void linkAccounts(int userId) throws JsonException {
        TypeDefinition listOfAccountsType = new TypeDefinition(List.class, OAuth2LinkedAccount.class);
        JsonArray linkedAccounts = json.getJsonArray("linkedAccounts");
        if (linkedAccounts != null) {
            List<LinkedAccount> accounts = (List<LinkedAccount>) ctx.getReader().read(listOfAccountsType, linkedAccounts);
            usersService.updateLinkedAccounts(userId, accounts);
        }
    }
}
