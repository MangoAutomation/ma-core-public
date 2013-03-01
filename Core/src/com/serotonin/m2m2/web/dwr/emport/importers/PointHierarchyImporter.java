package com.serotonin.m2m2.web.dwr.emport.importers;

import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class PointHierarchyImporter extends Importer {
    private final JsonArray json;

    public PointHierarchyImporter(JsonArray json) {
        super(null);
        this.json = json;
    }

    @Override
    protected void importImpl() {
        PointFolder root = new PointFolder(0, "Root");
        try {
            @SuppressWarnings("unchecked")
            List<PointFolder> subfolders = (List<PointFolder>) ctx.getReader().read(
                    new TypeDefinition(List.class, PointFolder.class), json);
            root.setSubfolders(subfolders);

            // Save the new values.
            ctx.getDataPointDao().savePointHierarchy(root);
            addSuccessMessage(false, "emport.pointHierarchy.prefix", "");
        }
        catch (TranslatableJsonException e) {
            addFailureMessage("emport.pointHierarchy.prefix", e.getMsg());
        }
        catch (JsonException e) {
            addFailureMessage("emport.pointHierarchy.prefix", getJsonExceptionMessage(e));
        }
    }
}
