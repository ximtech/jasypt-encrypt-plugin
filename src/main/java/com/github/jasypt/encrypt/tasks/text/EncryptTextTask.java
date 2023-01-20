package com.github.jasypt.encrypt.tasks.text;

import com.github.jasypt.encrypt.tasks.JasyptConfig;

import static com.github.jasypt.encrypt.JasyptPlugin.TASK_GROUP_NAME;

public class EncryptTextTask extends TextAwareTask {

    public static final String TASK_NAME = "encryptText";
    private static final String TASK_DESCRIPTION = "Encrypts the given text";

    public EncryptTextTask() {
        this.setGroup(TASK_GROUP_NAME);
        this.setDescription(TASK_DESCRIPTION);
    }

    @Override
    public void taskAction() {
        String encrypted = initEncryptor(JasyptConfig.DEFAULT_JASYPT_CONFIG).encrypt(this.text);
        System.out.println("Encrypted text: " + encrypted);
    }
}
