# Jasypt gradle plugin

[![build](https://github.com/ximtech/jasypt-encrypt-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/ximtech/jasypt-encrypt-plugin/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/ximtech/jasypt-encrypt-plugin/branch/main/graph/badge.svg?token=sHBgjzjp5Y)](https://codecov.io/gh/ximtech/jasypt-encrypt-plugin)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/92143660494d46789a1d5308bed6d17e)](https://app.codacy.com/gh/ximtech/jasypt-encrypt-plugin/dashboard)

Based on [moberwasserlechner/jasypt-gradle-plugin](https://github.com/moberwasserlechner/jasypt-gradle-plugin)

This plugin uses [Jasypt](http://jasypt.org/) library for encrypting/decrypting application properties. Works great
with `Spring Boot`, but also can work independently. With provided `password` as encryption key plugin hides sensitive
data from direct reading and decrypt them at runtime. Can be useful for data that don't need to be changed frequently
and also allow reducing parameters or system environment variables amount passing to the docker container.

## How to use

### Using plugins DSL

Check latest version at [Gradle repository](https://plugins.gradle.org/plugin/io.github.ximtech.jasypt-encrypt-plugin)

```groovy
plugins {
    id "io.github.ximtech.jasypt-encrypt-plugin" version "1.3.3"
}
```

#### Manually add to project

1. Copy `jar` file to classpath. Get prebuild `jar` file from `assets -> jasypt-encrypt-plugin-<version>.jar`
2. Add `buildscript` to the top of `build.gradle`

```groovy
buildscript {
    dependencies {
        classpath files('jasypt-encrypt-plugin-1.3.3.jar')
    }
}
```

Add plugin:

```groovy
apply plugin: com.github.jasypt.encrypt.JasyptPlugin
```

### Jasypt configuration

***NOTE:*** This section can be skipped if default configuration is ok for you.

For custom encryption configuration add `.yaml` or`.properties` file in the project root In Spring Boot application just
add `Jasypt` configuration to `application.yaml`

Example:

```yaml
jasypt:
    encryptor:
        password: ${JASYPT_ENCRYPTOR_PASSWORD}  # pass as environment variable
        algorithm: "PBEWITHHMACSHA512ANDAES_256"
        salt-generator-classname: "org.jasypt.salt.RandomSaltGenerator"
        iv-generator-classname: "org.jasypt.iv.RandomIvGenerator"
        provider-name: "SunJCE"
        string-output-type: "base64"
        pool-size: 1
        key-obtention-iterations: 1000
```

And for `.properties` configuration:

```properties
jasypt.encryptor.password="password"
jasypt.encryptor.algorithm="PBEWITHHMACSHA512ANDAES_256"
jasypt.encryptor.salt-generator-classname="org.jasypt.salt.RandomSaltGenerator"
jasypt.encryptor.iv-generator-classname="org.jasypt.iv.RandomIvGenerator"
jasypt.encryptor.provider-name="SunJCE"
jasypt.encryptor.string-output-type="base64"
jasypt.encryptor.pool-size=1
jasypt.encryptor.key-obtention-iterations=1000
```

Plugin will catch up configuration from file and setup encryptor/decryptor or use default if no configs found. More info
about `Spring Boot` usage with `Jasypt` you can find [here](https://github.com/ulisesbocchio/jasypt-spring-boot)

### Project build configuration

Create environment variable `JASYPT_ENCRYPTOR_PASSWORD` with encryption password. Then add to project `build.gradle`

```groovy
encryptProperties {
    password = System.getenv('JASYPT_ENCRYPTOR_PASSWORD')
}

decryptProperties {
    password = System.getenv('JASYPT_ENCRYPTOR_PASSWORD')
}
```

***Optionally:*** setup executable build task for encryption 'open' properties

```groovy
jar {
    dependsOn(encryptProperties)
}
```

### CI/CD pipeline

For running project in pipeline, add `JASYPT_ENCRYPTOR_PASSWORD` system environment variable in build configuration
```yaml
env:
  JASYPT_ENCRYPTOR_PASSWORD: ${{ secrets.JASYPT_ENCRYPTOR_PASSWORD }}
```

## Tasks

All plugin tasks require `password` parameter or system environment variable

### encryptProperties

Search for all `.properties/.yaml` files for values wrapped with `ENCRYPT()` and encrypt them.

```text
gradle encryptProperties --password=encryptorToken
```

Example:

```properties
some.very.secret.property=ENCRYPT(private)
```

Will be encrypted to:

```properties
some.very.secret.property=ENC(Lk5VWETH98C0/E/wOqzioQ==)
```

Property files can be filtered by pattern. In the example has been shown how search for non production yaml files and
encrypt their values.

```text
gradle encryptProperties --file-filter-pattern='application-((?!prod).*)\.yaml' --password=encryptorToken
```

#### Multiline properties

- ***Yaml file example:***
```yaml
multiline.property: |
    ENCRYPT(
    some
    very
    long
    text
    )
```
***NOTE:*** For the `yaml` file the indentation level should be the same for all multiline values. Also the pipe character '|' must be present on the first line,
otherwise encryption/decryption won't work correctly.

- ***Properties file:***
```properties
multiline.property=\
  ENCRYPT(\
  example\
  multiline\
  )
```

### decryptProperties

Search for all `.properties/.yaml` files for values wrapped with `ENC()` and decrypt them.

```text
gradle decryptProperties --password=encryptorToken
```

Search for non production `.properties/.yaml` files and decrypt their values.

```text
gradle decryptProperties --file-filter-pattern='application-((?!prod).*)\.yaml' --password=encryptorToken
```

### encryptText

```text
gradle encryptText --text=someText --password=encryptorToken
```

***Output***:

```text
Encrypted text: SCw2qhh2bvTFJ4TPXgolTqM1kDDZ8FWbSW3yHlvPLDV9yektRCO7Jx8I1ZMuzSzm
```

### decryptText

```text
gradle decryptText --text=SCw2qhh2bvTFJ4TPXgolTqM1kDDZ8FWbSW3yHlvPLDV9yektRCO7Jx8I1ZMuzSzm --password=encryptorToken
```

***Output***:

```text
Decrypted text: someText
```
