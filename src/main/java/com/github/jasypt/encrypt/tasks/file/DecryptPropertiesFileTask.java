package com.github.jasypt.encrypt.tasks.file;

import org.jasypt.encryption.pbe.PBEStringEncryptor;

import java.util.regex.Pattern;

import static com.github.jasypt.encrypt.JasyptPlugin.TASK_GROUP_NAME;

public class DecryptPropertiesFileTask extends PropertiesFileAwareTask {

    public static final String TASK_NAME = "decryptProperties";
    private static final String TASK_DESCRIPTION = "Decrypts the property values wrapped with 'ENC(encrypted_text)'";
    private static final Pattern DECRYPT_EXTRACTION_REGEX = Pattern.compile("ENC\\((.*)\\)");

    public DecryptPropertiesFileTask() {
        this.setGroup(TASK_GROUP_NAME);
        this.setDescription(TASK_DESCRIPTION);
    }

    @Override
    public void validateOptions() {}

    @Override
    public String process(PBEStringEncryptor encryptor, String extractedValue) {
        return encryptor.decrypt(extractedValue);
    }

    @Override
    public String getPropertyPrefix() {
        return "ENCRYPT(";
    }

    @Override
    public String getPropertySuffix() {
        return ")";
    }

    @Override
    public Pattern getDefaultExtractPattern() {
        return DECRYPT_EXTRACTION_REGEX;
    }

}
