package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade2 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseType.H2.name(), new String[0]);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "3";
    }

    private final String[] derbyScript = { //
    "alter table pointValues drop foreign key pointValuesFk1;", //
            "drop index pointValuesIdx1;", //
            "drop index pointValuesIdx2;", //
            "create index pointValuesIdx1 on pointValues (dataPointId, ts);", //

            "alter table pointValueAnnotations drop foreign key pointValueAnnotationsFk1;", //
            "alter table pointValueAnnotations add constraint pointValueAnnotationsPk primary key (pointValueId);", //
    };

    private final String[] mssqlScript = { //
    "alter table pointValues drop foreign key pointValuesFk1;", //
            "drop index pointValuesIdx1;", //
            "drop index pointValuesIdx2;", //
            "create index pointValuesIdx1 on pointValues (dataPointId, ts);", //

            "alter table pointValueAnnotations drop foreign key pointValueAnnotationsFk1;", //
            "alter table pointValueAnnotations add constraint pointValueAnnotationsPk primary key (pointValueId);", //
    };

    private final String[] mysqlScript = { //
    "alter table pointValueAnnotations drop foreign key pointValueAnnotationsFk1;", //
            "alter table pointValueAnnotations ENGINE = MyISAM;", //
            "alter table pointValueAnnotations add primary key (pointValueId);", //

            "alter table pointValues drop foreign key pointValuesFk1;", //
            "drop index pointValuesIdx1 on pointValues;", //
            "drop index pointValuesIdx2 on pointValues;", //
            "alter table pointValues ENGINE = MyISAM;", //
            "create index pointValuesIdx1 on pointValues (dataPointId, ts);", //
    };
}
