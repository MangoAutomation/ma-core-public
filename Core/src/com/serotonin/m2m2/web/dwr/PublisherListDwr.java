/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.comparators.StringStringPairComparator;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

/**
 * @author Matthew Lohbihler
 */
public class PublisherListDwr extends BaseDwr {
    @DwrPermission(admin = true)
    public ProcessResult init() {
        ProcessResult response = new ProcessResult();

        List<StringStringPair> translatedTypes = new ArrayList<StringStringPair>();
        for (String type : ModuleRegistry.getPublisherDefinitionTypes())
            translatedTypes.add(new StringStringPair(type, translate(ModuleRegistry.getPublisherDefinition(type)
                    .getDescriptionKey())));
        StringStringPairComparator.sort(translatedTypes);

        response.addData("types", translatedTypes);
        response.addData("publishers", PublisherDao.instance.getPublishers(new PublisherDao.PublisherNameComparator()));

        return response;
    }

    @DwrPermission(admin = true)
    public ProcessResult togglePublisher(int publisherId) {
        ProcessResult response = new ProcessResult();

        PublisherVO<? extends PublishedPointVO> publisher = Common.runtimeManager.getPublisher(publisherId);

        publisher.setEnabled(!publisher.isEnabled());
        Common.runtimeManager.savePublisher(publisher);

        response.addData("enabled", publisher.isEnabled());
        response.addData("id", publisherId);

        return response;
    }

    @DwrPermission(admin = true)
    public int deletePublisher(int publisherId) {
        Common.runtimeManager.deletePublisher(publisherId);
        return publisherId;
    }
}
