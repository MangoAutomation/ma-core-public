/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;
import com.serotonin.util.SerializationHelper;

/**
 * 3.6.0 Schema Update
 * - Add permissions to mailing lists
 * - Add session expiration settings to User
 * - Upgrade Email Event Handlers to have subject include set for legacy compatibility
 * - Upgrade all event handlers to have an alias and make that column required
 *
 * @author Terry Packer
 */
public class Upgrade26 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {

        //First upgrade all event handlers to have a name and use the correct subject in emails
        List<AbstractEventHandlerVO> handlers = this.ejt.query("SELECT id, xid, alias, eventHandlerType, data FROM eventHandlers", new Upgrade26EventHandlerRowMapper());

        for(AbstractEventHandlerVO handler : handlers) {

            if(handler instanceof EmailEventHandlerVO) {
                EmailEventHandlerVO vo = (EmailEventHandlerVO)handler;
                if(StringUtils.isEmpty(vo.getName())) {
                    vo.setName(new TranslatableMessage("eventHandlers.type.emailHandler").translate(Common.getTranslations()) + " - " + vo.getXid());
                    vo.setSubject(EmailEventHandlerVO.SUBJECT_INCLUDE_EVENT_MESSAGE);
                }else {
                    vo.setSubject(EmailEventHandlerVO.SUBJECT_INCLUDE_NAME);
                }
            }else if(handler instanceof SetPointEventHandlerVO){
                if(StringUtils.isEmpty(handler.getName()))
                    handler.setName(new TranslatableMessage("eventHandlers.type.setPointHandler").translate(Common.getTranslations()) + " - " + handler.getXid());
            }else if(handler instanceof ProcessEventHandlerVO) {
                if(StringUtils.isEmpty(handler.getName()))
                    handler.setName(new TranslatableMessage("eventHandlers.type.processHandler").translate(Common.getTranslations()) + " - " + handler.getXid());
            }
            this.ejt.update("UPDATE eventHandlers SET alias=?,data=? WHERE id=?", ps -> {
                ps.setString(1, handler.getName());
                ps.setBinaryStream(2, SerializationHelper.writeObject(handler));
                ps.setInt(3, handler.getId());
            });
        }

        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.MYSQL.name(), mysql);
        scripts.put(DatabaseType.H2.name(), sql);
        scripts.put(DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseType.POSTGRES.name(), sql);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "27";
    }

    private String[] sql = new String[]{
            "ALTER TABLE mailingLists ADD COLUMN readPermission varchar(255);",
            "ALTER TABLE mailingLists ADD COLUMN editPermission varchar(255);",
            "ALTER TABLE users ADD COLUMN sessionExpirationOverride char(1);",
            "ALTER TABLE users ADD COLUMN sessionExpirationPeriods int;",
            "ALTER TABLE users ADD COLUMN sessionExpirationPeriodType varchar(25);",
            "ALTER TABLE eventHandlers ALTER COLUMN alias varchar(255) not null;",
    };

    private String[] mysql = new String[]{
            "ALTER TABLE mailingLists ADD COLUMN readPermission varchar(255);",
            "ALTER TABLE mailingLists ADD COLUMN editPermission varchar(255);",
            "ALTER TABLE users ADD COLUMN sessionExpirationOverride char(1);",
            "ALTER TABLE users ADD COLUMN sessionExpirationPeriods int;",
            "ALTER TABLE users ADD COLUMN sessionExpirationPeriodType varchar(25);",
            "ALTER TABLE eventHandlers MODIFY COLUMN alias varchar(255) not null;",
    };

    private String[] mssql = new String[]{
            "ALTER TABLE mailingLists ADD COLUMN readPermission nvarchar(255);",
            "ALTER TABLE mailingLists ADD COLUMN editPermission nvarchar(255);",
            "ALTER TABLE users ADD COLUMN sessionExpirationOverride char(1);",
            "ALTER TABLE users ADD COLUMN sessionExpirationPeriods int;",
            "ALTER TABLE users ADD COLUMN sessionExpirationPeriodType nvarchar(25);",
            "ALTER TABLE eventHandlers ALTER COLUMN alias nvarchar(255) not null;",
    };

    private class Upgrade26EventHandlerRowMapper implements RowMapper<AbstractEventHandlerVO>{

        @Override
        public AbstractEventHandlerVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            AbstractEventHandlerVO h = (AbstractEventHandlerVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(5));
            h.setId(rs.getInt(1));
            h.setXid(rs.getString(2));
            h.setAlias(rs.getString(3));
            h.setDefinition(ModuleRegistry.getEventHandlerDefinition(rs.getString(4)));
            return h;
        }
    }
}
