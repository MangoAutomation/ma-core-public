/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.time.Instant;
import java.util.Date;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.db.tables.InstalledModules;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

@Repository
public class InstalledModulesDao extends BaseDao {

    private final Logger log = LoggerFactory.getLogger(DataPointDao.class);
    private final InstalledModules table = InstalledModules.INSTALLED_MODULES;
    private final LazyInitSupplier<Instant> lastUpgrade = new LazyInitSupplier<>(this::computeLastUpgradeTime);

    /**
     *
     * @param definitions - autowired to ensure the PostConstruct will have all definitions as
     *                    beans when executing the upgrades
     */
    @Autowired
    public InstalledModulesDao(DatabaseProxy databaseProxy, List<ModuleElementDefinition> definitions) {
        super(databaseProxy);
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
                    module.setBuildDate(new Date(record.get(table.buildTimestamp)));
                    return module;
                })
                .orElse(null);
    }

    public void removeModuleVersion(String name) {
        create.deleteFrom(table).where(table.name.eq(name)).execute();
    }

    /**
     * Update a single module version to its latest
     */
    public void updateModuleVersion(Module module) {
        doInTransaction(txStatus -> {
            create.deleteFrom(table).where(table.name.eq(module.getName())).execute();

            create.insertInto(table)
                    .columns(table.name, table.version, table.upgradedTimestamp, table.buildTimestamp)
                    .values(module.getName(), module.getVersion().toString(),
                            module.getUpgradedDate().getTime(),
                            module.getBuildDate().getTime())
                    .execute();
        });
    }

    public Instant lastUpgradeTime() {
        return lastUpgrade.get();
    }

    private Instant computeLastUpgradeTime() {
        return create.select(DSL.max(table.upgradedTimestamp)).from(table)
                .fetchOptional()
                .map(record -> Instant.ofEpochMilli(record.get(0, long.class)))
                .orElseThrow();
    }

    @PostConstruct
    private void postConstruct() {
        // upgrade modules
        for (Module module : ModuleRegistry.getModules()) {
            try {
                InstalledModule installedModule = getInstalledModule(module.getName());
                if (module.upgrade(installedModule)) {
                    updateModuleVersion(module);
                }
            } catch (Exception e) {
                log.error("Module upgrade failed", e);
            }
        }
    }

    @PreDestroy
    private void preDestroy() {
        //Delete our module versions
        for (Module module : ModuleRegistry.getModules()) {
            try {
                if (module.isMarkedForDeletion()) {
                    removeModuleVersion(module.getName());
                }
            } catch (Throwable e) {
                log.error("Error in deleting version of module '{}'", module.getName(), e);
            }
        }
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
