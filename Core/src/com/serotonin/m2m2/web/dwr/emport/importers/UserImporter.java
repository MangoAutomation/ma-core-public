package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.dwr.emport.Importer;

/**
 * Import Users except for the current user
 *
 * @author Matthew Lohbihler
 * @author Terry Packer
 */
public class UserImporter extends Importer {

    private User currentUser;

    public UserImporter(User currentUser, JsonObject json) {
        super(json);
        this.currentUser = currentUser;
    }

    @Override
    protected void importImpl() {
        String username = json.getString("username");
        if (StringUtils.isBlank(username))
            addFailureMessage("emport.user.username");
        else if((this.currentUser != null)&&(StringUtils.equals(this.currentUser.getUsername(), username))){
            addFailureMessage("emport.user.cannotImportCurrentUser");
        }else {
            User user = ctx.getUserDao().getUser(username);
            if (user == null) {
                user = new User();
                user.setUsername(username);
                user.setPassword(Common.encrypt(username));
            }

            try {
                ctx.getReader().readInto(user, json);

                // check if a password algorithm was specified, if not assume it was SHA-1 (legacy JSON format)
                if (user.checkPasswordAlgorithm("SHA-1") == null) {
                    // Password had no algorithm prefix
                    // Can assume this JSON was exported by mango < 2.8.x
                    // Check if they have receiveEvents==NONE, as IGNORE is the new NONE
                    if(user.getReceiveAlarmEmails() == AlarmLevels.NONE)
                        user.setReceiveAlarmEmails(AlarmLevels.IGNORE);
                }

                // Now validate it. Use a new response object so we can distinguish errors in this user from other
                // errors.
                ProcessResult userResponse = new ProcessResult();
                user.validate(userResponse);
                if (userResponse.getHasMessages())
                    // Too bad.
                    setValidationMessages(userResponse, "emport.user.prefix", username);
                else {
                    // Sweet. Save it.
                    boolean isnew = user.getId() == Common.NEW_ID;
                    ctx.getUserDao().saveUser(user);
                    addSuccessMessage(isnew, "emport.user.prefix", username);
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
