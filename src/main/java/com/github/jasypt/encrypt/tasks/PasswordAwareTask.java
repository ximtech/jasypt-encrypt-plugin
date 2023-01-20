package com.github.jasypt.encrypt.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.internal.tasks.options.OptionValidationException;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

public abstract class PasswordAwareTask extends DefaultTask {

    private String password;

    @Internal
    private PBEStringEncryptor encryptor;

    protected PBEStringEncryptor initEncryptor(JasyptConfig config) {
        if (encryptor == null) {
            this.encryptor = createEncryptor(config);
        }
        return encryptor;
    }

    @TaskAction
    public void action() {
        if (this.password == null || this.password.trim().isEmpty()) {
            throw new OptionValidationException("--password is required!");
        }
        validateOptions();
        taskAction();
    }

    public abstract void taskAction();
    public abstract void validateOptions();

    @Option(option = "password", description = "password [required]")
    public void setPassword(String password) {
        this.password = password;
    }

    public PBEStringEncryptor getEncryptor() {
        return encryptor;
    }

    private PBEStringEncryptor createEncryptor(JasyptConfig jasyptConfig) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(jasyptConfig.getAlgorithm());
        config.setKeyObtentionIterations(jasyptConfig.getKeyObtentionIterations());
        config.setPoolSize(jasyptConfig.getPoolSize());
        config.setProviderName(jasyptConfig.getProviderName());
        config.setProviderClassName(jasyptConfig.getProviderClassName());
        config.setSaltGeneratorClassName(jasyptConfig.getSaltGeneratorClassname());
        config.setIvGeneratorClassName(jasyptConfig.getIvGeneratorClassname());
        config.setStringOutputType(jasyptConfig.getStringOutputType());
        encryptor.setConfig(config);
        encryptor.initialize();
        return encryptor;
    }
}