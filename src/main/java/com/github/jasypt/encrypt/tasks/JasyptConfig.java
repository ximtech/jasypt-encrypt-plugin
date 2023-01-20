package com.github.jasypt.encrypt.tasks;

import com.tvd12.properties.file.annotation.Property;

public class JasyptConfig {

    public static final JasyptConfig DEFAULT_JASYPT_CONFIG = new JasyptConfig();
    
    @Property("jasypt.encryptor.pool-size")
    private String poolSize = "1";
    @Property("jasypt.encryptor.algorithm")
    private String algorithm = "PBEWITHHMACSHA512ANDAES_256";
    @Property("jasypt.encryptor.key-obtention-iterations")
    private String keyObtentionIterations = "1000";

    @Property("jasypt.encryptor.string-output-type")
    private String stringOutputType = "base64";
    @Property("jasypt.encryptor.iv-generator-classname")
    private String ivGeneratorClassname = "org.jasypt.iv.RandomIvGenerator";
    @Property("jasypt.encryptor.salt-generator-classname")
    private String saltGeneratorClassname = "org.jasypt.salt.RandomSaltGenerator";
    
    @Property("jasypt.encryptor.provider-name")
    private String providerName;
    @Property("jasypt.encryptor.provider-class-name")
    private String providerClassName;

    public JasyptConfig() {}

    public String getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(String poolSize) {
        this.poolSize = poolSize;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKeyObtentionIterations() {
        return keyObtentionIterations;
    }

    public void setKeyObtentionIterations(String keyObtentionIterations) {
        this.keyObtentionIterations = keyObtentionIterations;
    }

    public String getStringOutputType() {
        return stringOutputType;
    }

    public void setStringOutputType(String stringOutputType) {
        this.stringOutputType = stringOutputType;
    }

    public String getIvGeneratorClassname() {
        return ivGeneratorClassname;
    }

    public void setIvGeneratorClassname(String ivGeneratorClassname) {
        this.ivGeneratorClassname = ivGeneratorClassname;
    }

    public String getSaltGeneratorClassname() {
        return saltGeneratorClassname;
    }

    public void setSaltGeneratorClassname(String saltGeneratorClassname) {
        this.saltGeneratorClassname = saltGeneratorClassname;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderClassName() {
        return providerClassName;
    }

    public void setProviderClassName(String providerClassName) {
        this.providerClassName = providerClassName;
    }
}
        

