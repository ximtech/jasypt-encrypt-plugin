package com.github.jasypt.encrypt.tasks.text;

import static com.github.jasypt.encrypt.JasyptPlugin.TASK_GROUP_NAME;
import static com.github.jasypt.encrypt.tasks.JasyptConfig.DEFAULT_JASYPT_CONFIG;

public class DecryptTextTask extends TextAwareTask {

    public static final String TASK_NAME = "decryptText";
    private static final String TASK_DESCRIPTION = "Decrypts the given text";

    public DecryptTextTask() {
        this.setGroup(TASK_GROUP_NAME);
        this.setDescription(TASK_DESCRIPTION);
    }

    @Override
    public void taskAction() {
        String decrypted = initEncryptor(DEFAULT_JASYPT_CONFIG).decrypt(this.text);
        System.out.println("Decrypted text: " + decrypted);
    }

}
