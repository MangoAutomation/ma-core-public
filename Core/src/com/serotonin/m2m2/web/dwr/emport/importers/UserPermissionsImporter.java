package com.serotonin.m2m2.web.dwr.emport.importers;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class UserPermissionsImporter extends Importer {
    public UserPermissionsImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        // This method uses user objects which have already been validated.
        String username = json.getString("username");
        User user = ctx.getUserDao().getUser(username);

        try {
            user.jsonDeserializePermissions(ctx.getReader(), json);
            ctx.getUserDao().saveUser(user);
            addSuccessMessage(false, "emport.userPermission.prefix", username);
        }
        catch (TranslatableJsonException e) {
            addFailureMessage("emport.userPermission.prefix", username, e.getMsg());
        }
        catch (JsonException e) {
            addFailureMessage("emport.userPermission.prefix", username, getJsonExceptionMessage(e));
        }
    }
}
