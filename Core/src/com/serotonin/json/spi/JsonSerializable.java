package com.serotonin.json.spi;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * This interface can be used to provide custom conversion code within a class itself.
 *
 * @author Matthew Lohbihler
 */
public interface JsonSerializable {
    /**
     * Write the implementing object into the given object writer.
     *
     * @param writer
     *            the object writer to which to write.
     * @throws IOException
     * @throws JsonException
     */
    void jsonWrite(ObjectWriter writer) throws IOException, JsonException;

    /**
     * Read this object from the given JsonObject instance. This object will have been created by a registered
     * ObjectFactory.
     *
     * @param reader
     *            the JSON reader
     * @param jsonObject
     *            the JSON object from which to read attributes for this object
     * @throws JsonException
     */
    void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException;

    /**
     * Helper to read and merge legacy JSON permissions which are
     *  just a list of role names
     * @param existing
     * @param jsonObject
     * @return
     * @throws TranslatableJsonException
     */
    default Set<Role> readLegacyPermissions(String permissionName, Set<Role> existing, JsonObject jsonObject) throws TranslatableJsonException {
        //Legacy permissions support
        if(jsonObject.containsKey(permissionName)) {
            Set<Role> roles;
            if(existing != null) {
                roles = new HashSet<>(existing);
            }else {
                roles = new HashSet<>();
            }
            //Try string format
            try {
                String groups = jsonObject.getString(permissionName);
                for(String permission : PermissionService.explodeLegacyPermissionGroups(groups)) {
                    RoleVO role = RoleDao.getInstance().getByXid(permission);
                    if(role != null) {
                        roles.add(role.getRole());
                    } else {
                        throw new TranslatableJsonException("emport.error.missingRole", permission, permissionName);
                    }
                }

            }catch(ClassCastException e) {
                //Might be an array
                //Try array
                try {
                    JsonArray permissions = jsonObject.getJsonArray(permissionName);
                    for(JsonValue jv : permissions) {
                        RoleVO role = RoleDao.getInstance().getByXid(jv.toString());
                        if(role != null) {
                            roles.add(role.getRole());
                        } else {
                            throw new TranslatableJsonException("emport.error.missingRole", jv.toString(), permissionName);
                        }
                    }
                }catch(ClassCastException e2) {
                    throw e2; //Give up
                }
            }
            return Collections.unmodifiableSet(roles);
        }
        return Collections.emptySet();
    }
}
