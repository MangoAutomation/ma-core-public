/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.audit;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;

/**
 * Base Class to restore VOs via their audit trail.
 *
 * Example Code:
 * <pre>{@code
 * try {
 *     //First read from DB
 *     List<AuditEventInstanceVO> auditData = AuditEventDao.getInstance().getAllForObject(AuditEventType.TYPE_TEMPLATE, 2);
 *     TemplateRestorer restorer = new TemplateRestorer(auditData, new ProcessResult());
 *     BaseTemplateVO<?> data1 = restorer.restore();
 *
 *     writer.writeObject(data1);
 *     return stringWriter.toString();
 * } catch (JsonException e) {
 *     return e.getMessage();
 * } catch (IOException e) {
 *     return e.getMessage();
 * }
 * }</pre>
 *
 * @author Terry Packer
 *
 */
public abstract class Restorer<T extends AbstractVO> {

    private final ProcessResult result;
    private final Translations translations;
    private boolean success;
    private final List<ProcessMessage> failureMessages = new ArrayList<ProcessMessage>();

    private List<AuditEventInstanceVO> trail;

    public Restorer(List<AuditEventInstanceVO> trail, ProcessResult result){
        this.trail = trail;
        this.result = result;
        this.translations = Common.getTranslations();
    }

    /**
     * Restore the object based on the Audit Trail
     */
    public T restore(){
        T vo = null;
        try{
            //Follow the trail
            for(AuditEventInstanceVO audit : trail){
                JsonObject context = audit.getContext();
                JsonReader reader = new JsonReader(Common.JSON_CONTEXT, context);

                if(audit.getChangeType() == AuditEventInstanceVO.CHANGE_TYPE_CREATE){
                    vo = this.build(audit.getObjectId(), context, reader);
                }else if(audit.getChangeType() == AuditEventInstanceVO.CHANGE_TYPE_MODIFY){
                    if(vo == null)
                        vo = getExisting(audit.getObjectId());
                    vo = this.build(vo, context, reader);
                }
            }

            ProcessResult voResponse = new ProcessResult();
            //TODO need to be able to validate T vo.validate(voResponse);
            if (voResponse.getHasMessages())
                copyValidationMessages(voResponse, "restore.prefix", vo.getXid());
            else {
                addSuccessMessage(vo.isNew(), "restore.prefix", vo.getXid());
            }
        }catch (TranslatableJsonException e) {
            addFailureMessage("restore.prefix", "need-to-fill-in", e.getMsg());
        }catch (JsonException e) {
            addFailureMessage("restoring.prefix", "need-to-fill-in", getJsonExceptionMessage(e));
        }catch(Exception e){
            addFailureMessage("restoring.prefix", "need-to-fill-in", e.getMessage());
        }

        return vo;
    }

    /**
     * Build a brand new one from the JSON
     */
    protected T build(int id, JsonObject json, JsonReader reader) throws JsonException{
        T vo = build(null, json, reader);
        if(vo == null)
            return vo;
        vo.setId(id);
        return vo;
    }

    protected T build(T vo, JsonObject json, JsonReader reader) throws JsonException{
        if (vo == null) {
            vo = buildNewVO(json);
            if(vo == null)
                return null;
        }

        //Set XID as this doesn't happen in the readInto
        String xid = json.getString("xid");
        vo.setXid(xid);

        //Read Into
        reader.readInto(vo, json);
        return vo;
    }

    /**
     * Build a new Vo From scratch
     */
    protected abstract T buildNewVO(JsonObject json);

    protected abstract T getExisting(int id);

    protected void addFailureMessage(ProcessMessage message){
        success = false;
        failureMessages.add(message);
    }

    protected void addFailureMessage(String key, Object... params) {
        success = false;
        failureMessages.add(new ProcessMessage(key, params));
    }


    public boolean success() {
        return success;
    }

    public void copyValidationMessages(ProcessResult voResponse, String key, String desc) {
        for (ProcessMessage msg : voResponse.getMessages())
            result.addGenericMessage(key, desc, msg.toString(translations));
    }

    public void addSuccessMessage(boolean isnew, String key, String desc) {
        if (isnew)
            result.addGenericMessage(Level.info, key, desc, translations.translate("restore.created"));
        else
            result.addGenericMessage(Level.info, key, desc, translations.translate("restore.updated"));
    }

    public String getJsonExceptionMessage(JsonException e) {
        String msg = "'" + e.getMessage() + "'";
        Throwable t = e;
        while ((t = t.getCause()) != null) {
            if (t instanceof TranslatableJsonException)
                msg += ", " + translations.translate("emport.causedBy") + " '"
                        + ((TranslatableJsonException) t).getMsg().translate(translations) + "'";
            else
                msg += ", " + translations.translate("emport.causedBy") + " '" + t.getMessage() + "'";
        }
        return msg;
    }


}
