/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.jooq.Configuration;
import org.jooq.DDLExportConfiguration;
import org.jooq.DSLContext;
import org.jooq.Meta;
import org.jooq.Queries;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.Source;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

public class ConvertDDL implements Runnable {

    public static void main(String[] arguments) {
        Path input = Paths.get(arguments[0]);
        SQLDialect outputDialect = arguments.length > 1 ? SQLDialect.valueOf(arguments[1]) : SQLDialect.MYSQL;
        SQLDialect inputDialect = arguments.length > 2 ? SQLDialect.valueOf(arguments[2]) : SQLDialect.DEFAULT;
        new ConvertDDL(input, outputDialect, inputDialect).run();
    }

    private final Path input;
    private final DSLContext create;

    /**
     * @param input input DDL file
     * @param outputDialect output dialect
     * @param inputDialect input dialect, if {@link SQLDialect#DEFAULT DEFAULT} the DDL will be interpreted
     */
    public ConvertDDL(Path input, SQLDialect outputDialect, SQLDialect inputDialect) {
        this.input = input;

        Configuration configuration = new DefaultConfiguration();
        configuration.set(outputDialect);

        Settings settings = configuration.settings();
        settings.setInterpreterDialect(inputDialect);
        settings.withRenderNameCase(RenderNameCase.AS_IS);

        this.create = DSL.using(configuration);
    }

    @Override
    public void run() {
//        Meta coreMeta;
//        try (InputStream is  = getClass().getResourceAsStream("/com/serotonin/m2m2/db/createTables-H2.sql")) {
//            coreMeta = create.meta(Source.of(is));
//        } catch (IOException e) {
//            throw new UncheckedIOException(e);
//        }

        Meta meta;
        try (InputStream is  = getClass().getResourceAsStream("/com/serotonin/m2m2/db/createTables-H2.sql")) {
            meta = create.meta(Source.of(is), Source.of(input.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

//        Set<Table<?>> coreTables = new HashSet<>(coreMeta.getTables());
//        meta = meta.filterTables(t -> !coreTables.contains(t));

        DDLExportConfiguration configuration = new DDLExportConfiguration()
                .respectTableOrder(true)
                .respectColumnOrder(true)
                .respectConstraintOrder(true)
                .respectIndexOrder(true);

        Queries ddl = meta.ddl(configuration);

        Path output = input.getParent().resolve(String.format("createTables-%s.sql", create.configuration().dialect()));
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(output, StandardOpenOption.TRUNCATE_EXISTING))) {
            for (Query query : ddl.queries()) {
                writer.printf("%s;%n", query);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
