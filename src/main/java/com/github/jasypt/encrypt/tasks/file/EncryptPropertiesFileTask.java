package com.github.jasypt.encrypt.tasks.file;

import com.github.jasypt.encrypt.JasyptPlugin;
import org.jasypt.encryption.pbe.PBEStringEncryptor;

import java.util.regex.Pattern;

public class EncryptPropertiesFileTask extends PropertiesFileAwareTask {

    public static final String TASK_NAME = "encryptProperties";
    private static final String TASK_DESCRIPTION = "Encrypts the property values wrapped with 'ENCRYPT(plain_text)'";
    private static final Pattern ENCRYPT_EXTRACTION_REGEX = Pattern.compile("ENCRYPT\\((.*)\\)");

    public EncryptPropertiesFileTask() {
        this.setGroup(JasyptPlugin.TASK_GROUP_NAME);
        this.setDescription(TASK_DESCRIPTION);
    }

    @Override
    public void validateOptions() {}

    @Override
    public String process(PBEStringEncryptor encryptor, String extractedValue) {
        return encryptor.encrypt(extractedValue);
    }

    @Override
    public String getPropertyPrefix() {
        return "ENC(";
    }

    @Override
    public String getPropertySuffix() {
        return ")";
    }

    @Override
    public Pattern getDefaultExtractPattern() {
        return ENCRYPT_EXTRACTION_REGEX;
    }

}
