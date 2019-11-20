/**
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.MessageResolver;
import org.passay.PasswordData;
import org.passay.PasswordGenerator;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.RuleResultDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 * 
 * Class to validate passwords against the system settings defined rules.
 * 
 * Rules:
 * 
 *
 * 
 * 
 * @author Terry Packer
 *
 */
@Service
public class PasswordService implements SystemSettingsListener {

    // Settings
    private CharacterRule upperCaseCountRule;
    private CharacterRule lowerCaseCountRule;
    private CharacterRule digitCountRule;
    private CharacterRule specialCountRule;
    private LengthRule lengthRule;

    @Autowired
    public PasswordService(SystemSettingsDao dao) {
        int upperCaseCount = dao.getIntValue(SystemSettingsDao.PASSWORD_UPPER_CASE_COUNT);
        if (upperCaseCount > 0) {
            this.upperCaseCountRule =
                    new CharacterRule(EnglishCharacterData.UpperCase, upperCaseCount);
        }
        int lowerCaseCount = dao.getIntValue(SystemSettingsDao.PASSWORD_LOWER_CASE_COUNT);
        if (lowerCaseCount > 0) {
            this.lowerCaseCountRule =
                    new CharacterRule(EnglishCharacterData.LowerCase, lowerCaseCount);
        }
        int digitCount = dao.getIntValue(SystemSettingsDao.PASSWORD_DIGIT_COUNT);
        if (digitCount > 0) {
            this.digitCountRule = new CharacterRule(EnglishCharacterData.Digit, digitCount);
        }
        int specialCount = dao.getIntValue(SystemSettingsDao.PASSWORD_SPECIAL_COUNT);
        if (specialCount > 0) {
            this.specialCountRule = new CharacterRule(EnglishCharacterData.Special, specialCount);
        }
        this.lengthRule = new LengthRule(dao.getIntValue(SystemSettingsDao.PASSWORD_LENGTH_MIN),
                dao.getIntValue(SystemSettingsDao.PASSWORD_LENGTH_MAX));
    }

