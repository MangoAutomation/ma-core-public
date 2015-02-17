package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class UserImporter extends Importer {
    public UserImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        String username = json.getString("username");
        if (StringUtils.isBlank(username))
            addFailureMessage("emport.user.username");
        else {
            User user = ctx.getUserDao().getUser(username);
            if (user == null) {
                user = new User();
                user.setUsername(username);
                user.setPassword(Common.encrypt(username));
            }

            try {
                ctx.getReader().readInto(user, json);

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
