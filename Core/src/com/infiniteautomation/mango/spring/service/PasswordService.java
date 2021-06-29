/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.RuleResultDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 * Class to validate passwords against the system settings defined rules.
 * <p>
 * Possible Rules:
 * minimum required upper case characters
 * minimum required lower case characters
 * minimum required digit characters
 * minimum required special characters
 * minimum length
 * maximum length
 *
 * @author Terry Packer
 */
@Service
public class PasswordService implements SystemSettingsListener {

    private volatile int upperCaseCount;
    private volatile int lowerCaseCount;
    private volatile int digitCount;
    private volatile int specialCount;
    private volatile int lengthMin;
    private volatile int lengthMax;
    private volatile boolean expirationEnabled;
    private volatile int expirationPeriodType;
    private volatile int expirationPeriods;

    private final LazyInitSupplier<List<Rule>> rules = new LazyInitSupplier<>(this::createRules);

    @Autowired
    public PasswordService(SystemSettingsDao dao) {
        this.upperCaseCount = dao.getIntValue(SystemSettingsDao.PASSWORD_UPPER_CASE_COUNT);
        this.lowerCaseCount = dao.getIntValue(SystemSettingsDao.PASSWORD_LOWER_CASE_COUNT);
        this.digitCount = dao.getIntValue(SystemSettingsDao.PASSWORD_DIGIT_COUNT);
        this.specialCount = dao.getIntValue(SystemSettingsDao.PASSWORD_SPECIAL_COUNT);
        this.lengthMin = dao.getIntValue(SystemSettingsDao.PASSWORD_LENGTH_MIN);
        this.lengthMax = dao.getIntValue(SystemSettingsDao.PASSWORD_LENGTH_MAX);
        this.expirationEnabled = dao.getBooleanValue(SystemSettingsDao.PASSWORD_EXPIRATION_ENABLED);
        this.expirationPeriodType = dao.getIntValue(SystemSettingsDao.PASSWORD_EXPIRATION_PERIOD_TYPE);
        this.expirationPeriods = dao.getIntValue(SystemSettingsDao.PASSWORD_EXPIRATION_PERIODS);
    }

    /**
     * Validate a freetext password against the set of rules defined
     * in the system settings
     *
     * @param password
     * @throws PasswordInvalidException
     */
    public void validatePassword(String password) throws PasswordInvalidException {
        PasswordValidator validator = new PasswordValidator(rules.get());
        RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            MangoPassayMessageResolver resolver = new MangoPassayMessageResolver();
            result.getDetails().forEach(resolver::resolve);
            throw new PasswordInvalidException(resolver.getMessages());
        }
    }

    public boolean passwordExpired(User user) {
        if (expirationEnabled) {
            long expiration = user.getPasswordChangeTimestamp() + Common.getMillis(expirationPeriodType, expirationPeriods);
            return Common.timer.currentTimeMillis() >= expiration;
        }
        return false;
    }

    private List<Rule> createRules() {
        int upperCaseCount = this.upperCaseCount;
        int lowerCaseCount = this.lowerCaseCount;
        int digitCount = this.digitCount;
        int specialCount = this.specialCount;

        List<Rule> rules = new ArrayList<>();
        if (upperCaseCount > 0) {
            rules.add(new CharacterRule(EnglishCharacterData.UpperCase, upperCaseCount));
        }
        if (lowerCaseCount > 0) {
            rules.add(new CharacterRule(EnglishCharacterData.LowerCase, lowerCaseCount));
        }
        if (digitCount > 0) {
            rules.add(new CharacterRule(EnglishCharacterData.Digit, digitCount));
        }
        if (specialCount > 0) {
            rules.add(new CharacterRule(EnglishCharacterData.Special, specialCount));
        }
        rules.add(new LengthRule(lengthMin, lengthMax));
        return rules;
    }

    @Override
    public void systemSettingsSaved(String key, String oldValue, String newValue) {
        switch (key) {
            case SystemSettingsDao.PASSWORD_UPPER_CASE_COUNT:
                this.upperCaseCount = Integer.parseInt(newValue);
                break;
            case SystemSettingsDao.PASSWORD_LOWER_CASE_COUNT:
                this.lowerCaseCount = Integer.parseInt(newValue);
                break;
            case SystemSettingsDao.PASSWORD_DIGIT_COUNT:
                this.digitCount = Integer.parseInt(newValue);
                break;
            case SystemSettingsDao.PASSWORD_SPECIAL_COUNT:
                this.specialCount = Integer.parseInt(newValue);
                break;
            case SystemSettingsDao.PASSWORD_LENGTH_MIN:
                this.lengthMin = Integer.parseInt(newValue);
                break;
            case SystemSettingsDao.PASSWORD_LENGTH_MAX:
                this.lengthMax = Integer.parseInt(newValue);
                break;
            case SystemSettingsDao.PASSWORD_EXPIRATION_ENABLED:
                this.expirationEnabled = SystemSettingsDao.parseBoolean(newValue);
                break;
            case SystemSettingsDao.PASSWORD_EXPIRATION_PERIOD_TYPE:
                this.expirationPeriodType = Integer.parseInt(newValue);
                break;
            case SystemSettingsDao.PASSWORD_EXPIRATION_PERIODS:
                this.expirationPeriods = Integer.parseInt(newValue);
                break;
        }
        this.rules.reset();
    }

    @Override
    public List<String> getKeys() {
        return Arrays.asList(SystemSettingsDao.PASSWORD_UPPER_CASE_COUNT,
                SystemSettingsDao.PASSWORD_LOWER_CASE_COUNT,
                SystemSettingsDao.PASSWORD_DIGIT_COUNT,
                SystemSettingsDao.PASSWORD_SPECIAL_COUNT,
                SystemSettingsDao.PASSWORD_LENGTH_MIN,
                SystemSettingsDao.PASSWORD_LENGTH_MAX,
                SystemSettingsDao.PASSWORD_EXPIRATION_ENABLED,
                SystemSettingsDao.PASSWORD_EXPIRATION_PERIOD_TYPE,
                SystemSettingsDao.PASSWORD_EXPIRATION_PERIODS);
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
            switch (detail.getErrorCode()) {
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
     *
     * @author Terry Packer
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
