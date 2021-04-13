/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.db.dao;

import java.util.Date;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.db.tables.InstalledModules;
import com.serotonin.m2m2.module.Module;

public class InstalledModulesDao extends BaseDao {
    public static final InstalledModulesDao instance = new InstalledModulesDao();

    private final InstalledModules table = InstalledModules.INSTALLED_MODULES;

    public void removeModuleVersion(String name) {
        create.deleteFrom(table).where(table.name.eq(name)).execute();
    }

    public InstalledModule getInstalledModule(String name) {
        return create.select(table.fields()).from(table)
                .where(table.name.eq(name))
                .fetchOptional()
                .map(record -> {
                    InstalledModule module = new InstalledModule();
                    module.setName(record.get(table.name));
                    module.setVersion(Version.valueOf(record.get(table.version)));
                    module.setUpgradedDate(new Date(record.get(table.upgradedTimestamp)));
                    return module;
                })
                .orElse(null);
    }

    /**
     * Update a single module version to its latest
     * @param module
     */
    public void updateModuleVersion(Module module) {
        doInTransaction(txStatus -> {
            create.deleteFrom(table).where(table.name.eq(module.getName())).execute();

            create.insertInto(table)
                    .columns(table.name, table.version, table.upgradedTimestamp)
                    .values(module.getName(), module.getVersion().toString(),
                            module.getUpgradedDate().getTime())
                    .execute();
        });
    }

    public static class InstalledModule {
        String name;
        Version version;
        Date upgradedDate;
        Date buildDate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Version getVersion() {
            return version;
        }

        public void setVersion(Version version) {
            this.version = version;
        }

        public Date getUpgradedDate() {
            return upgradedDate;
        }

        public void setUpgradedDate(Date upgradedDate) {
            this.upgradedDate = upgradedDate;
        }

        public Date getBuildDate() {
            return buildDate;
        }

        public void setBuildDate(Date buildDate) {
            this.buildDate = buildDate;
        }
    }
}