    /**
     * Validate a freetext password against the set of rules defined
     *  in the system settings
     * @param password
     * @throws PasswordInvalidException
     */
    public void validatePassword(String password)
            throws PasswordInvalidException {
        List<CharacterRule> charRules = getCharacterRules();
        List<Rule> rules = new ArrayList<>(charRules);
        rules.add(getLengthRule());
        PasswordValidator validator = new PasswordValidator(rules);
        RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            MangoPassayMessageResolver resolver = new MangoPassayMessageResolver();
            result.getDetails().stream().forEach(detail -> {resolver.resolve(detail);});
            throw new PasswordInvalidException(resolver.getMessages());
        }
    }

    public String generatePassword(int length) {
        PasswordGenerator generator = new PasswordGenerator();
        generator.generatePassword(length, getCharacterRules());
        return null;
    }

    private Rule getLengthRule() {
        return this.lengthRule;
    }

    /**
     * Get the list of all character requirements
     * 
     * @return
     */
    private List<CharacterRule> getCharacterRules() {
        List<CharacterRule> rules = new ArrayList<>();
        if (this.lowerCaseCountRule != null) {
            rules.add(this.lowerCaseCountRule);
        }
        if (this.upperCaseCountRule != null) {
            rules.add(this.upperCaseCountRule);
        }
        if (this.digitCountRule != null) {
            rules.add(this.digitCountRule);
        }
        if (this.specialCountRule != null) {
            rules.add(this.specialCountRule);
        }
        return rules;
    }

    @Override
    public void systemSettingsSaved(String key, String oldValue, String newValue) {
        switch (key) {
            case SystemSettingsDao.PASSWORD_UPPER_CASE_COUNT:
                Integer upperCaseCount = Integer.parseInt(newValue);
                if(upperCaseCount > 0) {
                    this.upperCaseCountRule = new CharacterRule(EnglishCharacterData.UpperCase, upperCaseCount);
                }else {
                    this.upperCaseCountRule = null;
                }
                break;
            case SystemSettingsDao.PASSWORD_LOWER_CASE_COUNT:
                Integer lowerCaseCount = Integer.parseInt(newValue);
                if(lowerCaseCount > 0) {
                    this.lowerCaseCountRule = new CharacterRule(EnglishCharacterData.LowerCase, lowerCaseCount);
                }else {
                    this.lowerCaseCountRule = null;
                }
                break;
            case SystemSettingsDao.PASSWORD_DIGIT_COUNT:
                Integer digitCount = Integer.parseInt(newValue);
                if(digitCount > 0) {
                    this.digitCountRule = new CharacterRule(EnglishCharacterData.Digit, digitCount);
                }else {
                    this.digitCountRule = null;
                }
                break;
            case SystemSettingsDao.PASSWORD_SPECIAL_COUNT:
                Integer specialCount = Integer.parseInt(newValue);
                if(specialCount > 0) {
                    this.specialCountRule = new CharacterRule(EnglishCharacterData.Special, specialCount);
                }else {
                    this.specialCountRule = null;
                }
                break;
            case SystemSettingsDao.PASSWORD_LENGTH_MIN:
                this.lengthRule = new LengthRule(Integer.parseInt(newValue),
                        this.lengthRule.getMaximumLength());
                break;
            case SystemSettingsDao.PASSWORD_LENGTH_MAX:
                this.lengthRule = new LengthRule(this.lengthRule.getMinimumLength(),
                        Integer.parseInt(newValue));
                break;
        }
    }

    @Override
    public List<String> getKeys() {
        return Arrays.asList(SystemSettingsDao.PASSWORD_UPPER_CASE_COUNT,
                SystemSettingsDao.PASSWORD_LOWER_CASE_COUNT, SystemSettingsDao.PASSWORD_DIGIT_COUNT,
                SystemSettingsDao.PASSWORD_SPECIAL_COUNT, SystemSettingsDao.PASSWORD_LENGTH_MIN,
                SystemSettingsDao.PASSWORD_LENGTH_MAX);
    }

    public static final class MangoPassayMessageResolver implements MessageResolver {

        private static final String INSUFFICIENT_LOWERCASE = "INSUFFICIENT_LOWERCASE";
        private static final String INSUFFICIENT_UPPERCASE = "INSUFFICIENT_UPPERCASE";
        private static final String INSUFFICIENT_DIGIT = "INSUFFICIENT_DIGIT";
        private static final String INSUFFICIENT_SPECIAL = "INSUFFICIENT_SPECIAL";
        private static final String TOO_SHORT = "TOO_SHORT";
        private static final String TOO_LONG = "TOO_LONG";
        
        private final List<TranslatableMessage> messages;

        public MangoPassayMessageResolver() {
            this.messages = new ArrayList<>();
        }

        @Override
        public String resolve(RuleResultDetail detail) {
            switch(detail.getErrorCode()) {
                case INSUFFICIENT_LOWERCASE:
                    this.messages.add(new TranslatableMessage("validate.password.insufficientLowercase", detail.getValues()));
                    return INSUFFICIENT_LOWERCASE;
                case INSUFFICIENT_UPPERCASE:
                    this.messages.add(new TranslatableMessage("validate.password.insufficientUppercase", detail.getValues()));
                    return INSUFFICIENT_UPPERCASE;
                case INSUFFICIENT_DIGIT:
                    this.messages.add(new TranslatableMessage("validate.password.insufficientDigit", detail.getValues()));
                    return INSUFFICIENT_DIGIT;
                case INSUFFICIENT_SPECIAL:
                    this.messages.add(new TranslatableMessage("validate.password.insufficientSpecial", detail.getValues()));
                    return INSUFFICIENT_SPECIAL;
                case TOO_LONG:
                    this.messages.add(new TranslatableMessage("validate.password.tooLong", detail.getValues()));
                    return TOO_LONG;
                case TOO_SHORT:
                    this.messages.add(new TranslatableMessage("validate.password.tooShort", detail.getValues()));
                    return TOO_SHORT;
                default:
                    return "Unsupported password error code: " + detail.getErrorCode();
            }
        }
        
        public List<TranslatableMessage> getMessages() {
            return messages;
        }
    }

    /**
     * Exception container for messages as to why a password is invalid
     * @author Terry Packer
     *
     */
    public static class PasswordInvalidException extends Exception {

        private static final long serialVersionUID = 1L;
        private final List<TranslatableMessage> messages;
        
        public PasswordInvalidException(List<TranslatableMessage> messages) {
            this.messages = messages;
        }
        
        public List<TranslatableMessage> getMessages() {
            return messages;
        }
    }
}
